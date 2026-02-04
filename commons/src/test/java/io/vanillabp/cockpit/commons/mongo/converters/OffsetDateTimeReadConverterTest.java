package io.vanillabp.cockpit.commons.mongo.converters;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class OffsetDateTimeReadConverterTest {

    private final OffsetDateTimeReadConverter converter = new OffsetDateTimeReadConverter();

    @Test
    void implementsConverter() {
        assertThat(converter).isInstanceOf(Converter.class);
    }

    @Test
    void convert_normalDate_returnsOffsetDateTime() {
        OffsetDateTime expected = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC);
        Date source = Date.from(expected.toInstant());

        OffsetDateTime result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.toInstant()).isEqualTo(expected.toInstant());
    }

    @Test
    void convert_maxValue_returnsOffsetDateTimeMax() {
        Date source = new Date(Long.MAX_VALUE);

        OffsetDateTime result = converter.convert(source);

        assertThat(result).isEqualTo(OffsetDateTime.MAX);
    }

    @Test
    void convert_epochTime_returnsCorrectOffsetDateTime() {
        Date source = new Date(0L);

        OffsetDateTime result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.toInstant().toEpochMilli()).isZero();
    }

    @Test
    void convert_currentDate_returnsCorrectOffsetDateTime() {
        Date source = new Date();

        OffsetDateTime result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.toInstant().toEpochMilli()).isEqualTo(source.getTime());
    }

    @Test
    void convert_specificTimestamp_returnsCorrectOffsetDateTime() {
        long timestamp = 1718448600000L; // 2024-06-15T10:30:00Z
        Date source = new Date(timestamp);

        OffsetDateTime result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.toInstant().toEpochMilli()).isEqualTo(timestamp);
    }
}
