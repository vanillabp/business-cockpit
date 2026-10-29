package io.vanillabp.cockpit.commons.rest.adapter.feign;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import feign.Param.Expander;

/**
 * Writes a parameter of a Feign request as text. An {@link OffsetDateTime} is written in UTC with
 * milliseconds, like {@code 2024-01-31T12:00:00.000Z}. Every other value is written with
 * {@code toString()}.
 */
public class OffsetDateTimeAwareExpander implements Expander {

  private DateTimeFormatter formatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
      .withZone(ZoneOffset.UTC);

  /**
   * Creates the expander. Feign calls this constructor when a parameter names this class as its
   * expander.
   */
  public OffsetDateTimeAwareExpander() {
  }

  @Override
  public String expand(
      final Object value) {

    if (value instanceof OffsetDateTime) {
      return ((OffsetDateTime) value).format(formatter);
    }
    return value.toString();

  }

}
