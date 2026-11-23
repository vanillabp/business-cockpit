package io.vanillabp.cockpit.users.model;

import java.util.Collection;
import java.util.Map;

import io.vanillabp.cockpit.exceptions.BcInvalidRequestException;

/**
 * The key Spring Data writes into a stored document to say which Java type it holds, and reads
 * back to create that type. A free value of a user which carries this key, a setting for example,
 * would make Spring Data read the whole document of the user as another type, and fail. So the
 * cockpit refuses the key wherever a user writes free keys into the user document: as the name of
 * a setting, at any depth inside its value, and in the notification and recipient configurations.
 */
public final class TypeHintKey {

  /** The key, Spring Data's default. The cockpit does not configure another one. */
  public static final String KEY = "_class";

  private TypeHintKey() {
  }

  /**
   * Refuses a value which holds the key at any depth.
   *
   * @param parameter What the value is, for the message
   * @param value A value as Jackson read it: maps, lists and plain values
   * @throws BcInvalidRequestException if the key is found
   */
  public static void refuseIn(
      final String parameter,
      final Object value) {

    if (isIn(value)) {
      throw refused(parameter);
    }

  }

  /**
   * Refuses the key as a name.
   *
   * @param parameter What the name is, for the message
   * @param name The name
   * @throws BcInvalidRequestException if the name is the key
   */
  public static void refuseAsName(
      final String parameter,
      final String name) {

    if (KEY.equals(name)) {
      throw refused(parameter);
    }

  }

  private static BcInvalidRequestException refused(
      final String parameter) {

    return new BcInvalidRequestException(
        "'%s' may not use the key '%s' at any depth, because MongoDB documents reserve it".formatted(parameter, KEY));

  }

  /**
   * Tells whether a value holds the key at any depth.
   *
   * @param value A value made of maps, lists and plain values, as Jackson reads JSON or as the
   *        MongoDB driver reads a document
   * @return Whether a map in the value has the key
   */
  public static boolean isIn(
      final Object value) {

    if (value instanceof Map<?, ?> map) {
      for (final var entry : map.entrySet()) {
        if (KEY.equals(String.valueOf(entry.getKey())) || isIn(entry.getValue())) {
          return true;
        }
      }
    } else if (value instanceof Collection<?> collection) {
      for (final var element : collection) {
        if (isIn(element)) {
          return true;
        }
      }
    }
    return false;

  }

}
