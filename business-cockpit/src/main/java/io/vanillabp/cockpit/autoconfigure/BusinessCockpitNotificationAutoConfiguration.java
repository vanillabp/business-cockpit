package io.vanillabp.cockpit.autoconfigure;

import io.vanillabp.cockpit.notification.email.EmailNotificationConfiguration;
import io.vanillabp.cockpit.notification.poller.NotificationPollerConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Telling a user about a task they have not seen: the poller which collects what is worth a message
 * and the reference medium which sends it by e-mail.
 * <p>
 * Both stay quiet on their own. The poller returns immediately while no notification service is
 * registered, and the e-mail service is only built once
 * {@code business-cockpit.notification.smtp.enabled} says so, so an installation which does not
 * notify anybody runs as it did before the feature existed.
 */
@AutoConfiguration
@Import({
    NotificationPollerConfiguration.class,
    EmailNotificationConfiguration.class
})
public class BusinessCockpitNotificationAutoConfiguration {

}
