package io.vanillabp.cockpit.adapter.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Golden samples for the {@code ObjectMapper} used to serialize Kafka messages between the workflow
 * modules and the Business Cockpit. Written against Spring Boot 3 / Jackson 2 to pin the wire format
 * down before the Jackson 3 migration (T16).
 *
 * <p>The mapper is hand-built in
 * {@link CockpitCommonAdapterKafkaConfiguration#businessCockpitProtobufObjectMapper()} with a specific
 * combination of settings: indented output, UTC time zone, ISO-8601 dates rather than timestamps, no
 * nanosecond timestamps and {@code NON_NULL} inclusion. Every one of those is observable in the
 * produced bytes, and every one of them can change silently when moving to Jackson 3, where the mapper
 * is immutable and configured through a builder and where the Java-8 time types are built in.
 *
 * <p>If a test here fails after the migration, running adapters and cockpits of different versions can
 * no longer read each other's messages. That is a data-compatibility break, not a test problem - the
 * expected strings must not be "fixed" without a deliberate decision.
 */
class CockpitCommonAdapterKafkaObjectMapperTest {

    /**
     * The mapper factory method is an instance method on the configuration class, but it does not touch
     * any instance state, so it can be exercised without a Spring context.
     */
    private final tools.jackson.databind.ObjectMapper objectMapper =
            new CockpitCommonAdapterKafkaConfiguration().businessCockpitProtobufObjectMapper();

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

        final var json = objectMapper.writeValueAsString(sample());

        // indented output, UTC-normalised ISO-8601 timestamp, unquoted BigDecimal, null field omitted
        assertThat(json).isEqualTo("""
                {
                  "text" : "hello",
                  "timestamp" : "2026-07-30T10:34:56Z",
                  "amount" : 1234.50,
                  "items" : [ "a", "b" ]
                }""");

    }

    @Test
    void datesAreWrittenAsIsoStringsNotTimestamps() {

        final var json = objectMapper.writeValueAsString(sample());

        assertThat(json).contains("\"2026-07-30T10:34:56Z\"");
        assertThat(json).doesNotContain("1785582896");

    }

    @Test
    void timestampsAreNormalisedToUtc() {

        // the input carries +02:00; the mapper's time zone is UTC
        final var json = objectMapper.writeValueAsString(sample());

        assertThat(json).contains("T10:34:56Z");
        assertThat(json).doesNotContain("+02:00");

    }

    @Test
    void nullFieldsAreOmitted() {

        final var json = objectMapper.writeValueAsString(sample());

        assertThat(json).doesNotContain("nullValue");

    }

    @Test
    void bigDecimalScaleIsPreserved() {

        final var json = objectMapper.writeValueAsString(sample());

        // 1234.50, not 1234.5 - the trailing zero is part of the value
        assertThat(json).contains("\"amount\" : 1234.50");

    }

    @Test
    void theFormatRoundTrips() {

        final var json = objectMapper.writeValueAsString(sample());

        final var readBack = objectMapper.readValue(json, Sample.class);

        assertThat(readBack.text).isEqualTo("hello");
        assertThat(readBack.amount).isEqualByComparingTo("1234.50");
        assertThat(readBack.items).containsExactly("a", "b");
        assertThat(readBack.timestamp.toInstant()).isEqualTo(sample().timestamp.toInstant());

    }

}
