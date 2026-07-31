package io.vanillabp.cockpit.config.web;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.TimeZone;

/**
 * Jackson 3 configuration for the GUI API, migrated from Jackson 2 in T16.
 * <p>
 * Four things changed beyond the package rename:
 * <ul>
 * <li>the {@code JavaTimeModule} bean is gone - Jackson 3 has the Java 8 date and time types built in;</li>
 * <li>the date related flags moved from {@code SerializationFeature} to {@link DateTimeFeature}, so they
 * are set with {@code enable}/{@code disable} instead of {@code featuresToEnable}/{@code featuresToDisable};</li>
 * <li>{@code serializationInclusion(..)} became {@code changeDefaultPropertyInclusion(..)}, which takes an
 * operator on the existing value rather than a plain value;</li>
 * <li>the reactive codecs are {@code JacksonJsonEncoder}/{@code JacksonJsonDecoder}, and the
 * {@code ServerCodecConfigurer} methods lost the "2": {@code jacksonJsonEncoder(..)}. The codecs are
 * typed on {@link JsonMapper} rather than on the {@code ObjectMapper} base class, so that is what gets
 * injected here - Spring Boot 4 auto-configures exactly that bean ("jacksonJsonMapper"), built from the
 * {@code JsonMapperBuilderCustomizer} beans above.</li>
 * </ul>
 * The resulting wire format is deliberately unchanged: ISO-8601 timestamps normalised to UTC, indented
 * output, {@code null} properties omitted.
 * <p>
 * <p>Measured while writing the tests for this class: Jackson 3 already defaults
 * {@code WRITE_DATES_AS_TIMESTAMPS} to <em>off</em>, whereas Jackson 2 defaulted it to <em>on</em>.
 * Removing the explicit {@code disable(..)} therefore changes nothing today. It is kept anyway, so a
 * future default flip cannot silently turn timestamps back into numbers. The flag that does carry the
 * output is {@code defaultTimeZone(UTC)}.
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

    @Bean
    public JacksonJsonEncoder jacksonJsonEncoder(
            final JsonMapper mapper) {

        return new JacksonJsonEncoder(mapper);

    }

    @Bean
    public JacksonJsonDecoder jacksonJsonDecoder(
            final JsonMapper mapper) {

        return new JacksonJsonDecoder(mapper);

    }

    @Bean
    public WebFluxConfigurer webFluxConfigurer(
            final JacksonJsonEncoder encoder,
            final JacksonJsonDecoder decoder) {

        return new WebFluxConfigurer() {
            @Override
            public void configureHttpMessageCodecs(
                    final ServerCodecConfigurer configurer) {

                configurer.defaultCodecs().jacksonJsonEncoder(encoder);
                configurer.defaultCodecs().jacksonJsonDecoder(decoder);

            }
        };

    }

}
