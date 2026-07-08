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
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

/**
 * Registers the {@link NotificationPoller}. The poller only becomes active when at least one
 * {@link NotificationService} bean exists (its scheduled tick returns immediately otherwise), so
 * installations without notifications keep their exact runtime behavior (AC tech 4). This runtime
 * guard is used instead of a class-level {@code @ConditionalOnBean}, which is order-sensitive for
 * component-scanned beans.
 */
@Configuration
public class NotificationPollerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NotificationPoller notificationPoller(
            final ReactiveMongoTemplate mongoTemplate,
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
