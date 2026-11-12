package io.vanillabp.cockpit.users.directory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A clock a test moves forward by hand, so the expiry of the cache is tested without waiting.
 */
class MovableClock extends Clock {

  private Instant now;

  private final ZoneId zone;

  MovableClock(
      final Instant now,
      final ZoneId zone) {

    this.now = now;
    this.zone = zone;

  }

  void moveBy(
      final Duration duration) {

    now = now.plus(duration);

  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(
      final ZoneId zone) {
    return new MovableClock(now, zone);
  }

  @Override
  public Instant instant() {
    return now;
  }

}
