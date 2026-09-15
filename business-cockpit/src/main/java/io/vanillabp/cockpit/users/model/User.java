package io.vanillabp.cockpit.users.model;

import io.vanillabp.cockpit.notification.model.NotificationConfiguration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public OffsetDateTime getLastLoggedIn() {
        return lastLoggedIn;
    }

    public void setLastLoggedIn(OffsetDateTime lastLoggedIn) {
        this.lastLoggedIn = lastLoggedIn;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public NotificationConfiguration getNotificationConfiguration() {
        return notificationConfiguration;
    }

    public void setNotificationConfiguration(NotificationConfiguration notificationConfiguration) {
        this.notificationConfiguration = notificationConfiguration;
    }

    public Map<String, Map<String, String>> getRecipientConfigurations() {
        return recipientConfigurations;
    }

    public void setRecipientConfigurations(Map<String, Map<String, String>> recipientConfigurations) {
        this.recipientConfigurations = recipientConfigurations;
    }

}
