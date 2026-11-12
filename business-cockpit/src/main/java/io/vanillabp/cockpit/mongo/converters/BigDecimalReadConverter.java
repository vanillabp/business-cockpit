package io.vanillabp.cockpit.mongo.converters;

import java.math.BigDecimal;

import org.bson.types.Decimal128;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import com.mongodb.lang.NonNull;

/**
 * Reads a {@link Decimal128} stored by MongoDB back as {@link BigDecimal}.
 */
@ReadingConverter
public class BigDecimalReadConverter implements Converter<Decimal128, BigDecimal> {

  /**
   * Creates the converter.
   */
  public BigDecimalReadConverter() {
  }

  @Override
  public BigDecimal convert(
      @NonNull Decimal128 source) {
    return source.bigDecimalValue();
  }

}
