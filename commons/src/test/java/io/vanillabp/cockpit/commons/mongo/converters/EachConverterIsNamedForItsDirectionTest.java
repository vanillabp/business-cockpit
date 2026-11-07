package io.vanillabp.cockpit.commons.mongo.converters;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A converter called "Read" reads from MongoDB, and one called "Write" writes to it. Spring Data
 * goes by the annotation, not by the name, so a swapped pair still works. But somebody who
 * registers only the reading half, by its name, gets the writing half and stores the wrong type.
 */
@ExtendWith(SuppressOutputExtension.class)
class EachConverterIsNamedForItsDirectionTest {

  private static final List<Class<?>> READING = List.of(
      BigDecimalReadConverter.class,
      OffsetDateTimeReadConverter.class);

  private static final List<Class<?>> WRITING = List.of(
      BigDecimalWriteConverter.class,
      OffsetDateTimeWriteConverter.class);

  @Test
  void aReadConverterReadsAndAWriteConverterWrites() {

    READING.forEach(converter -> assertThat(converter)
        .as(converter.getSimpleName())
        .hasAnnotation(ReadingConverter.class));
    WRITING.forEach(converter -> assertThat(converter)
        .as(converter.getSimpleName())
        .hasAnnotation(WritingConverter.class));

  }

  @Test
  void theBigDecimalConvertersGoTheWayTheirNamesSay() throws Exception {

    final var stored = new Decimal128(new BigDecimal("1234.50"));
    final var value = new BigDecimal("1234.50");

    // called by reflection, so the test compiles against both the swapped and the fixed pair
    final var read = BigDecimalReadConverter.class
        .getMethod("convert", Decimal128.class)
        .invoke(new BigDecimalReadConverter(), stored);
    final var written = BigDecimalWriteConverter.class
        .getMethod("convert", BigDecimal.class)
        .invoke(new BigDecimalWriteConverter(), value);

    assertThat(read).isEqualTo(value);
    assertThat(written).isEqualTo(stored);

  }

}
