package models;

import com.google.gson.annotations.SerializedName;

/**
 * TwilioConfiguration.java - created by Mark on 5/25/16
 *
 * POJO that we use with GSON to serialize the data in twilio_configuration.json
 * into. Used by TwilioHelper to pull in data without having to check it into
 * version control.
 */
public class TwilioConfiguration {

    @SerializedName("accountSid") public String accountSid = "";
    @SerializedName("authToken") public String authToken = "";
    @SerializedName("messageServiceSid") public String messageServiceSid = "";

    public TwilioConfiguration() {

    }

    public boolean isValid() {
        return accountSid != "" && authToken != "" && messageServiceSid != "";
    }
}
