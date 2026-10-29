package io.vanillabp.cockpit.commons.utils;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Turns dates of other forms into {@link OffsetDateTime} values in UTC.
 */
public class DateTimeUtil {

    private static final ThreadLocal<SimpleDateFormat> dateFormatter = new ThreadLocal<>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };

    /**
     * There is no instance, the class only has static methods.
     */
    private DateTimeUtil() {
    }
    
    /**
     * Reads a date like {@code 2024-01-31}. The date is read in the time zone of the JVM, and the
     * moment it starts is returned in UTC.
     *
     * @param dateString The date in the form {@code yyyy-MM-dd}
     * @return The start of the date, or {@code null} if the text is empty
     */
    public static OffsetDateTime toOffsetDate(final String dateString) {
        
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        try {
            return dateFormatter.get().parse(dateString).toInstant().atOffset(ZoneOffset.UTC);
        } catch (final Exception e) {
            throw new RuntimeException("Wrong date format '" + dateString + "'", e);
        }
        
    }

    /**
     * Turns milliseconds since the epoch into a moment in UTC.
     *
     * @param input The milliseconds since 1970-01-01T00:00Z
     * @return The moment
     */
    public static OffsetDateTime fromMilliseconds(final long input) {
        return fromDate(new Date(input));
    }

    /**
     * Turns a {@link Date} into a moment in UTC.
     *
     * @param input The date
     * @return The moment, or {@code null} if there is no date
     */
    public static OffsetDateTime fromDate(final Date input) {
        
        if (input == null) {
            return null;
        }
        return input.toInstant().atOffset(ZoneOffset.UTC);
        
    }
    
}
