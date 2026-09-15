package io.vanillabp.cockpit.notification.email;

import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.notification.NotificationProperties;
import io.vanillabp.cockpit.users.model.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Registers the reference e-mail {@link EmailNotificationService}, gated by
 * {@code business-cockpit.notification.smtp.enabled=true} and overridable via
 * {@code @ConditionalOnMissingBean}. The conditions stand on a {@code @Bean} method and not on the
 * component itself, because that is the placement which works. A {@code @ConditionalOnMissingBean}
 * on the component named the component itself and kept it from registering.
 * <p>
 * Requires a {@link JavaMailSender}, which Spring Boot auto-configures when {@code spring.mail.host}
 * is set.
 */
@Configuration
public class EmailNotificationConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "business-cockpit.notification.smtp", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(EmailNotificationService.class)
    public EmailNotificationService emailNotificationService(
            final NotificationProperties notificationProperties,
            final UserRepository userRepository,
            final JavaMailSender mailSender,
            final ApplicationProperties applicationProperties) {

        return new EmailNotificationService(
                notificationProperties, userRepository, mailSender, applicationProperties);

    }

}
