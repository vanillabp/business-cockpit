package io.vanillabp.cockpit.users.model;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import io.vanillabp.cockpit.notification.model.NotificationConfiguration;

/**
 * A user known to the Business Cockpit because they logged in at least once.
 * <p>
 * The entity is a general one. Notifications are only the first feature which needs it, which is
 * why it lives in the {@code users} package and not in the notification package. The document is
 * written at login, and it also holds what single features need: the notification configuration,
 * and the recipient configuration per medium.
 */
@Document(collection = User.COLLECTION_NAME)
public class User {

  /** The name of the MongoDB collection the users are stored in. */
  public static final String COLLECTION_NAME = "users";

  /** The user id as reported by {@code UserDetails#getId()}. */
  @Id
  private String id;

  @Version
  private Long version;

  /** Timestamp of the most recent login (refreshed on login). */
  private OffsetDateTime lastLoggedIn;

  /**
   * The e-mail {@code UserDetails#getEmail()} knew at login. It is only a suggestion. The
   * address a user is notified at is the value per medium in {@link #recipientConfigurations},
   * which the user can edit.
   */
  private String email;

  /**
   * The locale the user prefers. {@code null} means the application's default locale, which is
   * {@code business-cockpit.default-locale}. Setting it is a story of its own. Today every user
   * interface is fixed to German, so this stays {@code null}, but the notification templating
   * already reads it.
   */
  private Locale locale;

  /** The tree holding what the user configured about notifications. */
  private NotificationConfiguration notificationConfiguration;

  /**
   * The recipient configuration per medium, keyed by the type of the medium, {@code "email"}
   * for example. The values are the ones
   * {@code NotificationService#saveRecipientConfiguration} stored, for example
   * {@code {"emailAddress": "a@b.c"}}.
   */
  private Map<String, Map<String, String>> recipientConfigurations;

  /**
   * Creates an empty user. Used by Spring Data when it reads a document.
   */
  public User() {

  }

  /**
   * The user id as reported by {@code UserDetails#getId()}.
   *
   * @return The id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the user id.
   *
   * @param id The id
   */
  public void setId(
      String id) {
    this.id = id;
  }

  /**
   * The version of the document, used by Spring Data for optimistic locking.
   *
   * @return The version
   */
  public Long getVersion() {
    return version;
  }

  /**
   * Sets the version of the document.
   *
   * @param version The version
   */
  public void setVersion(
      Long version) {
    this.version = version;
  }

  /**
   * When the user logged in the last time.
   *
   * @return The time of the most recent login
   */
  public OffsetDateTime getLastLoggedIn() {
    return lastLoggedIn;
  }

  /**
   * Sets the time of the most recent login.
   *
   * @param lastLoggedIn The time of the login
   */
  public void setLastLoggedIn(
      OffsetDateTime lastLoggedIn) {
    this.lastLoggedIn = lastLoggedIn;
  }

  /**
   * The e-mail address known at login. It is only a suggestion, see the field.
   *
   * @return The e-mail address
   */
  public String getEmail() {
    return email;
  }

  /**
   * Sets the e-mail address known at login.
   *
   * @param email The e-mail address
   */
  public void setEmail(
      String email) {
    this.email = email;
  }

  /**
   * The locale the user prefers.
   *
   * @return The locale, or {@code null} for the application's default locale
   */
  public Locale getLocale() {
    return locale;
  }

  /**
   * Sets the locale the user prefers.
   *
   * @param locale The locale, or {@code null} for the application's default locale
   */
  public void setLocale(
      Locale locale) {
    this.locale = locale;
  }

  /**
   * What the user configured about notifications.
   *
   * @return The notification configuration
   */
  public NotificationConfiguration getNotificationConfiguration() {
    return notificationConfiguration;
  }

  /**
   * Sets what the user configured about notifications.
   *
   * @param notificationConfiguration The notification configuration
   */
  public void setNotificationConfiguration(
      NotificationConfiguration notificationConfiguration) {
    this.notificationConfiguration = notificationConfiguration;
  }

  /**
   * The recipient configuration per medium, keyed by the type of the medium.
   *
   * @return The recipient configurations
   */
  public Map<String, Map<String, String>> getRecipientConfigurations() {
    return recipientConfigurations;
  }

  /**
   * Sets the recipient configuration per medium.
   *
   * @param recipientConfigurations The recipient configurations, keyed by the type of the medium
   */
  public void setRecipientConfigurations(
      Map<String, Map<String, String>> recipientConfigurations) {
    this.recipientConfigurations = recipientConfigurations;
  }

}
