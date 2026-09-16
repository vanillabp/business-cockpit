package io.vanillabp.cockpit.extension.springboot.test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which aggregates somebody asked the repository to save.
 * <p>
 * JPA writes a change to a managed object whether anybody asked for it or not, so the row in the
 * database does not say who wrote it. This record does. {@link TestAggregateRepository} notes
 * every call of <code>save</code> here, which is the call VanillaBP makes after a handler method
 * it saves for. A test which wants to know whether the platform saved reads this and not the
 * database.
 * <p>
 * Aggregates are told apart by their token and not by their id. The record is one per JVM, while
 * every test class of this module boots a context with a database of its own, and the generated
 * ids start at one in each of them.
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
   * Starts a fresh record, so that the save of a test's setup is not mistaken for one of the
   * run under test.
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
