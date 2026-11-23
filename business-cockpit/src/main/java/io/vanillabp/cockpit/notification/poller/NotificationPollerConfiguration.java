package io.vanillabp.cockpit.notification.poller;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

import io.vanillabp.cockpit.notification.NotificationProperties;
import io.vanillabp.cockpit.notification.NotificationService;
import io.vanillabp.cockpit.notification.model.NotificationOutboxRepository;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.workflowmodules.GroupHierarchyService;

/**
 * Registers the {@link NotificationPoller}. The poller only starts working once at least one
 * {@link NotificationService} bean exists, and until then its scheduled tick returns at once. So an
 * installation which does not notify anybody behaves exactly as it did. The check happens while the
 * application runs and not as a {@code @ConditionalOnBean} on the class, because that one depends
 * on the order the scanned beans are found in.
 */
@Configuration
public class NotificationPollerConfiguration {

  /**
   * Creates the configuration. Spring calls it while it reads the configuration classes.
   */
  public NotificationPollerConfiguration() {
  }

  /**
   * The poller, unless the application declares one of its own.
   *
   * @param mongoTemplate          the template the poller takes its lease and cursor with, and
   *                               reads the users with
   * @param userTaskRepository     the user tasks, to find the ones which changed
   * @param outboxRepository       the outbox the notifications wait in until they are sent
   * @param notificationServices   every medium the cockpit offers; none means the poller stays idle
   * @param userDirectory          the user directory, which has to know a user who is notified
   * @param groupHierarchy         the group hierarchy the login uses as well, so a user is
   *                               notified about every task the task list shows them
   * @param notificationProperties the notification settings, with the delivery limits
   * @return the poller
   */
  @Bean
  @ConditionalOnMissingBean
  public NotificationPoller notificationPoller(
      final MongoTemplate mongoTemplate,
      final UserTaskRepository userTaskRepository,
      final NotificationOutboxRepository outboxRepository,
      final List<NotificationService> notificationServices,
      final UserDirectoryService userDirectory,
      final GroupHierarchyService groupHierarchy,
      final NotificationProperties notificationProperties) {

    final var activeWithin = notificationProperties.getRecipientsActiveWithin();
    if ((activeWithin == null) || activeWithin.isNegative() || activeWithin.isZero()) {
      throw new IllegalStateException("'"
          + NotificationProperties.PREFIX
          + ".recipients-active-within' has to be a positive duration, like P90D for 90 days, but it is '"
          + activeWithin
          + "'. Only a user who used the cockpit within this time gets notifications.");
    }
    return new NotificationPoller(mongoTemplate, userTaskRepository, outboxRepository, notificationServices, userDirectory, groupHierarchy, notificationProperties
        .getMaxDeliveryAttempts(), notificationProperties.getCleanupSentOlderThan(), activeWithin);

  }

}
