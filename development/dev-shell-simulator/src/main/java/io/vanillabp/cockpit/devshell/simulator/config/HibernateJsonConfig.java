package io.vanillabp.cockpit.devshell.simulator.config;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.format.jackson.Jackson3JsonFormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class HibernateJsonConfig {

    /**
     * Support for OffsetDateTime, when storing Workflow or UserTask objects as JSON in H2.
     * <p>
     * The JavaTimeModule registration is gone: Jackson 3 has the Java 8 date and time types built in, and
     * its ObjectMapper is immutable anyway - modules have to be added through JsonMapper.builder().
     * <p>
     * Hibernate 7.4 ships two mappers side by side: {@code JacksonJsonFormatMapper} is Jackson 2 only, while
     * {@link Jackson3JsonFormatMapper} takes a Jackson 3 {@link JsonMapper}. The Jackson 2 one compiles
     * against {@code com.fasterxml.jackson.databind.ObjectMapper}, so it cannot be fed the auto-configured
     * mapper any more - hence the switch. This only affects the simulator's in-memory H2 database.
     */
    @Bean
    public HibernatePropertiesCustomizer jsonFormatMapperCustomizer(
            final JsonMapper objectMapper) {
        return (properties) -> {
            properties.put(AvailableSettings.JSON_FORMAT_MAPPER, new Jackson3JsonFormatMapper(objectMapper));
        };
    }

}
