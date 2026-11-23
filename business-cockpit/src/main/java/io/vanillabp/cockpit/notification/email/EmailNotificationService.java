package io.vanillabp.cockpit.notification.email;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;

import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.exceptions.BcInvalidRequestException;
import io.vanillabp.cockpit.notification.AbstractTemplatingNotificationService;
import io.vanillabp.cockpit.notification.NotificationDeliveryException;
import io.vanillabp.cockpit.notification.NotificationProperties;
import io.vanillabp.cockpit.notification.NotificationType;
import io.vanillabp.cockpit.notification.RecipientConfiguration;
import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.users.model.UserRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

/**
 * The reference {@link io.vanillabp.cockpit.notification.NotificationService}, which sends e-mails
 * through the Spring Boot mail sender. {@link EmailNotificationConfiguration} registers it once
 * {@code business-cockpit.notification.smtp.enabled=true}, and a derived cockpit application
 * replaces it through {@code @ConditionalOnMissingBean}.
 */
public class EmailNotificationService extends AbstractTemplatingNotificationService {

  private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

  /** The technical type of the e-mail medium, see {@link #getType()}. */
  public static final String TYPE = "email";

  /** {@link RecipientConfiguration#type()} of the e-mail address value. */
  public static final String FIELD_EMAIL_ADDRESS = "emailAddress";

  private static final String TEMPLATE_HEADER = "email-header";
  private static final String TEMPLATE_BODY = "email-body";

  private final UserRepository userRepository;

  private final JavaMailSender mailSender;

  private final ApplicationProperties applicationProperties;

  /**
   * Creates the e-mail medium. {@link EmailNotificationConfiguration} calls it.
   *
   * @param notificationProperties the notification settings, with the sender address
   * @param userRepository         the users, which hold the e-mail address of each recipient
   * @param mailSender             the Spring Boot mail sender the e-mails go through
   * @param applicationProperties  the settings of the cockpit, with its default locale and its URL
   */
  public EmailNotificationService(
      final NotificationProperties notificationProperties,
      final UserRepository userRepository,
      final JavaMailSender mailSender,
      final ApplicationProperties applicationProperties) {

    super(notificationProperties);
    this.userRepository = userRepository;
    this.mailSender = mailSender;
    this.applicationProperties = applicationProperties;

  }

  @Override
  public String getType() {

    return TYPE;

  }

  @Override
  public Map<String, String> getName() {

    return Map.of("de", "E-Mail", "en", "Email");

  }

  @Override
  public List<RecipientConfiguration> getRecipientConfiguration(
      final String userId) {

    final var user = userRepository.findById(userId).orElse(null);

    String value = null;
    if (user != null && user.getRecipientConfigurations() != null && user.getRecipientConfigurations()
        .get(TYPE) != null) {
      value = user.getRecipientConfigurations().get(TYPE).get(FIELD_EMAIL_ADDRESS);
    }
    if (!StringUtils.hasText(value) && user != null) {
      // suggest the address login knew. The user may edit it, and it holds for every task
      value = user.getEmail();
    }

    return List.of(new RecipientConfiguration(
        FIELD_EMAIL_ADDRESS, Map.of("de", "E-Mail-Adresse", "en", "E-mail address"), Map.of(
            "de", "Adresse für die Benachrichtigung aller Aufgaben",
            "en", "Address used for notifications of all tasks"), value));

  }

  /**
   * Stores the address the user wants the mails at. An empty value means the address the login
   * knew. Any other value has to be one mailbox, see {@link #oneMailbox(String)}, and is refused
   * with 400 otherwise.
   */
  @Override
  public void saveRecipientConfiguration(
      final String userId,
      final Map<String, String> configurationValues) {

    final var address = configurationValues == null ? null : configurationValues.get(FIELD_EMAIL_ADDRESS);
    if (StringUtils.hasText(address) && oneMailbox(address).isEmpty()) {
      throw new BcInvalidRequestException("'"
          + FIELD_EMAIL_ADDRESS
          + "' has to be one mailbox, like 'jane@example.com' or 'Jane Doe <jane@example.com>', "
          + "and not a list of addresses or a group");
    }
    // one atomic update of this medium's entry, so a setting written at the same time is no
    // conflict
    userRepository.writeRecipientConfiguration(userId, TYPE, configurationValues);

  }

  @Override
  public void sendNotification(
      final List<String> userIds,
      final UserTask userTask) {

    final var notificationType = userTask.getNotificationType();
    final var forced = userTask.isForced();
    final var context = buildContext(userTask, notificationType);

    final var failures = new ArrayList<String>();
    Exception lastFailure = null;
    for (final var userId : userIds) {
      try {
        sendToRecipient(userId, userTask, notificationType, forced, context);
      } catch (Exception e) {
        logger.error("Could not send notification e-mail to user '{}'", userId, e);
        failures.add(userId);
        lastFailure = e;
      }
    }

    if (!failures.isEmpty()) {
      // name the failed recipients so the poller retries only those and does not send the
      // e-mail again to the recipients already reached
      throw new NotificationDeliveryException(failures, lastFailure);
    }

  }

