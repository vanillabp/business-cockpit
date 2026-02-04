package io.vanillabp.cockpit.commons.mongo.converters;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class OffsetDateTimeWriteConverterTest {

    private final OffsetDateTimeWriteConverter converter = new OffsetDateTimeWriteConverter();

    @Test
    void implementsConverter() {
        assertThat(converter).isInstanceOf(Converter.class);
    }

    @Test
    void convert_null_returnsNull() {
        Date result = converter.convert(null);

        assertThat(result).isNull();
    }

    @Test
    void convert_normalValue_returnsDate() {
        OffsetDateTime source = OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        Date result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.toInstant()).isEqualTo(source.toInstant());
    }

    @Test
    void convert_maxValue_returnsMaxDate() {
        OffsetDateTime source = OffsetDateTime.MAX;

        Date result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getTime()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void convert_epochTime_returnsEpochDate() {
        OffsetDateTime source = OffsetDateTime.ofInstant(java.time.Instant.EPOCH, ZoneOffset.UTC);

        Date result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getTime()).isZero();
    }

    @Test
    void convert_currentTime_returnsCorrectDate() {
        OffsetDateTime source = OffsetDateTime.now(ZoneOffset.UTC);

        Date result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.toInstant().toEpochMilli()).isEqualTo(source.toInstant().toEpochMilli());
    }

    @Test
    void convert_withDifferentTimezone_preservesInstant() {
        OffsetDateTime source = OffsetDateTime.of(2024, 6, 15, 12, 30, 0, 0, ZoneOffset.ofHours(2));

        Date result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.toInstant()).isEqualTo(source.toInstant());
    }

    @Test
    void convert_preservesMillisecondPrecision() {
        OffsetDateTime source = OffsetDateTime.of(2024, 6, 15, 10, 30, 15, 123_000_000, ZoneOffset.UTC);

        Date result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.getTime() % 1000).isEqualTo(123);
    }
}
