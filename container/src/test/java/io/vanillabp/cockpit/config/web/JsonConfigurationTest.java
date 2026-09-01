package io.vanillabp.cockpit.config.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Golden samples for the JSON format of the GUI API, pinned down during the Jackson 3 migration (T16).
 *
 * <p>{@link JsonConfiguration} does not build a mapper itself, it contributes two
 * {@code JsonMapperBuilderCustomizer} beans which Spring Boot applies to the auto-configured
 * {@code JsonMapper}. The test applies them to a plain builder in the same way, so what it asserts is the
 * format the GUI actually receives: indented output, ISO-8601 timestamps normalised to UTC, {@code null}
 * properties omitted, declaration order of properties.
 *
 * <p>Every one of those settings moved during the migration - the date flags to {@code DateTimeFeature},
 * the inclusion to {@code changeDefaultPropertyInclusion(..)} - and property ordering changed default
 * between Jackson 2 and 3. A failure here means the React frontend and the official GUI API clients see
 * different JSON than before, so the expected strings must not be "fixed" without a deliberate decision.
 */
class JsonConfigurationTest {

    private final JsonConfiguration configuration = new JsonConfiguration();

    /**
     * Applies the customizer beans to a builder exactly as Spring Boot's {@code JsonMapper}
     * auto-configuration does.
     */
    private JsonMapper mapper() {

        final var builder = JsonMapper.builder();
        configuration.jsonFormatDateTimes().customize(builder);
        configuration.jsonMinimizeOutput().customize(builder);
        return builder.build();

    }

    public static class Sample {

        public String text;

        public OffsetDateTime timestamp;

        public BigDecimal amount;

        public String nullValue;

        public List<String> items;

    }

    private Sample sample() {

        final var sample = new Sample();
        sample.text = "hello";
        sample.timestamp = OffsetDateTime.of(2026, 7, 30, 12, 34, 56, 0, ZoneOffset.ofHours(2));
        sample.amount = new BigDecimal("1234.50");
        sample.nullValue = null;
        sample.items = List.of("a", "b");
        return sample;

    }

    @Test
    void theSerializedFormIsStable() {

        assertEquals("""
                {
                  "text" : "hello",
                  "timestamp" : "2026-07-30T10:34:56Z",
                  "amount" : 1234.50,
                  "items" : [ "a", "b" ]
                }""",
                mapper().writeValueAsString(sample()));

    }

    @Test
    void datesAreIsoStringsInUtcRatherThanTimestamps() {

        final var json = mapper().writeValueAsString(sample());

        // the input carries +02:00, the mapper's time zone is UTC
        assertTrue(json.contains("\"2026-07-30T10:34:56Z\""), json);
        assertFalse(json.contains("+02:00"), json);
        assertFalse(json.contains("1785582896"), json);

    }

    @Test
    void nullFieldsAreOmitted() {

        assertFalse(mapper().writeValueAsString(sample()).contains("nullValue"));

    }

    @Test
    void theFormatRoundTrips() {

        final var mapper = mapper();

        final var readBack = mapper.readValue(mapper.writeValueAsString(sample()), Sample.class);

        assertEquals("hello", readBack.text);
        assertEquals(0, readBack.amount.compareTo(new BigDecimal("1234.50")));
        assertEquals(List.of("a", "b"), readBack.items);
        assertEquals(sample().timestamp.toInstant(), readBack.timestamp.toInstant());

    }

    /**
     * Spring Boot 4 keeps Jackson 2 support in a separate module. If {@code spring-boot-jackson2} ever
     * returns to the classpath - directly or transitively - its auto-configuration adds a second
     * {@code ObjectMapper} bean which none of the customizers above touch: dates would be written as
     * timestamps and {@code null} properties would be included. Whether that mapper is picked up depends
     * on injection points, so the failure would be silent. Keep it off the classpath instead.
     */
    @Test
    void jackson2IsNotAutoConfigured() {

        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration"));

    }

}
