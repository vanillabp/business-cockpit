package io.vanillabp.cockpit.commons.mongo.converters;

import java.math.BigDecimal;

import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import com.mongodb.lang.NonNull;

/**
 * Reads a {@link Decimal128} stored by MongoDB back as {@link BigDecimal}. Despite its name this
 * converter is used for reading.
 */
@ReadingConverter
public class BigDecimalWriteConverter implements Converter<Decimal128, BigDecimal> {

  /**
   * Creates the converter.
   */
  public BigDecimalWriteConverter() {
  }

  @Override
  public BigDecimal convert(
      @NonNull Decimal128 source) {
    return source.bigDecimalValue();
  }

}
