package io.vanillabp.cockpit.notification.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.exceptions.BcInvalidRequestException;
import io.vanillabp.cockpit.notification.NotificationProperties;
import io.vanillabp.cockpit.notification.NotificationType;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.users.model.UserRepository;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class EmailNotificationServiceTest {

  private GreenMail greenMail;

  private EmailNotificationService service;

  private ApplicationProperties applicationProperties;

  private UserRepository userRepository;

  private NotificationProperties notificationProperties;

  private JavaMailSenderImpl mailSender;

  @BeforeEach
  void setUp() {
    greenMail = new GreenMail(ServerSetupTest.SMTP);
    greenMail.start();

    mailSender = new JavaMailSenderImpl();
    mailSender.setHost("localhost");
    mailSender.setPort(greenMail.getSmtp().getPort());

    final var user = new User();
    user.setId("u1");
    user.setEmail("u1@example.org");
    user.setLocale(java.util.Locale.ENGLISH); // template builds English URL paths (/task, /tasks)
    userRepository = mock(UserRepository.class);
    when(userRepository.findById("u1")).thenReturn(java.util.Optional.of(user));

    applicationProperties = new ApplicationProperties();
    applicationProperties.setApplicationUri("http://cockpit.example.org/");

    notificationProperties = new NotificationProperties();
    notificationProperties.getSmtp().setFrom("cockpit@example.org");

    service = new EmailNotificationService(
        notificationProperties, userRepository, mailSender, applicationProperties);
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
    assertTrue(body.toLowerCase().contains("forced") || body.contains("cannot be switched off"), body);
  }

  /**
   * The library has no default locale. Without one the template without a language in its name
   * is taken, and not the one of the machine's locale.
   */
  @Test
  void resolveLocale_withoutAnApplicationDefaultIsTheRootLocale() {
    // the module container sets English, the library sets nothing
    assertNull(new ApplicationProperties().getDefaultLocale());
    assertEquals(java.util.Locale.ROOT, service.resolveLocale(new User()));
    assertEquals(java.util.Locale.ROOT, service.resolveLocale(null));
  }

  @Test
  void resolveLocale_usesUserLocale_elseApplicationDefault() {
    applicationProperties.setDefaultLocale(java.util.Locale.GERMAN);
    final var withoutLocale = new User();
    assertEquals(java.util.Locale.GERMAN, service.resolveLocale(withoutLocale));
    assertEquals(java.util.Locale.GERMAN, service.resolveLocale(null));

    final var withLocale = new User();
    withLocale.setLocale(java.util.Locale.FRENCH);
    assertEquals(java.util.Locale.FRENCH, service.resolveLocale(withLocale));
  }

  /**
   * The address is written as one atomic update of the e-mail entry, so a setting of the user
   * interface written at the same time is no conflict.
   */
  @Test
  void saveRecipientConfiguration_writesTheEntryOfTheMediumOnly() {
    service.saveRecipientConfiguration("u1", Map.of("emailAddress", "u1@example.com"));

    verify(userRepository).writeRecipientConfiguration("u1", "email", Map.of("emailAddress", "u1@example.com"));
    verify(userRepository, never()).save(any());
  }

  /**
   * The address a user sets is one mailbox. A group, which a mail server would deliver to each of
   * its members, is refused, and so is a list of addresses.
   */
  @Test
  void anAddressWhichIsNotOneMailboxIsRefused() {
    for (final var notOneMailbox : List.of(
        "team: a@example.org, b@example.org;",
        "a@example.org, b@example.org",
        "no address")) {
      assertThrows(
          BcInvalidRequestException.class,
          () -> service.saveRecipientConfiguration("u1", Map.of("emailAddress", notOneMailbox)),
          notOneMailbox);
    }
    verify(userRepository, never()).writeRecipientConfiguration(any(), any(), any());
  }

  /** One mailbox is stored, with or without a name, and so is an empty value, which means the address of the login. */
  @Test
  void oneMailboxIsStored() {
    for (final var oneMailbox : List.of("u1@example.com", "Jane Doe <u1@example.com>", "")) {
      service.saveRecipientConfiguration("u1", Map.of("emailAddress", oneMailbox));
      verify(userRepository).writeRecipientConfiguration("u1", "email", Map.of("emailAddress", oneMailbox));
    }
  }

  /**
   * An address stored before the check is not mailed to when it is not one mailbox. The other
   * recipients of the same notification get theirs.
   */
  @Test
  void aStoredAddressWhichIsNotOneMailboxGetsNoMail() throws Exception {
    final var team = new User();
    team.setId("team");
    team.setRecipientConfigurations(Map.of("email", Map.of("emailAddress", "team: a@example.org, b@example.org;")));
    when(userRepository.findById("team")).thenReturn(java.util.Optional.of(team));

    service.sendNotification(List.of("team", "u1"), aTask());

    assertTrue(greenMail.waitForIncomingEmail(5000, 1), "no e-mail received");
    final var received = greenMail.getReceivedMessages();
    assertEquals(1, received.length);
    assertEquals("u1@example.org", received[0].getAllRecipients()[0].toString());
  }

  /**
   * Without a configuration the links have the paths the library always built: German for a
   * German recipient, English for everybody else.
   */
  @Test
  void theLinksHaveThePathsOfTheLanguageOfTheRecipient() throws Exception {
    recipient("de-user", java.util.Locale.GERMAN);
    recipient("fr-user", java.util.Locale.FRENCH);

    service.sendNotification(List.of("de-user", "fr-user"), aTask());

    assertTrue(greenMail.waitForIncomingEmail(5000, 2), "no e-mails received");
    final var german = bodyOfTheMailTo("de-user@example.org");
    assertTrue(german.contains("http://cockpit.example.org/aufgabe/task-1"), german);
    assertTrue(german.contains("http://cockpit.example.org/aufgaben\""), german);
    final var french = bodyOfTheMailTo("fr-user@example.org");
    assertTrue(french.contains("http://cockpit.example.org/task/task-1"), french);
    assertTrue(french.contains("http://cockpit.example.org/tasks\""), french);
  }

  /**
   * A user interface with paths of its own names them in the configuration, and the links follow.
   * A language without a path of its own takes the one of every other language.
   */
  @Test
  void theLinksHaveTheConfiguredPaths() throws Exception {
    final var configured = new Binder(new MapConfigurationPropertySource(Map.of(
        "business-cockpit.notification.task-path.fr", "taches",
        "business-cockpit.notification.task-path.default", "tasks",
        "business-cockpit.notification.task-list-path.fr", "taches")))
        .bind(NotificationProperties.PREFIX, Bindable.ofInstance(notificationProperties))
        .get();
    service = new EmailNotificationService(configured, userRepository, mailSender, applicationProperties);
    recipient("de-user", java.util.Locale.GERMAN);
    recipient("fr-user", java.util.Locale.FRENCH);

    service.sendNotification(List.of("u1", "de-user", "fr-user"), aTask());

    assertTrue(greenMail.waitForIncomingEmail(5000, 3), "no e-mails received");
    final var french = bodyOfTheMailTo("fr-user@example.org");
    assertTrue(french.contains("http://cockpit.example.org/taches/task-1"), french);
    assertTrue(french.contains("http://cockpit.example.org/taches\""), french);
    // a configured language adds to the defaults and does not replace them
    final var german = bodyOfTheMailTo("de-user@example.org");
    assertTrue(german.contains("http://cockpit.example.org/aufgabe/task-1"), german);
    final var english = bodyOfTheMailTo("u1@example.org");
    assertTrue(english.contains("http://cockpit.example.org/tasks/task-1"), english);
    assertTrue(english.contains("http://cockpit.example.org/tasks\""), english);
  }

  private void recipient(
      final String id,
      final java.util.Locale locale) {
    final var user = new User();
    user.setId(id);
    user.setEmail(id
        + "@example.org");
    user.setLocale(locale);
    when(userRepository.findById(id)).thenReturn(java.util.Optional.of(user));
  }

  private static UserTask aTask() {
    final var task = new UserTask();
    task.setId("task-1");
    task.setTitle(Map.of("en", "Approve invoice"));
    task.setNotificationType(NotificationType.CREATED);
    return task;
  }

  private String bodyOfTheMailTo(
      final String address) throws Exception {
    for (final var message : greenMail.getReceivedMessages()) {
      if (message.getAllRecipients()[0].toString().equals(address)) {
        return (String) message.getContent();
      }
    }
    throw new AssertionError("no e-mail to "
        + address);
  }

}
