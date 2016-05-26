package models;

/**
 * TextMessageHelper.java - created by Mark on 5/25/16
 *
 * Represents a person that we want to send a text message to. Contains a
 * Contact object and the message we want to send them. NOTE: Contacts DO NOT
 * necessarily have a valid cell phone number!
 */
public class TextMessageHelper {

    public Contact contact;
    public String message;

    // This method determines the text message that will be sent. This particular
    // implementation outputs something like: "Mark Cerqueira: here is a message
    // for you!"
    public String getMessage() {
        return this.contact.fullName + ": " + this.message;
    }
}
