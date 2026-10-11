package io.vanillabp.cockpit.extension.springboot.test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which aggregates somebody asked the repository to save.
 * <p>
 * JPA writes a change to a managed object whether anybody asked for it or not, so the row in the
 * database does not say who wrote it. This record does. {@link TestAggregateRepository} notes
 * every call of <code>save</code> here. That is the call VanillaBP makes after a handler method
 * when it saves the aggregate. A test which wants to know whether the platform saved reads this
 * record and not the database.
 * <p>
 * Aggregates are told apart by their token and not by their id. There is one record per JVM, but
 * every test class of this module boots a context with its own database, and the generated ids
 * start at one in each of them.
 */
public final class PlatformSaves {

  private static final Set<String> saved = ConcurrentHashMap.newKeySet();

  private PlatformSaves() {
  }

  /**
   * @param token The token of the aggregate somebody saved
   */
  static void note(
      final String token) {

    if (token != null) {
      saved.add(token);
    }

  }

  /**
   * Clears the record, so that a save in the setup of a test does not count as part of the
   * test.
   */
  public static void forget() {

    saved.clear();

  }

  /**
   * @param token The token of the aggregate asked about
   * @return Whether the repository was asked to save it since the last {@link #forget()}
   */
  public static boolean sawSaveOf(
      final String token) {

    return saved.contains(token);

  }

}
