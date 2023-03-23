package io.vanillabp.cockpit.commons.mongo;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.time.OffsetDateTime;
import java.util.Date;

/**
 * Converter for writing OffsetDateTime as a date (e.g. during MongoDb serialization).
 */
@WritingConverter
public class OffsetDateTimeWriteConverter implements Converter<OffsetDateTime, Date> {

    @Override
    public Date convert(final OffsetDateTime source) {
        
        if (source == null) {
            return null;
        }
        
        return Date.from(source.toInstant());
        
    }
    
}
