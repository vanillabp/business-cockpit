package io.vanillabp.cockpit.commons.mongo.converters;

import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BigDecimalWriteConverterTest {

    private final BigDecimalWriteConverter converter = new BigDecimalWriteConverter();

    @Test
    void implementsConverter() {
        assertThat(converter).isInstanceOf(Converter.class);
    }

    @Test
    void convert_simpleValue_returnsBigDecimal() {
        Decimal128 source = new Decimal128(new BigDecimal("123.45"));

        BigDecimal result = converter.convert(source);

        assertThat(result).isEqualByComparingTo(new BigDecimal("123.45"));
    }

    @Test
    void convert_zero_returnsBigDecimalZero() {
        Decimal128 source = new Decimal128(BigDecimal.ZERO);

        BigDecimal result = converter.convert(source);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void convert_negativeValue_returnsCorrectBigDecimal() {
        Decimal128 source = new Decimal128(new BigDecimal("-999.99"));

        BigDecimal result = converter.convert(source);

        assertThat(result).isEqualByComparingTo(new BigDecimal("-999.99"));
    }

    @Test
    void convert_largeValue_returnsCorrectBigDecimal() {
        BigDecimal expected = new BigDecimal("12345678901234567890.123456789");
        Decimal128 source = new Decimal128(expected);

        BigDecimal result = converter.convert(source);

        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    void convert_verySmallValue_returnsCorrectBigDecimal() {
        BigDecimal expected = new BigDecimal("0.00000000001");
        Decimal128 source = new Decimal128(expected);

        BigDecimal result = converter.convert(source);

        assertThat(result).isEqualByComparingTo(expected);
    }

    @Test
    void convert_one_returnsBigDecimalOne() {
        Decimal128 source = new Decimal128(BigDecimal.ONE);

        BigDecimal result = converter.convert(source);

        assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void convert_ten_returnsBigDecimalTen() {
        Decimal128 source = new Decimal128(BigDecimal.TEN);

        BigDecimal result = converter.convert(source);

        assertThat(result).isEqualByComparingTo(BigDecimal.TEN);
    }
}
