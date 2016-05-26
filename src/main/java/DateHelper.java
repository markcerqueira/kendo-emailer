import com.google.api.client.util.DateTime;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * DateHelper.java - created by Mark on 5/25/16
 *
 * Helper class that helps with days and time.
 */
public class DateHelper {
    static {
        TimeZone.setDefault(getOurTimeZone());
    }

    private static TimeZone getOurTimeZone() {
        return TimeZone.getTimeZone("America/Pacific");
    }

    // Given a DateTime, returns the day of the week (e.g. Monday, Tuesday)
    public static String getNameOfDay(DateTime dateTime) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(dateTime.getValue());
        calendar.setTimeZone(getOurTimeZone());
        return calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
    }

    // Returns the date formatted as MM-dd-yyyy
    public static String getDateFormatted(DateTime dateTime) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        dateFormat.setTimeZone(getOurTimeZone());
        return dateFormat.format(new Date(dateTime.getValue()));
    }

    // Returns the date formatted as MM/dd
    public static String getTodaysDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd");
        dateFormat.setTimeZone(getOurTimeZone());
        return dateFormat.format(new Date());
    }
}
