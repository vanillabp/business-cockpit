package io.vanillabp.cockpit.notification.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vanillabp.cockpit.notification.NotificationProperties;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

/**
 * Pins down what the Spring profile 'mailpit' of {@code config/application-mailpit.yaml} is good
 * for: switching on the reference e-mail medium and pointing it at the Mailpit container of
 * {@code development/docker-compose.yaml}.
 * <p>
 * The shipped file is read and bound to the very properties the runtime uses, so a renamed property
 * fails here instead of leaving somebody wondering why no e-mail shows up. What the profile relies
 * on beyond that: {@code business-cockpit.notification.smtp.enabled} gates the
 * {@link EmailNotificationConfiguration} bean, and {@code spring.mail.host} makes Spring Boot
 * auto-configure the {@code JavaMailSender} that bean requires.
 */
class MailpitProfileTest {

    private static StandardEnvironment profile() throws IOException {

        final var environment = new StandardEnvironment();
        new YamlPropertySourceLoader()
                .load("mailpit", new ClassPathResource("config/application-mailpit.yaml"))
                .forEach(source -> environment.getPropertySources().addFirst(source));
        return environment;

    }

    @Test
    void theProfileEnablesTheEmailMediumAndSetsASender() throws IOException {
        final var notification = Binder
                .get(profile())
                .bind(NotificationProperties.PREFIX, Bindable.of(NotificationProperties.class))
                .orElseThrow(() -> new AssertionError("nothing bound below " + NotificationProperties.PREFIX));

        assertTrue(notification.getSmtp().isEnabled());
        // many SMTP servers reject a message without sender; Mailpit does not, but a real one would
        assertTrue(StringUtils.hasText(notification.getSmtp().getFrom()));
    }

    @Test
    void theProfileGatesTheEmailNotificationServiceBean() throws IOException {
        // the very property @ConditionalOnProperty of EmailNotificationConfiguration keys on
        assertEquals("true", profile().getProperty("business-cockpit.notification.smtp.enabled"));
    }

    @Test
    void theProfilePointsAtTheMailpitContainerOfTheDockerCompose() throws IOException {
        final var environment = profile();

        assertEquals("localhost", environment.getProperty("spring.mail.host"));
        assertEquals(1025, environment.getProperty("spring.mail.port", Integer.class));
    }

    @Test
    void theProfileShortensThePollIntervalForManualTesting() throws IOException {
        final var notification = Binder
                .get(profile())
                .bind(NotificationProperties.PREFIX, Bindable.of(NotificationProperties.class))
                .get();

        assertTrue(notification.getInterval().compareTo(Duration.ofMinutes(1)) < 0,
                "a manual test should not have to wait a minute: " + notification.getInterval());
    }

}
