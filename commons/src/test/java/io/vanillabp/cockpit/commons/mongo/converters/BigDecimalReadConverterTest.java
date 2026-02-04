package io.vanillabp.cockpit.commons.mongo.converters;

import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BigDecimalReadConverterTest {

    private final BigDecimalReadConverter converter = new BigDecimalReadConverter();

    @Test
    void implementsConverter() {
        assertThat(converter).isInstanceOf(Converter.class);
    }

    @Test
    void convert_simpleValue_returnsDecimal128() {
        BigDecimal source = new BigDecimal("123.45");

        Decimal128 result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.bigDecimalValue()).isEqualByComparingTo(source);
    }

    @Test
    void convert_zero_returnsDecimal128Zero() {
        BigDecimal source = BigDecimal.ZERO;

        Decimal128 result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.bigDecimalValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void convert_negativeValue_returnsCorrectDecimal128() {
        BigDecimal source = new BigDecimal("-999.99");

        Decimal128 result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.bigDecimalValue()).isEqualByComparingTo(source);
    }

    @Test
    void convert_largeValue_returnsCorrectDecimal128() {
        BigDecimal source = new BigDecimal("12345678901234567890.123456789");

        Decimal128 result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.bigDecimalValue()).isEqualByComparingTo(source);
    }

    @Test
    void convert_verySmallValue_returnsCorrectDecimal128() {
        BigDecimal source = new BigDecimal("0.00000000001");

        Decimal128 result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.bigDecimalValue()).isEqualByComparingTo(source);
    }

    @Test
    void convert_one_returnsDecimal128One() {
        BigDecimal source = BigDecimal.ONE;

        Decimal128 result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.bigDecimalValue()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void convert_ten_returnsDecimal128Ten() {
        BigDecimal source = BigDecimal.TEN;

        Decimal128 result = converter.convert(source);

        assertThat(result).isNotNull();
        assertThat(result.bigDecimalValue()).isEqualByComparingTo(BigDecimal.TEN);
    }
}
