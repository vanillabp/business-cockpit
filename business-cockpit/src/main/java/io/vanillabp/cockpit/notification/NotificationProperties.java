package io.vanillabp.cockpit.notification;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the notification feature ({@code business-cockpit.notification.*}).
 */
@ConfigurationProperties(prefix = NotificationProperties.PREFIX, ignoreUnknownFields = true)
public class NotificationProperties {

  /** The prefix of every property of the notification feature. */
  public static final String PREFIX = "business-cockpit.notification";

  /** What a template directory starts with to be read from the classpath. */
  public static final String CLASSPATH_PREFIX = "classpath:";

  /**
   * The classpath spelling a workflow module may write for the templates of its titles, kept here
   * so that the same value means the same thing on both sides of the cockpit. Every jar is
   * searched either way, because the class loader is asked per template.
   */
  public static final String EVERY_CLASSPATH_PREFIX = "classpath*:";

  /**
   * The key of {@code task-path} and {@code task-list-path} which holds the path for every language
   * without a key of its own.
   */
  public static final String EVERY_OTHER_LANGUAGE = "default";

  /** What a template directory starts with to be read from the file system. */
  public static final String FILE_PREFIX = "file:";

  /**
   * How often the cockpit looks for changes of a user task worth a notification, and sends
   * them.
   */
  private Duration interval = Duration.ofMinutes(1);

  /**
   * How often the delivery of one bulk is attempted. Once an outbox entry reaches this number
   * it counts as stale and is no longer retried. The default of 120 is two hours at an interval
   * of one minute. Delivery starts again once somebody resets the {@code attempts} counter of
   * the entry in MongoDB.
   */
  private int maxDeliveryAttempts = 120;

  /**
   * Interval at which successfully sent outbox entries are cleaned up (default hourly).
   */
  private Duration cleanupInterval = Duration.ofHours(1);

  /**
   * A successfully sent outbox entry is deleted once it is older than this (default 48 hours).
   */
  private Duration cleanupSentOlderThan = Duration.ofHours(48);

  /**
   * Only a user who used the cockpit within this time is notified (default 90 days). Each request
   * of a logged-in user counts as a use. A user who was away longer is notified again after the
   * next login.
   */
  private Duration recipientsActiveWithin = Duration.ofDays(90);

  /**
   * Reference e-mail medium settings.
   */
  private Smtp smtp = new Smtp();

  /**
   * Freemarker template directory per medium type, written with {@code classpath:} or
   * {@code file:} in front of it. Defaults to
   * {@code classpath:templates/notification/<type>/} when no path is configured
   * (see {@link #templatesPath(String)}).
   */
  private Map<String, String> templates = new HashMap<>();

  /**
   * The path of the page of one user task in the user interface, per language. The links of a
   * notification lead there: the base URI of the cockpit, this path and the id of the task. The key
   * is the language of the recipient, like {@code de}, and {@value #EVERY_OTHER_LANGUAGE} holds the
   * path for every language without a key of its own. An entry configured for a language is added
   * to these defaults, so a configuration only names what differs.
   */
  private Map<String, String> taskPath = new HashMap<>(Map.of(
      EVERY_OTHER_LANGUAGE, "task",
      "de", "aufgabe"));

  /**
   * The path of the task list in the user interface, per language, the same way as
   * {@link #taskPath}.
   */
  private Map<String, String> taskListPath = new HashMap<>(Map.of(
      EVERY_OTHER_LANGUAGE, "tasks",
      "de", "aufgaben"));

  /**
   * Creates the properties with their defaults. Spring Boot calls it and then binds the
   * configured values.
   */
  public NotificationProperties() {
  }

  /**
   * Resolves the template directory of a medium type, defaulting to
   * {@code classpath:templates/notification/<type>/}, which is where the delivered templates are.
   * <p>
   * A configured value carries its own prefix, and
   * {@link AbstractTemplatingNotificationService} ends the boot of an enabled medium whose value
   * carries none. So the string this returns always says where the directory is.
   *
   * @param type the medium type (e.g. {@code "email"})
   * @return the directory holding the medium's templates, as it was configured
   */
  public String templatesPath(
      final String type) {

    final var configured = templates.get(type);
    if (configured != null) {
      return configured;
    }
    return CLASSPATH_PREFIX
        + "templates/notification/"
        + type
        + "/";

  }

  /**
   * The path of the page of one user task for a recipient who reads the given language.
   *
   * @param language the language of the recipient, like {@code de}, or an empty string if it is
   *        not known
   * @return the path of that language, or the one of {@value #EVERY_OTHER_LANGUAGE}
   */
  public String taskPathFor(
      final String language) {

    return forLanguage(taskPath, language);

  }

  /**
   * The path of the task list for a recipient who reads the given language.
   *
   * @param language the language of the recipient, like {@code de}, or an empty string if it is
   *        not known
   * @return the path of that language, or the one of {@value #EVERY_OTHER_LANGUAGE}
   */
  public String taskListPathFor(
      final String language) {

    return forLanguage(taskListPath, language);

  }

  private static String forLanguage(
      final Map<String, String> paths,
      final String language) {

    final var path = paths.get(language);
    return path != null
        ? path
        : paths.get(EVERY_OTHER_LANGUAGE);

  }

  /**
   * The property key of the template directory of a medium.
   *
   * @param type the medium type (e.g. {@code "email"})
   * @return the property key naming that medium's template directory, for a message which asks
   *         the operator to change it
   */
  public static String templatesKey(
      final String type) {

    return PREFIX
        + ".templates."
        + type;

  }

