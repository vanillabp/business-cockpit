package io.vanillabp.cockpit.users.directory;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.SimpleTriggerContext;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * When the directory forgets every user, and that a change in the directory counts the next day at
 * the latest. The clock is moved by hand and the task of the scheduler is run by the test, so
 * nothing waits for 2 a.m.
 */
@ExtendWith(SuppressOutputExtension.class)
class NightlyCacheClearingTest {

  private static final ZoneId VIENNA = ZoneId.of("Europe/Vienna");

  private static final ZonedDateTime AFTERNOON = ZonedDateTime.of(2026, 11, 13, 15, 0, 0, 0, VIENNA);

  private final MovableClock clock = new MovableClock(AFTERNOON.toInstant(), VIENNA);

  private final CountingDirectory directory = new CountingDirectory(
      Duration.parse(UserDirectoryProperties.DEFAULT_CACHE_DURATION), clock);

  @Test
  void byDefaultTheCacheIsEmptiedAtTwoInTheNightInTheZoneOfTheServer() {

    final var clearing = new NightlyCacheClearing(directory, UserDirectoryProperties.DEFAULT_CACHE_CLEAR_CRON, VIENNA);

    final var next = clearing
        .trigger()
        .orElseThrow()
        .nextExecution(new SimpleTriggerContext(clock));

    assertThat(next).isEqualTo(AFTERNOON
        .plusDays(1)
        .withHour(2)
        .toInstant());

  }

  @Test
  void theScheduledTaskMakesTheDirectoryAskAgain() {

    final var registrar = new ScheduledTaskRegistrar();
    new NightlyCacheClearing(directory, UserDirectoryProperties.DEFAULT_CACHE_CLEAR_CRON, VIENNA)
        .configureTasks(registrar);
    directory.getUser("jane");
    directory.getUser("jane");

    assertThat(registrar.getCronTaskList()).hasSize(1);
    final var task = registrar
        .getCronTaskList()
        .getFirst();
    assertThat(task.getExpression()).isEqualTo("0 0 2 * * *");
    task
        .getRunnable()
        .run();
    directory.getUser("jane");

    assertThat(directory.loads()).isEqualTo(2);

  }

  /**
   * A user read in the afternoon would be kept until the next morning by the 14 hours alone, with
   * the groups of the day before.
   */
  @Test
  void aUserReadInTheAfternoonIsReadAgainAfterTheNight() {

    final var registrar = new ScheduledTaskRegistrar();
    new NightlyCacheClearing(directory, UserDirectoryProperties.DEFAULT_CACHE_CLEAR_CRON, VIENNA)
        .configureTasks(registrar);
    directory.getUser("jane");

    clock.moveBy(Duration.ofHours(11));
    registrar
        .getCronTaskList()
        .getFirst()
        .getRunnable()
        .run();
    clock.moveBy(Duration.ofHours(6));
    directory.getUser("jane");

    assertThat(directory.loads()).isEqualTo(2);

  }

  @Test
  void withoutTheClearingAUserIsKeptFourteenHours() {

    directory.getUser("jane");
    clock.moveBy(Duration
        .ofHours(14)
        .minusSeconds(1));
    directory.getUser("jane");
    clock.moveBy(Duration.ofSeconds(1));
    directory.getUser("jane");

    assertThat(directory.loads()).isEqualTo(2);

  }

  @Test
  void theDashSwitchesTheClearingOff() {

    final var registrar = new ScheduledTaskRegistrar();
    final var clearing = new NightlyCacheClearing(directory, " - ", VIENNA);

    clearing.configureTasks(registrar);

    assertThat(clearing.trigger()).isEmpty();
    assertThat(registrar.getCronTaskList()).isEmpty();

  }

}