  private void sendToRecipient(
      final String userId,
      final UserTask userTask,
      final NotificationType notificationType,
      final boolean forced,
      final Map<String, Object> context) throws Exception {

    final var user = userRepository.findById(userId).orElse(null);
    final var configured = resolveAddress(user);
    if (!StringUtils.hasText(configured)) {
      logger.info("Skipping notification e-mail: user '{}' has no e-mail address configured", userId);
      return;
    }
    final var address = oneMailbox(configured).orElse(null);
    if (address == null) {
      // stored before addresses were checked. Sending again would not change it, so this is no
      // failure the poller should repeat
      logger.warn("Skipping notification e-mail: the address of user '{}' is not one mailbox. "
          + "The user can set a new one in the notification settings.", userId);
      return;
    }

    final var userDetails = toUserDetails(user);
    final var locale = resolveLocale(user);
    final var contextOfTheRecipient = withThePathsOfTheLanguage(context, locale);
    // subject is always plain text; the body may be HTML when an .ftlh template is used
    final var subject = render(
        TEMPLATE_HEADER, contextOfTheRecipient, userDetails, notificationType, forced, locale);
    final var body = renderContent(
        TEMPLATE_BODY, contextOfTheRecipient, userDetails, notificationType, forced, locale);
    if (subject == null || body == null) {
      throw new IllegalStateException("Missing e-mail template (header and/or body)");
    }

    final var message = mailSender.createMimeMessage();
    final var helper = new MimeMessageHelper(message, "UTF-8");
    final var from = notificationProperties.getSmtp().getFrom();
    if (StringUtils.hasText(from)) {
      helper.setFrom(from);
    }
    helper.setTo(address);
    helper.setSubject(subject.strip());
    helper.setText(body.content(), body.html());
    mailSender.send(message);

  }

  /**
   * The locale a notification is rendered in for a recipient. It is the locale the user
   * prefers, and where the user has none, the application's default locale
   * ({@code business-cockpit.default-locale}). Without that, it is {@link Locale#ROOT}. Then the
   * template without a language in its name is taken. FreeMarker would otherwise fall back to the
   * locale of the machine, and the same mail would read differently on another server.
   */
  Locale resolveLocale(
      final User user) {

    if (user != null && user.getLocale() != null) {
      return user.getLocale();
    }
    final var defaultLocale = applicationProperties.getDefaultLocale();
    return defaultLocale != null ? defaultLocale : Locale.ROOT;

  }

  /**
   * The paths of the user interface differ per language, and so do the links of the mail. Each
   * recipient reads the mail in their own language, so each gets the paths of it.
   */
  private Map<String, Object> withThePathsOfTheLanguage(
      final Map<String, Object> context,
      final Locale locale) {

    final var result = new HashMap<>(context);
    result.put("taskPath", notificationProperties.taskPathFor(locale.getLanguage()));
    result.put("taskListPath", notificationProperties.taskListPathFor(locale.getLanguage()));
    return result;

  }

  /**
   * Reads a value as one mailbox. A group like {@code team: a@example.com, b@example.com;} is
   * one address to the mail library, but the mail server delivers it to each of its members. So
   * it does not count, and neither does a list of addresses or an address without a domain.
   *
   * @param value an address as a user or the login gave it
   * @return the mailbox, or empty if the value is not exactly one mailbox
   */
  static Optional<InternetAddress> oneMailbox(
      final String value) {

    try {
      final var parsed = InternetAddress.parse(value, true);
      if ((parsed.length != 1) || parsed[0].isGroup() || !parsed[0].getAddress().contains("@")) {
        return Optional.empty();
      }
      parsed[0].validate();
      return Optional.of(parsed[0]);
    } catch (AddressException e) {
      return Optional.empty();
    }

  }

  private String resolveAddress(
      final User user) {

    if (user == null) {
      return null;
    }
    if (user.getRecipientConfigurations() != null && user.getRecipientConfigurations().get(TYPE) != null) {
      final var configured = user.getRecipientConfigurations().get(TYPE).get(FIELD_EMAIL_ADDRESS);
      if (StringUtils.hasText(configured)) {
        return configured;
      }
    }
    return user.getEmail();

  }

  private Map<String, Object> buildContext(
      final UserTask userTask,
      final NotificationType notificationType) {

    final var context = new HashMap<String, Object>();
    context.put("userTask", userTask);
    context.put("taskTitle", pickLanguage(userTask.getTitle()));
    context.put("workflowTitle", pickLanguage(userTask.getWorkflowTitle()));
    context.put("taskDefinitionTitle", pickLanguage(userTask.getTaskDefinitionTitle()));
    context.put("businessId", userTask.getBusinessId());
    context.put("notificationTypeName", notificationType == null ? null : notificationType.name());
    // the paths behind the base URI differ per language, so they are added per recipient
    context.put("baseUri", baseUri());
    return context;

  }

  private String baseUri() {

    final var uri = applicationProperties.getApplicationUri();
    if (uri == null) {
      return "";
    }
    return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;

  }

  private static String pickLanguage(
      final Map<String, String> translatable) {

    if (translatable == null || translatable.isEmpty()) {
      return null;
    }
    final var english = translatable.get("en");
    if (english != null) {
      return english;
    }
    return translatable.values().iterator().next();

  }

  private static UserDetails toUserDetails(
      final User user) {

    final var id = user == null ? null : user.getId();
    final var email = user == null ? null : user.getEmail();
    return new UserDetails() {
      @Override
      public String getId() {
        return id;
      }

      @Override
      public String getEmail() {
        return email;
      }

      @Override
      public String getDisplay() {
        return id;
      }

      @Override
      public String getDisplayShort() {
        return id;
      }

      @Override
      public List<String> getAuthorities() {
        return List.of();
      }
    };

  }

}
