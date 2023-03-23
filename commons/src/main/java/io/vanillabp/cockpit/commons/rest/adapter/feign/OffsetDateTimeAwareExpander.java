package io.vanillabp.cockpit.commons.rest.adapter.feign;

import feign.Param.Expander;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeAwareExpander implements Expander {

    private DateTimeFormatter formatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneOffset.UTC);

    @Override
    public String expand(
            final Object value) {
        
        if (value instanceof OffsetDateTime) {
            return ((OffsetDateTime) value).format(formatter);
        }
        return value.toString();
        
    }

}
