package io.vanillabp.cockpit.users.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * How long the directories keep a user, and what they do not keep.
 */
@ExtendWith(SuppressOutputExtension.class)
class CachedUsersTest {

  private static final Instant NOW = Instant.parse("2026-11-12T08:00:00Z");

  private final MovableClock clock = new MovableClock(NOW, ZoneOffset.UTC);

  private final AtomicInteger loads = new AtomicInteger();

  private Optional<UserDetails> jane(
      final String id) {

    loads.incrementAndGet();
    return Optional.of(new UserDetailsImpl(id, null, id, id, List.of("approvers")));

  }

  private Optional<UserDetails> nobody(
      final String id) {

    loads.incrementAndGet();
    return Optional.empty();

  }

  @Test
  void aUserIsAskedForOnceWhileItIsKept() {

    final var cache = new CachedUsers(Duration.ofMinutes(5), clock);

    cache.get("jane", this::jane);
    clock.moveBy(Duration.ofMinutes(4));
    final var user = cache.get("jane", this::jane);

    assertThat(user).isPresent();
    assertThat(loads).hasValue(1);

  }

  @Test
  void anExpiredUserIsAskedForAgain() {

    final var cache = new CachedUsers(Duration.ofMinutes(5), clock);

    cache.get("jane", this::jane);
    clock.moveBy(Duration.ofMinutes(5));
    cache.get("jane", this::jane);

    assertThat(loads).hasValue(2);

  }

  /**
   * Otherwise every request of a user the directory does not know would ask the directory again.
   */
  @Test
  void anUnknownUserIsKeptAsWell() {

    final var cache = new CachedUsers(Duration.ofMinutes(5), clock);

    cache.get("ghost", this::nobody);
    final var user = cache.get("ghost", this::nobody);

    assertThat(user).isEmpty();
    assertThat(loads).hasValue(1);

  }

  @Test
  void aFailureIsNotKept() {

    final var cache = new CachedUsers(Duration.ofMinutes(5), clock);

    assertThatThrownBy(() -> cache.get("jane", id -> {
      throw new IllegalStateException("down");
    })).hasMessage("down");
    final var user = cache.get("jane", this::jane);

    assertThat(user).isPresent();
    assertThat(loads).hasValue(1);

  }

  @Test
  void aDurationOfZeroAsksEveryTime() {

    final var cache = new CachedUsers(Duration.ZERO, clock);

    cache.get("jane", this::jane);
    cache.get("jane", this::jane);

    assertThat(loads).hasValue(2);

  }

}
