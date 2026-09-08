package io.vanillabp.cockpit.extension.config;

/**
 * How the Business Cockpit is to load a workflow module's user interface. The two values are
 * what the cockpit's API accepts; the string form is what travels on the wire.
 */
public enum UiUriType {

  /** The cockpit opens the module's own page. */
  EXTERNAL,

  /** The cockpit loads a React micro frontend through Webpack module federation. */
  WEBPACK_MF_REACT;

  /**
   * Reads a configured value.
   *
   * @param value What was configured
   * @return The type
   * @throws IllegalArgumentException If the value names none of the types - the message lists
   *           them
   */
  public static UiUriType of(
      final String value) {

    for (final var candidate : values()) {
      if (candidate.name().equals(value)) {
        return candidate;
      }
    }
    throw new IllegalArgumentException(
        "'%s' is none of the known UI URI types! Use one of: %s."
            .formatted(value, String.join(", ", names())));

  }

  /**
   * @return The names of all types, for a message listing what may stand there
   */
  public static String[] names() {

    final var names = new String[values().length];
    for (var i = 0; i < values().length; i++) {
      names[i] = values()[i].name();
    }
    return names;

  }

}
