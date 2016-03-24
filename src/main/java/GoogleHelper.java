import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.DateTime;

import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class GoogleHelper {
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "Kendo Emailer";

    /**
     * Directory to store user credentials for this application.
     */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".credentials/kendo-emailer-google.json");

    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;

    /**
     * Global instance of the scopes required by this quickstart.
     * <p>
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/calendar-java-quickstart.json
     */
    private static final List<String> SCOPES = Arrays.asList(CalendarScopes.CALENDAR_READONLY, GmailScopes.MAIL_GOOGLE_COM);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static Credential sCredential;

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    private static Credential authorize() throws IOException {
        if (sCredential != null) {
            return sCredential;
        }

        // Load client secrets.
        InputStream in = GoogleHelper.class.getResourceAsStream("/client_secret.json");

        if (in == null) {
            System.out.println("authorize - unable to open client_secret.json");
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();

        sCredential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        System.out.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return sCredential;
    }

    /**
     * Build and return an authorized Calendar client service.
     *
     * @return an authorized Calendar client service
     * @throws IOException
     */
    private static com.google.api.services.calendar.Calendar getCalendarService() throws IOException {
        Credential credential = authorize();
        return new com.google.api.services.calendar.Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static List<Event> getUpcomingCalendarEvents() throws IOException {
        // Build a new authorized API client service.
        // Note: Do not confuse this class with the com.google.api.services.calendar.model.Calendar class.
        com.google.api.services.calendar.Calendar service = getCalendarService();

        DateTime now = new DateTime(System.currentTimeMillis());
        Events events = service.events().list("primary")
                .setMaxResults(20)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        System.out.println("GoogleHelper/getUpcomingCalendarEvents size = " + events.size());

        return events.getItems();
    }

    /**
     * Build and return an authorized Gmail client service.
     *
     * @return an authorized Gmail client service
     * @throws IOException
     */
    private static Gmail getGmailService() throws IOException {
        Credential credential = authorize();
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param emailBuilder EmailBuilder object that can has data that can be used to construct the email
     * @return MimeMessage to be used to send email.
     * @throws MessagingException
     */
    private static MimeMessage createEmail(EmailBuilder emailBuilder) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        try {
            email.setFrom(new InternetAddress(emailBuilder.from));

            for (InternetAddress internetAddress : emailBuilder.toList) {
                email.addRecipient(javax.mail.Message.RecipientType.TO, internetAddress);
            }

            email.setSubject(emailBuilder.subject);
            email.setContent(emailBuilder.body, "text/html; charset=utf-8");
        } catch (Exception e) {

        }

        return email;
    }

    /**
     * Create a Message from an email
     *
     * @param email Email to be set to raw of message
     * @return Message containing base64url encoded email.
     * @throws IOException
     * @throws MessagingException
     */
    private static Message createMessageWithEmail(MimeMessage email) throws MessagingException, IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        email.writeTo(bytes);
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    /**
     * Send an email from the user's mailbox to its recipient.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param email Email to be sent.
     * @throws MessagingException
     * @throws IOException
     */
    private static void sendMessage(Gmail service, String userId, MimeMessage email) throws MessagingException, IOException {
        Message message = createMessageWithEmail(email);
        message = service.users().messages().send(userId, message).execute();

        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
    }

    public static void buildAndSendEmail(EmailBuilder emailBuilder) throws IOException {
        try {
            // Build a new authorized API client service.
            Gmail service = getGmailService();

            MimeMessage mimeMessage = createEmail(emailBuilder);
            sendMessage(service, "me", mimeMessage);
        } catch (Exception e) {
            System.out.println("GoogleHelper/buildAndSendEmail - exception thrown: " + e);
        }
    }
}