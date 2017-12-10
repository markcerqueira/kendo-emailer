import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import models.Contact;
import models.TextMessageHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * KendoEmailer.java
 * <p>
 * Main entry-point for the app.
 * <p>
 * Run from the command-line:
 * gradle -q run
 * <p>
 * And with arguments:
 * gradle -q run -Dexec.args="preview"
 */
public class KendoEmailer {

    private static final String NEW_LINE = "<br>\n";
    private static final String TAB = "&nbsp;&nbsp;&nbsp;&nbsp;";

    private static String EMAIL_HEADER; // Pulled from email_header.txt
    private static String EMAIL_FOOTER; // Pulled from email_footer.txt
    private static String FROM_EMAIL_ADDRESS; // Pulled from email_from.txt

    private static final String PREVIEW_SEND_KEY = "preview";
    private static final String PRODUCTION_SEND_KEY = "production";

    public static boolean sPreview;
    public static boolean sProduction;

    // Map from instructor id to the days they are teaching
    // e.g. "Mark" -> "Tuesday and Thursday"
    private static Map<String, String> sTeachingThisWeekMap = new HashMap<>();

    // gradle -q run -Dexec.args="preview"
    public static void main(String[] args) {
        try {
            runKendoEmailer(args);
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private static void runKendoEmailer(String[] args) throws Exception {
        Set<String> argumentSet = new HashSet<>();
        for (int i = 0; i < args.length; i++) {
            argumentSet.add(args[i]);
        }

        sPreview = argumentSet.contains(PREVIEW_SEND_KEY);
        sProduction = argumentSet.contains(PRODUCTION_SEND_KEY);

        // If we're in preview, we're definitely not in production
        if (sPreview) {
            sProduction = false;
        }

        EMAIL_HEADER = loadStringFromFile("/email_header.txt");
        EMAIL_FOOTER = loadStringFromFile("/email_footer.txt");
        FROM_EMAIL_ADDRESS = loadStringFromFile("/email_from.txt");

        // Get a list of upcoming events from the GoogleHelper class
        List<Event> upcomingEvents = GoogleHelper.getUpcomingCalendarEvents();

        System.out.println("KendoEmailer/main - upcomingEvents.size() = " + upcomingEvents.size());

        // Create email object with the main workhouse of this project
        EmailBuilder emailBuilder = createEmailBuilderWithSubjectAndBody(upcomingEvents);

        System.out.println("KendoEmailer/main - createEmailBuilderWithSubjectAndBody done");

        // If we're running without the preview or production flags just print
        // out the email we have created.
        if (!sPreview && !sProduction) {
            System.out.println(emailBuilder.toString());
        } else {
            System.out.println("KendoEmailer/main - sending email to " + emailBuilder.getToEmailList().size() + " people");
            GoogleHelper.buildAndSendEmail(emailBuilder);

            System.out.println("KendoEmailer/main - buildAndSendEmail done");

            if (sProduction) {
                List<TextMessageHelper> textMessageHelperList = getMessagesToSend();
                if (textMessageHelperList.size() > 0) {
                    TwilioHelper twilioHelper = new TwilioHelper();
                    for (TextMessageHelper textMessageHelper : textMessageHelperList) {
                        if (textMessageHelper.contact.cellNumber.length() == 12) {
                            twilioHelper.sendTextMessage(textMessageHelper.contact.cellNumber, textMessageHelper.getMessage());
                        }
                    }
                }
            }
        }
    }

    // Not everyone has a cell number!
    private static List<TextMessageHelper> getMessagesToSend() {
        List<TextMessageHelper> resultList = new ArrayList<>();

        // Send text message reminder if the instructor is teaching this week and has a cell number in their info.
        if (sTeachingThisWeekMap != null && sTeachingThisWeekMap.size() > 0) {
            for (Map.Entry<String, String> entry : sTeachingThisWeekMap.entrySet()) {
                String instructorId = entry.getKey();
                String daysTeaching = entry.getValue();

                Contact contact = EmailBuilder.getContactWithId(instructorId);

                if (contact == null) {
                    System.out.println("KendoEmailer/getMessagesToSend - no contact found for id = " + instructorId);
                    continue;
                }

                String body = "You are teaching this coming " + daysTeaching + ".";

                TextMessageHelper textMessageHelper = new TextMessageHelper();
                textMessageHelper.contact = contact;
                textMessageHelper.message = body;

                resultList.add(textMessageHelper);
            }
        } else {
            System.out.println("KendoEmailer/getMessagesToSend - sTeachingThisWeekMap is null or empty");
        }

        return resultList;
    }

    // Helper method that takes the content of the file in the passed parameter
    // and returns it as a String.
    private static String loadStringFromFile(String filename) {
        InputStream inputStream = KendoEmailer.class.getResourceAsStream(filename);
        BufferedReader reader = null;

        StringBuilder stringBuilder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

            String text;

            while ((text = reader.readLine()) != null) {
                stringBuilder.append(text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }

        return stringBuilder.toString();
    }

    // This takes the list of Events returned by Google and constructs our email.
    public static EmailBuilder createEmailBuilderWithSubjectAndBody(List<Event> eventList) {
        LinkedHashMap<DateTime, List<String>> eventMap = new LinkedHashMap<>();

        Set<String> teachingThisWeekSet = new HashSet<>();

        // We will use this to ensure we don't look more than 2 weeks ahead
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, 14);
        long twoWeeksFromNowMillis = calendar.getTimeInMillis();

        // We will use this to ensure we only show instructors teaching this week in the subject line
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, 7);
        long oneWeekFromNowMillis = calendar.getTimeInMillis();

        for (Event event : eventList) {
            // Skip events that start with a "-" since that denotes a special day and not a teaching event
            if (event.getSummary().startsWith("-")) {
                continue;
            }

            DateTime startDateTime = event.getStart().getDate();

            if (startDateTime.getValue() > twoWeeksFromNowMillis) {
                continue;
            }

            if (!eventMap.containsKey(startDateTime)) {
                if (eventMap.size() > 3) {
                    break;
                }

                eventMap.put(startDateTime, new ArrayList<>());
            }

            if (startDateTime.getValue() < oneWeekFromNowMillis && eventMap.size() <= 2) {
                String instructorName = event.getSummary().split(" - ")[1];
                teachingThisWeekSet.add(instructorName);
                // teachingThisWeekSet.add(removeAllOccurrences(event.getSummary(), "", "A - ", "B - ", "C - ", "I - "));

                String dayName = DateHelper.getNameOfDay(startDateTime);

                if (sTeachingThisWeekMap.containsKey(instructorName)) {
                    sTeachingThisWeekMap.put(instructorName, sTeachingThisWeekMap.get(instructorName) + " and " + dayName);
                } else {
                    sTeachingThisWeekMap.put(instructorName, dayName);
                }
            }

            eventMap.get(startDateTime).add(event.getSummary());
        }

        StringBuilder sb = new StringBuilder();

        if (EMAIL_HEADER.length() > 0) {
            sb.append(EMAIL_HEADER).append(NEW_LINE).append(NEW_LINE);
        }

        for (Map.Entry<DateTime, List<String>> entry : eventMap.entrySet()) {
            String dayName = DateHelper.getNameOfDay(entry.getKey());

            sb.append("<b>" + dayName + " - " + DateHelper.getDateFormatted(entry.getKey()) + "</b>" + NEW_LINE);

            List<String> instructorList = entry.getValue();
            Collections.sort(instructorList);
            for (String instructor : instructorList) {
                sb.append(TAB + instructor + NEW_LINE);
            }

            sb.append(NEW_LINE);
        }

        if (EMAIL_FOOTER.length() > 0) {
            sb.append(EMAIL_FOOTER).append(NEW_LINE);
        }

        EmailBuilder emailBuilder = new EmailBuilder();

        emailBuilder.setToAndFromFields(FROM_EMAIL_ADDRESS);

        // For non-production show who we are texting and what we're texting them
        if (!sProduction) {
            sb.append(NEW_LINE).append("--~-- DEBUG --~--").append(NEW_LINE);

            List<TextMessageHelper> textMessageHelperList = getMessagesToSend();
            if (textMessageHelperList.size() > 0) {
                for (TextMessageHelper textMessageHelper : textMessageHelperList) {
                    if (textMessageHelper.contact.cellNumber.length() == 12) {
                        sb.append(NEW_LINE).append(TAB).append(textMessageHelper.contact.fullName + ": " + textMessageHelper.message);
                    } else {
                        sb.append(NEW_LINE).append(TAB).append("No cell number on file for: " + textMessageHelper.contact.fullName);
                    }
                }
            } else {
                sb.append(NEW_LINE).append("getMessagesToSend() returned an empty list");
            }
        }

        emailBuilder.subject = prepareSubjectWithTeachingEvents(teachingThisWeekSet);

        emailBuilder.body = sb.toString();

        return emailBuilder;
    }

    private static String prepareSubjectWithTeachingEvents(Set<String> instructorNameSet) {
        StringBuilder teachingThisWeekSb = new StringBuilder("[SF Kendo] " + DateHelper.getTodaysDate() + " - Teaching this week: ");

        List<String> teachingThisWeekList = new ArrayList<>();
        teachingThisWeekList.addAll(instructorNameSet);
        Collections.sort(teachingThisWeekList);

        for (String teacher : teachingThisWeekList) {
            teachingThisWeekSb.append(teacher + ", ");
        }

        return teachingThisWeekSb.toString().substring(0, teachingThisWeekSb.length() - 2);
    }
}