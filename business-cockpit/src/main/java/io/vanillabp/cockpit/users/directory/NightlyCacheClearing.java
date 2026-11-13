package io.vanillabp.cockpit.users.directory;

import java.time.ZoneId;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Empties the cache of the directory at the time {@code business-cockpit.user-directory.cache-clear-cron}
 * names, every night at 2 a.m. by default.
 * <p>
 * A user is kept for 14 hours by default. Without this clearing a user who worked in the afternoon
 * would still be kept the next morning, with the groups of the day before. Emptying the whole cache
 * at night means a change in the directory counts the next day at the latest, while the directory is
 * asked about once per user and day.
 */
public class NightlyCacheClearing implements SchedulingConfigurer {

  private static final Logger log = LoggerFactory.getLogger(NightlyCacheClearing.class);

  private final CachingUserDirectoryService directory;

  private final Optional<CronTrigger> trigger;

  NightlyCacheClearing(
      final CachingUserDirectoryService directory,
      final String cron,
      final ZoneId zone) {

    this.directory = directory;
    this.trigger = UserDirectoryProperties.CACHE_CLEAR_SWITCHED_OFF.equals(cron.trim())
        ? Optional.empty()
        : Optional.of(new CronTrigger(cron.trim(), zone));

  }

  /**
   * When the cache is emptied.
   *
   * @return The trigger, in the time zone of the server, or empty if the clearing is switched off
   */
  Optional<CronTrigger> trigger() {

    return trigger;

  }

  /**
   * Empties the cache. Spring's scheduler calls it at the time of the trigger.
   */
  void clear() {

    final var forgotten = directory.clearCache();
    log.info("Emptied the cache of the user directory as planned, {} users forgotten. Each of them is read "
        + "from the directory again at the next request.", forgotten);

  }

  @Override
  public void configureTasks(
      final ScheduledTaskRegistrar registrar) {

    trigger.ifPresent(cron -> registrar.addCronTask(new CronTask(this::clear, cron)));

  }

}
