package io.vanillabp.cockpit.commons.rest.adapter.feign;

import feign.Param.Expander;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class OffsetDateTimeAwareExpanderTest {

    private final OffsetDateTimeAwareExpander expander = new OffsetDateTimeAwareExpander();

    @Test
    void implementsExpander() {
        assertThat(expander).isInstanceOf(Expander.class);
    }

    @Test
    void expand_withOffsetDateTime_returnsFormattedString() {
        OffsetDateTime dateTime = OffsetDateTime.of(2024, 6, 15, 10, 30, 45, 123_000_000, ZoneOffset.UTC);

        String result = expander.expand(dateTime);

        assertThat(result).isEqualTo("2024-06-15T10:30:45.123Z");
    }

    @Test
    void expand_withDifferentTimezone_convertsToUtc() {
        OffsetDateTime dateTime = OffsetDateTime.of(2024, 6, 15, 12, 30, 45, 0, ZoneOffset.ofHours(2));

        String result = expander.expand(dateTime);

        // 12:30 at +02:00 is 10:30 UTC
        assertThat(result).isEqualTo("2024-06-15T10:30:45.000Z");
    }

    @Test
    void expand_withMidnight_returnsCorrectFormat() {
        OffsetDateTime dateTime = OffsetDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        String result = expander.expand(dateTime);

        assertThat(result).isEqualTo("2024-01-01T00:00:00.000Z");
    }

    @Test
    void expand_withEndOfDay_returnsCorrectFormat() {
        OffsetDateTime dateTime = OffsetDateTime.of(2024, 12, 31, 23, 59, 59, 999_000_000, ZoneOffset.UTC);

        String result = expander.expand(dateTime);

        assertThat(result).isEqualTo("2024-12-31T23:59:59.999Z");
    }

    @Test
    void expand_withNonOffsetDateTimeValue_returnsToString() {
        String value = "test-string";

        String result = expander.expand(value);

        assertThat(result).isEqualTo("test-string");
    }

    @Test
    void expand_withInteger_returnsToString() {
        Integer value = 42;

        String result = expander.expand(value);

        assertThat(result).isEqualTo("42");
    }

    @Test
    void expand_withNegativeOffset_convertsToUtc() {
        OffsetDateTime dateTime = OffsetDateTime.of(2024, 6, 15, 8, 30, 0, 0, ZoneOffset.ofHours(-2));

        String result = expander.expand(dateTime);

        // 08:30 at -02:00 is 10:30 UTC
        assertThat(result).isEqualTo("2024-06-15T10:30:00.000Z");
    }

    @Test
    void expand_preservesMillisecondPrecision() {
        OffsetDateTime dateTime = OffsetDateTime.of(2024, 6, 15, 10, 30, 45, 456_000_000, ZoneOffset.UTC);

        String result = expander.expand(dateTime);

        assertThat(result).isEqualTo("2024-06-15T10:30:45.456Z");
    }

    @Test
    void expand_withZeroMilliseconds_includesMilliseconds() {
        OffsetDateTime dateTime = OffsetDateTime.of(2024, 6, 15, 10, 30, 45, 0, ZoneOffset.UTC);

        String result = expander.expand(dateTime);

        assertThat(result).isEqualTo("2024-06-15T10:30:45.000Z");
    }
}
