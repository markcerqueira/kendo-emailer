import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;
import models.TwilioConfiguration;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * TwilioHelper.java - created by Mark on 5/25/16
 *
 * Sends text messages through the Twilio API. You need to put your Twilio
 * credentials in twilio_configuration.json for this to work properly.
 */
public class TwilioHelper {

    private TwilioConfiguration mTwilioConfiguration;

    public TwilioHelper() {
        try {
            InputStream inputStream = EmailBuilder.class.getResourceAsStream("/twilio_configuration.json");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            JsonReader jsonReader = new JsonReader(bufferedReader);

            Gson gson = new Gson();

            mTwilioConfiguration = gson.fromJson(jsonReader, TwilioConfiguration.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendTextMessage(String to, String body) throws Exception {
        if (mTwilioConfiguration == null || !mTwilioConfiguration.isValid()) {
            System.out.println("TwilioHelper/sendTextMessage - mTwilioConfiguration is null or invalid; aborting");
            return;
        }

        if (to == null || to.length() != 12) {
            System.out.println("TwilioHelper/sendTextMessage - to is null or invalid length (not 12); aborting");
            return;
        }

        // We're only texting people in the US to make sure the country code is correct
        if (!to.startsWith("+1")) {
            System.out.println("TwilioHelper/sendTextMessage - to does not start with +1; aborting");
            return;
        }

        TwilioRestClient client = new TwilioRestClient(mTwilioConfiguration.accountSid, mTwilioConfiguration.authToken);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("To", to));
        params.add(new BasicNameValuePair("Body", body));
        params.add(new BasicNameValuePair("MessagingServiceSid", mTwilioConfiguration.messageServiceSid));

        MessageFactory messageFactory = client.getAccount().getMessageFactory();
        Message message = messageFactory.create(params);

        System.out.println("TwilioHelper/sendTextMessage - message.getSid() = " + message.getSid());
    }
} 