  /**
   * How often the cockpit looks for changes of a user task worth a notification, and sends them.
   *
   * @return The time between two runs of the poller
   */
  public Duration getInterval() {
    return interval;
  }

  /**
   * How often the cockpit looks for changes of a user task worth a notification, and sends them.
   *
   * @param interval The time between two runs of the poller
   */
  public void setInterval(
      Duration interval) {
    this.interval = interval;
  }

  /**
   * How often the delivery of one bulk is attempted before its outbox entry counts as stale.
   *
   * @return The number of attempts
   */
  public int getMaxDeliveryAttempts() {
    return maxDeliveryAttempts;
  }

  /**
   * How often the delivery of one bulk is attempted before its outbox entry counts as stale.
   *
   * @param maxDeliveryAttempts The number of attempts
   */
  public void setMaxDeliveryAttempts(
      int maxDeliveryAttempts) {
    this.maxDeliveryAttempts = maxDeliveryAttempts;
  }

  /**
   * How often the outbox entries which were sent are cleaned up.
   *
   * @return The time between two cleanups
   */
  public Duration getCleanupInterval() {
    return cleanupInterval;
  }

  /**
   * How often the outbox entries which were sent are cleaned up.
   *
   * @param cleanupInterval The time between two cleanups
   */
  public void setCleanupInterval(
      Duration cleanupInterval) {
    this.cleanupInterval = cleanupInterval;
  }

  /**
   * How long the cleanup keeps an outbox entry which was sent.
   *
   * @return The age of a deletable entry
   */
  public Duration getCleanupSentOlderThan() {
    return cleanupSentOlderThan;
  }

  /**
   * How long the cleanup keeps an outbox entry which was sent.
   *
   * @param cleanupSentOlderThan The age of a deletable entry
   */
  public void setCleanupSentOlderThan(
      Duration cleanupSentOlderThan) {
    this.cleanupSentOlderThan = cleanupSentOlderThan;
  }

  /**
   * How recently a user has to have used the cockpit to be notified.
   *
   * @return The time since the last use within which a user is notified
   */
  public Duration getRecipientsActiveWithin() {
    return recipientsActiveWithin;
  }

  /**
   * How recently a user has to have used the cockpit to be notified.
   *
   * @param recipientsActiveWithin The time since the last use within which a user is notified
   */
  public void setRecipientsActiveWithin(
      Duration recipientsActiveWithin) {
    this.recipientsActiveWithin = recipientsActiveWithin;
  }

  /**
   * The settings of the reference e-mail medium.
   *
   * @return The e-mail settings
   */
  public Smtp getSmtp() {
    return smtp;
  }

  /**
   * The settings of the reference e-mail medium.
   *
   * @param smtp The e-mail settings
   */
  public void setSmtp(
      Smtp smtp) {
    this.smtp = smtp;
  }

  /**
   * The Freemarker template directory per medium type, each with {@code classpath:} or {@code file:} in front.
   *
   * @return The template directories, keyed by medium type
   */
  public Map<String, String> getTemplates() {
    return templates;
  }

  /**
   * The Freemarker template directory per medium type, each with {@code classpath:} or {@code file:} in front.
   *
   * @param templates The template directories, keyed by medium type
   */
  public void setTemplates(
      Map<String, String> templates) {
    this.templates = templates;
  }

  /**
   * The path of the page of one user task in the user interface, per language.
   *
   * @return The paths, keyed by language and {@value #EVERY_OTHER_LANGUAGE}
   */
  public Map<String, String> getTaskPath() {
    return taskPath;
  }

  /**
   * The path of the page of one user task in the user interface, per language.
   *
   * @param taskPath The paths, keyed by language and {@value #EVERY_OTHER_LANGUAGE}
   */
  public void setTaskPath(
      Map<String, String> taskPath) {
    this.taskPath = taskPath;
  }

  /**
   * The path of the task list in the user interface, per language.
   *
   * @return The paths, keyed by language and {@value #EVERY_OTHER_LANGUAGE}
   */
  public Map<String, String> getTaskListPath() {
    return taskListPath;
  }

  /**
   * The path of the task list in the user interface, per language.
   *
   * @param taskListPath The paths, keyed by language and {@value #EVERY_OTHER_LANGUAGE}
   */
  public void setTaskListPath(
      Map<String, String> taskListPath) {
    this.taskListPath = taskListPath;
  }

  /**
   * Settings of the reference e-mail medium.
   */
  public static class Smtp {

    /**
     * Switches the reference e-mail {@code NotificationService} on, which sends through the
     * Spring Boot mail sender ({@code spring.mail.*}). It is off by default, so an
     * installation which does not notify anybody behaves exactly as it did.
     */
    private boolean enabled = false;

    /**
     * The {@code From} address of a notification e-mail. A real deployment should set it,
     * because many SMTP servers refuse a message without a sender.
     */
    private String from;

    /**
     * Creates the e-mail settings with their defaults, so the medium is off.
     */
    public Smtp() {
    }

    /**
     * Whether the reference e-mail medium is switched on. It is off by default.
     *
     * @return {@code true} if the e-mail medium sends notifications
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Whether the reference e-mail medium is switched on. It is off by default.
     *
     * @param enabled {@code true} if the e-mail medium sends notifications
     */
    public void setEnabled(
        boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * The {@code From} address of a notification e-mail.
     *
     * @return The sender address
     */
    public String getFrom() {
      return from;
    }

    /**
     * The {@code From} address of a notification e-mail.
     *
     * @param from The sender address
     */
    public void setFrom(
        String from) {
      this.from = from;
    }

  }

}
