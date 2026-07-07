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
 * This is a generic entity: notifications are only the first feature relying on it, hence it lives
 * in the {@code users} package rather than the notification package. The document is upserted at
 * login time and additionally holds feature-specific data such as the notification configuration
 * (AC tech 5/7) and the per-medium recipient configuration values (AC tech 10).
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
     * The e-mail known from {@code UserDetails#getEmail()} at login. Used only as a default
     * suggestion; the address the user is actually notified at is the editable per-medium value
     * kept in {@link #recipientConfigurations}.
     */
    private String email;

    /**
     * The user's preferred locale. {@code null} means "use the application default locale"
     * ({@code business-cockpit.default-locale}). Setting/changing it is the subject of a separate
     * story; today all UIs are fixed to German, so this stays {@code null} for now but the
     * notification templating already honors it.
     */
    private Locale locale;

    /** The user's notification configuration object tree (AC func 4 / tech 7). */
    private NotificationConfiguration notificationConfiguration;

    /**
     * Per-medium recipient configuration values, keyed by the medium type (e.g. {@code "email"})
     * mapping to the raw values persisted by
     * {@code NotificationService#saveRecipientConfiguration} (e.g. {@code {"emailAddress": "a@b.c"}}).
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
