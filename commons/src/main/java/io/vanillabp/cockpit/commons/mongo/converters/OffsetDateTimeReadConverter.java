package io.vanillabp.cockpit.commons.mongo.converters;

import java.time.OffsetDateTime;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import io.vanillabp.cockpit.commons.utils.DateTimeUtil;

/**
 * Reads a date stored by MongoDB back as an OffsetDateTime.
 */
@ReadingConverter
public class OffsetDateTimeReadConverter implements Converter<Date, OffsetDateTime> {

  /**
   * Creates the converter.
   */
  public OffsetDateTimeReadConverter() {
  }

  @Override
  public OffsetDateTime convert(
      final Date source) {

    if (source.getTime() == Long.MAX_VALUE) {
      return OffsetDateTime.MAX;
    }

    return DateTimeUtil.fromDate(source);

  }

}
