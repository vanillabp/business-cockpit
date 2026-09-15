package io.vanillabp.cockpit.commons.mongo.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.time.OffsetDateTime;
import java.util.Date;

/**
 * Writes an OffsetDateTime to MongoDB as a date.
 */
@WritingConverter
public class OffsetDateTimeWriteConverter implements Converter<OffsetDateTime, Date> {

    @Override
    public Date convert(final OffsetDateTime source) {
        
        if (source == null) {
            return null;
        }
        
        if (source.equals(OffsetDateTime.MAX)) {
            return new Date(Long.MAX_VALUE);
        }
        
        return Date.from(source.toInstant());
        
    }
    
}
