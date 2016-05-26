package models;

import com.google.gson.annotations.SerializedName;

/**
 * Contact.java - created by Mark on 5/25/16
 *
 * Represents a person that we will contact. POJO that we use with GSON to
 * serialize the data in instructors.json into.
 */
public class Contact {

    @SerializedName("id") public String id = "";
    @SerializedName("email") public String email = "";
    @SerializedName("fullName") public String fullName = "";
    @SerializedName("cellNumber") public String cellNumber = "";
    @SerializedName("debug") public boolean debug = false;

    public Contact() {

    }

    public boolean isValid() {
        return email != "" && fullName != "";
    }

    public String toString() {
        return fullName + " (" + email + ") " + " - " + cellNumber + " - " + debug;
    }
}
