import javax.mail.internet.InternetAddress;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class EmailBuilder {
    public String subject;
    public String body;
    public String from;
    public List<InternetAddress> toList;

    private static List<InternetAddress> sInstructorList = new ArrayList<>();

    private static void refreshRecipientContactList() {
        sInstructorList.clear();

        InputStream inputStream = EmailBuilder.class.getResourceAsStream("/instructors.csv");
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String text;

            while ((text = reader.readLine()) != null) {
                // Header that describes structure so skip
                if (text.startsWith("#")) {
                    continue;
                }

                Contact instructorContact = new Contact(text);

                if (!KendoEmailer.sProduction && !instructorContact.debug) {
                    continue;
                }

                sInstructorList.add(new InternetAddress(instructorContact.email, instructorContact.fullName));
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

        System.out.println("refreshRecipientContactList - sInstructorList.size() = " + sInstructorList.size());
    }

    public static List<InternetAddress> getInstructorEmailList() {
        return sInstructorList;
    }

    public void setToAndFromFields(String fromEmail) {
        refreshRecipientContactList();

        this.from = fromEmail;
        this.toList = sInstructorList;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Subject: " + subject + "\n");

        sb.append("From : " + from + "\n");

        sb.append("To : ");
        for (InternetAddress internetAddress : toList) {
            sb.append(internetAddress.getPersonal() + " <" + internetAddress.getAddress() + "> , ");
        }

        sb = new StringBuilder(sb.toString().substring(0, sb.length() - 3));

        sb.append("\n");

        sb.append(body);

        return sb.toString();
    }
}
