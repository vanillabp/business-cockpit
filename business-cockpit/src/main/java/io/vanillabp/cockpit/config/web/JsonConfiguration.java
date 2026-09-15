package io.vanillabp.cockpit.config.web;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;

import java.util.TimeZone;

/**
 * The Jackson 3 configuration of the GUI API.
 * <p>
 * Three things changed on the way from Jackson 2, beyond the rename of the packages:
 * <ul>
 * <li>the {@code JavaTimeModule} bean is gone, because Jackson 3 has the Java 8 date and time
 * types built in;</li>
 * <li>the flags about dates moved from {@code SerializationFeature} to {@link DateTimeFeature}, so
 * they are set with {@code enable} and {@code disable} instead of {@code featuresToEnable} and
 * {@code featuresToDisable};</li>
 * <li>{@code serializationInclusion(..)} became {@code changeDefaultPropertyInclusion(..)}, which
 * takes an operator on the value at hand instead of a plain value.</li>
 * </ul>
 * Nothing else is needed. Spring Boot 4 builds the {@code JsonMapper} bean from the customizers
 * below and hands it to the message converter Spring MVC uses for request and response bodies.
 * <p>
 * What travels on the wire stays as it was, on purpose: ISO-8601 timestamps in UTC, indented
 * output, and {@code null} properties left out.
 * <p>
 * One measurement from writing the tests of this class. Jackson 3 already has
 * {@code WRITE_DATES_AS_TIMESTAMPS} switched off, where Jackson 2 had it switched on. Dropping the
 * explicit {@code disable(..)} would therefore change nothing today. It is kept anyway, so a
 * changed default cannot turn timestamps back into numbers without a word. The flag which really
 * carries the output is {@code defaultTimeZone(UTC)}.
 */
@Configuration
public class JsonConfiguration {

    @Bean
    public JsonMapperBuilderCustomizer jsonFormatDateTimes() {

        return builder -> builder
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
                .enable(DateTimeFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
                .defaultTimeZone(TimeZone.getTimeZone("UTC"));

    }

    @Bean
    public JsonMapperBuilderCustomizer jsonMinimizeOutput() {

        return builder -> builder
                // Jackson 3 sorts properties alphabetically by default; Jackson 2 used declaration order.
                // Kept as it was so API responses stay byte-stable for anything that diffs or caches them.
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .changeDefaultPropertyInclusion(
                        inclusion -> inclusion.withValueInclusion(Include.NON_NULL));

    }

}
