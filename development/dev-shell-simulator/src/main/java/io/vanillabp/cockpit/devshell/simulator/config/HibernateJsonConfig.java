package io.vanillabp.cockpit.devshell.simulator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateJsonConfig {

    /**
     * Support for OffsetDateTime, when storing Workflow or UserTask objects as JSON in H2.
     */
    @Bean
    public HibernatePropertiesCustomizer jsonFormatMapperCustomizer(
            final ObjectMapper objectMapper) {
        objectMapper.registerModule(new JavaTimeModule());
        return (properties) -> {
            properties.put(AvailableSettings.JSON_FORMAT_MAPPER, new JacksonJsonFormatMapper(objectMapper));
        };
    }

}
