package io.vanillabp.cockpit.commons.mongo.converters;

import io.vanillabp.cockpit.commons.utils.DateTimeUtil;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.time.OffsetDateTime;
import java.util.Date;

/**
 * Converter for writing OffsetDateTime as a date (e.g. during MongoDb deserialization).
 */
@ReadingConverter
public class OffsetDateTimeReadConverter implements Converter<Date, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(final Date source) {
        
        if (source.getTime() == Long.MAX_VALUE) {
            return OffsetDateTime.MAX;
        }
        
        return DateTimeUtil.fromDate(source);
        
    }
    
}
