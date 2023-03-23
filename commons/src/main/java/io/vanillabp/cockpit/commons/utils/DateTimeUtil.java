package io.vanillabp.cockpit.commons.utils;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class DateTimeUtil {

    private static final ThreadLocal<SimpleDateFormat> dateFormatter = new ThreadLocal<>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };
    
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

    public static OffsetDateTime fromMilliseconds(final long input) {
        return fromDate(new Date(input));
    }

    public static OffsetDateTime fromDate(final Date input) {
        
        if (input == null) {
            return null;
        }
        return input.toInstant().atOffset(ZoneOffset.UTC);
        
    }
    
}
