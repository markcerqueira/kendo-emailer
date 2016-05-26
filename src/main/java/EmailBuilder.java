import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import models.Contact;

import javax.mail.internet.InternetAddress;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailBuilder {
    public String subject;
    public String body;
    public String from;
    public List<InternetAddress> toList = new ArrayList<>();

    private static List<Contact> sInstructorList = new ArrayList<>();
    private static Map<String, Contact> sInstructorMap = new HashMap<>();

    private static void refreshRecipientContactList() {
        sInstructorList.clear();
        sInstructorMap.clear();

        try {
            InputStream inputStream = EmailBuilder.class.getResourceAsStream("/instructors.json");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            JsonReader jsonReader = new JsonReader(bufferedReader);

            Type CONTACT_TYPE = new TypeToken<List<Contact>>(){}.getType();
            Gson gson = new Gson();

            sInstructorList = gson.fromJson(jsonReader, CONTACT_TYPE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Contact contact : sInstructorList) {
            sInstructorMap.put(contact.id, contact);
        }

        System.out.println("refreshRecipientContactList - sInstructorList.size() = " + sInstructorList.size());
    }

    public List<InternetAddress> getToEmailList() {
        return this.toList;
    }

    public void setToAndFromFields(String fromEmail) {
        refreshRecipientContactList();

        this.from = fromEmail;
        this.toList.clear();

        for (Contact contact : sInstructorList) {
            // System.out.println(contact);

            if (!KendoEmailer.sProduction && !contact.debug) {
                continue;
            }

            try {
                this.toList.add(new InternetAddress(contact.email, contact.fullName));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

    public static Contact getContactWithId(String id) {
        return sInstructorMap.get(id);
    }
}
