package io.vanillabp.cockpit.users.directory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;

/**
 * Keeps each user the directory loaded for a while, with its groups. The cockpit asks for the user
 * on every request, and a remote directory should not be asked that often.
 * <p>
 * An answer that the directory does not know a user is kept as well. Otherwise every request of
 * such a user would go to the directory again. A failure is not kept, so the next request asks
 * again.
 */
final class CachedUsers {

  /**
   * How many users are kept before the expired ones are removed. Expired users are removed only
   * then, so the map does not grow without end while nobody pays for a cleanup on every request.
   */
  private static final int SIZE_WHICH_STARTS_A_CLEANUP = 10_000;

  private final Map<String, CachedUser> users = new ConcurrentHashMap<>();

  private final Duration duration;

  private final Clock clock;

  private record CachedUser(
                            Optional<UserDetails> user,
                            Instant expiresAt) {
  }

  CachedUsers(
      final Duration duration,
      final Clock clock) {

    this.duration = duration;
    this.clock = clock;

  }

  /**
   * The user from the cache, or from the loader if the cache has none or an expired one.
   *
   * @param id The id of the user
   * @param loader Asks the directory. If it throws, nothing is kept and the exception goes on
   * @return The user, or empty if the directory does not know the id
   */
  Optional<UserDetails> get(
      final String id,
      final Function<String, Optional<UserDetails>> loader) {

    if (duration.isZero() || duration.isNegative()) {
      return loader.apply(id);
    }

    final var now = clock.instant();
    final var cached = users.get(id);
    if ((cached != null) && now.isBefore(cached.expiresAt())) {
      return cached.user();
    }

    // two requests at the same moment may both ask the directory. That costs one more request
    // and is cheaper than holding a lock while a remote system answers
    final var loaded = loader.apply(id);
    if (users.size() >= SIZE_WHICH_STARTS_A_CLEANUP) {
      users
          .values()
          .removeIf(user -> !now.isBefore(user.expiresAt()));
    }
    users.put(id, new CachedUser(loaded, now.plus(duration)));
    return loaded;

  }

}
