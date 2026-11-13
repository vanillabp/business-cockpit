package io.vanillabp.cockpit.users.directory;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;

/**
 * A directory which knows every id and counts how often it is asked, so a test sees whether a user
 * came from the cache or from the directory.
 */
class CountingDirectory extends CachingUserDirectoryService {

  private final AtomicInteger loads = new AtomicInteger();

  CountingDirectory(
      final Duration cacheDuration,
      final Clock clock) {

    super(cacheDuration, clock);

  }

  int loads() {

    return loads.get();

  }

  @Override
  Optional<UserDetails> loadUser(
      final String id) {

    loads.incrementAndGet();
    return Optional.of(userOf(id, null, null, null, List.of("approvers")));

  }

  @Override
  List<UserDetails> searchUsers(
      final String query) {

    return List.of();

  }

  @Override
  List<UserDetails> loadAllUsers() {

    return List.of();

  }

}
