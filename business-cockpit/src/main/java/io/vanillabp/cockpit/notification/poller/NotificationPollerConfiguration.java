package io.vanillabp.cockpit.notification.poller;

import io.vanillabp.cockpit.notification.NotificationProperties;
import io.vanillabp.cockpit.notification.NotificationService;
import io.vanillabp.cockpit.notification.model.NotificationOutboxRepository;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.users.model.UserRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Registers the {@link NotificationPoller}. The poller only starts working once at least one
 * {@link NotificationService} bean exists, and until then its scheduled tick returns at once. So an
 * installation which does not notify anybody behaves exactly as it did. The check happens while the
 * application runs and not as a {@code @ConditionalOnBean} on the class, because that one depends
 * on the order the scanned beans are found in.
 */
@Configuration
public class NotificationPollerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NotificationPoller notificationPoller(
            final MongoTemplate mongoTemplate,
            final UserRepository userRepository,
            final UserTaskRepository userTaskRepository,
            final NotificationOutboxRepository outboxRepository,
            final List<NotificationService> notificationServices,
            final UserDetailsProvider userDirectory,
            final NotificationProperties notificationProperties) {

        return new NotificationPoller(mongoTemplate, userRepository, userTaskRepository,
                outboxRepository, notificationServices, userDirectory,
                notificationProperties.getMaxDeliveryAttempts(),
                notificationProperties.getCleanupSentOlderThan());

    }

}
