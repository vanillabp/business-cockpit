package io.vanillabp.cockpit.commons.mongo.converters;

import java.math.BigDecimal;

import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import com.mongodb.lang.NonNull;

/**
 * Writes a {@link BigDecimal} to MongoDB as {@link Decimal128}, so the value keeps all its digits.
 * Despite its name this converter is used for writing.
 */
@WritingConverter
public class BigDecimalReadConverter implements Converter<BigDecimal, Decimal128> {

  /**
   * Creates the converter.
   */
  public BigDecimalReadConverter() {
  }

  @Override
  public Decimal128 convert(
      @NonNull BigDecimal source) {
    return new Decimal128(source);
  }

}
