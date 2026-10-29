package io.vanillabp.cockpit.simulator.common;

import java.util.Locale;

import com.devskiller.jfairy.Fairy;

/**
 * Builds the jFairy generators which produce the random names and texts of the test data.
 */
public class FairyHelper {

  private FairyHelper() {
    // only static methods
  }

  /**
   * Builds a generator for one language. The seed is taken from the clock, so every call
   * produces other data.
   *
   * @param language The language tag, e.g. {@code de} or {@code en}
   * @return The generator
   */
  public static Fairy buildFairy(
      final String language) {

    return Fairy.builder()
        .withLocale(Locale.forLanguageTag(language))
        .withRandomSeed((int) System.currentTimeMillis())
        .build();

  }
}