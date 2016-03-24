import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;

import javax.mail.internet.InternetAddress;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

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

    // gradle -q buildAndSendEmail -Dexec.args="preview"
    public static void main(String[] args) {
        try {
            Set<String> argumentSet = new HashSet<>();
            for (int i = 0; i < args.length; i++) {
                argumentSet.add(args[i]);
            }

            sPreview = argumentSet.contains(PREVIEW_SEND_KEY);
            sProduction = argumentSet.contains(PRODUCTION_SEND_KEY);

            if (sPreview) {
                sProduction = false;
            }

            EMAIL_HEADER = loadStringFromFile("/email_header.txt");
            EMAIL_FOOTER = loadStringFromFile("/email_footer.txt");
            FROM_EMAIL_ADDRESS = loadStringFromFile("/email_from.txt");

            // Get a list of upcoming events
            List<Event> upcomingEvents = GoogleHelper.getUpcomingCalendarEvents();

            System.out.println("KendoEmailer/main - upcomingEvents.size() = " + upcomingEvents.size());

            // Create email object
            EmailBuilder emailBuilder = createEmailBuilderWithSubjectAndBody(upcomingEvents);
            emailBuilder.setToAndFromFields(FROM_EMAIL_ADDRESS);

            if (!sPreview && !sProduction) {
                System.out.println(emailBuilder.toString());
            } else {
                System.out.println("Sending email to " + EmailBuilder.getInstructorEmailList().size() + " people");
                GoogleHelper.buildAndSendEmail(emailBuilder);
            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

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

    static {
        TimeZone.setDefault(getOurTimeZone());
    }

    private static TimeZone getOurTimeZone() {
        return TimeZone.getTimeZone("America/Pacific");
    }

    // Given a DateTime, returns the day of the week (e.g. Monday, Tuesday)
    private static String getNameOfDay(DateTime dateTime) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(dateTime.getValue());
        calendar.setTimeZone(getOurTimeZone());
        return calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
    }

    private static String getDateFormatted(DateTime dateTime) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        dateFormat.setTimeZone(getOurTimeZone());
        return dateFormat.format(new Date(dateTime.getValue()));
    }

    private static String getTodaysDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd");
        dateFormat.setTimeZone(getOurTimeZone());
        return dateFormat.format(new Date());
    }

    public static EmailBuilder createEmailBuilderWithSubjectAndBody(List<Event> eventList) {
        LinkedHashMap<DateTime, List<String>> eventMap = new LinkedHashMap<>();

        Set<String> teachingThisWeekSet = new HashSet<>();

        for (Event event : eventList) {
            DateTime startDateTime = event.getStart().getDate();
            if (!eventMap.containsKey(startDateTime)) {
                if (eventMap.size() > 3) {
                    break;
                }

                eventMap.put(startDateTime, new ArrayList<String>());
            }

            if (eventMap.size() <= 2) {
                teachingThisWeekSet.add(event.getSummary().split(" - ")[1]);
                // teachingThisWeekSet.add(removeAllOccurrences(event.getSummary(), "", "A - ", "B - ", "C - ", "I - "));
            }

            eventMap.get(startDateTime).add(event.getSummary());
        }

        StringBuilder sb = new StringBuilder();

        if (EMAIL_HEADER.length() > 0) {
            sb.append(EMAIL_HEADER).append(NEW_LINE).append(NEW_LINE);
        }

        for (Map.Entry<DateTime, List<String>> entry : eventMap.entrySet()) {
            String dayName = getNameOfDay(entry.getKey());

            sb.append("<b>" + dayName + " - " + getDateFormatted(entry.getKey()) + "</b>" + NEW_LINE);

            List<String> instructorList = entry.getValue();
            Collections.sort(instructorList);
            for (String instructor : instructorList) {
                sb.append(TAB + instructor + NEW_LINE);
            }

            sb.append(NEW_LINE);
        }

        if (EMAIL_FOOTER.length() > 0) {
            sb.append(NEW_LINE).append(EMAIL_FOOTER).append(NEW_LINE);
        }

        EmailBuilder emailBuilder = new EmailBuilder();

        emailBuilder.subject = prepareSubjectWithTeachingEvents(teachingThisWeekSet);

        emailBuilder.body = sb.toString();

        return emailBuilder;
    }

    private static String prepareSubjectWithTeachingEvents(Set<String> instructorNameSet) {
        StringBuilder teachingThisWeekSb = new StringBuilder("[SF Kendo] " + getTodaysDate() + " - Teaching this week: ");

        List<String> teachingThisWeekList = new ArrayList<String>();
        teachingThisWeekList.addAll(instructorNameSet);
        Collections.sort(teachingThisWeekList);

        for (String teacher : teachingThisWeekList) {
            teachingThisWeekSb.append(teacher + ", ");
        }

        return teachingThisWeekSb.toString().substring(0, teachingThisWeekSb.length() - 2);
    }
}