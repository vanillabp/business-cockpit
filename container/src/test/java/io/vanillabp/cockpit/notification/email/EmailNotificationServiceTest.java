package io.vanillabp.cockpit.notification.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.notification.NotificationProperties;
import io.vanillabp.cockpit.notification.NotificationType;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.users.model.UserRepository;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import reactor.core.publisher.Mono;

class EmailNotificationServiceTest {

    private GreenMail greenMail;

    private EmailNotificationService service;

    @BeforeEach
    void setUp() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();

        final var mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(greenMail.getSmtp().getPort());

        final var user = new User();
        user.setId("u1");
        user.setEmail("u1@example.org");
        user.setLocale(java.util.Locale.ENGLISH); // template builds English URL paths (/task, /tasks)
        final var userRepository = mock(UserRepository.class);
        when(userRepository.findById("u1")).thenReturn(Mono.just(user));
        when(userRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        final var applicationProperties = new ApplicationProperties();
        applicationProperties.setApplicationUri("http://cockpit.example.org/");

        final var notificationProperties = new NotificationProperties();
        notificationProperties.getSmtp().setFrom("cockpit@example.org");

        service = new EmailNotificationService(
                notificationProperties,
                userRepository,
                mailSender,
                applicationProperties);
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }

    @Test
    void sendNotification_deliversMail_withLinksAndForcedHint() throws Exception {
        final var task = new UserTask();
        task.setId("task-1");
        task.setTitle(Map.of("en", "Approve invoice"));
        task.setWorkflowTitle(Map.of("en", "Invoice process"));
        task.setBusinessId("INV-42");
        task.setNotificationType(NotificationType.CREATED);
        task.setForced(true);

        service.sendNotification(List.of("u1"), task);

        assertTrue(greenMail.waitForIncomingEmail(5000, 1), "no e-mail received");
        final MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);

        final var message = received[0];
        assertEquals("u1@example.org", message.getAllRecipients()[0].toString());
        assertTrue(message.getSubject().contains("New user task"), message.getSubject());

        final var body = (String) message.getContent();
        assertTrue(body.contains("http://cockpit.example.org/task/task-1"), body);
        assertTrue(body.contains("http://cockpit.example.org/tasks"), body);
        assertTrue(body.contains("Approve invoice"), body);
        assertTrue(body.toLowerCase().contains("forced")
                || body.contains("cannot be switched off"), body);
    }

    @Test
    void resolveLocale_usesUserLocale_elseApplicationDefault() {
        final var withoutLocale = new User();
        // application default is German (set via ApplicationProperties default)
        assertEquals(java.util.Locale.GERMAN, service.resolveLocale(withoutLocale));
        assertEquals(java.util.Locale.GERMAN, service.resolveLocale(null));

        final var withLocale = new User();
        withLocale.setLocale(java.util.Locale.FRENCH);
        assertEquals(java.util.Locale.FRENCH, service.resolveLocale(withLocale));
    }

}
