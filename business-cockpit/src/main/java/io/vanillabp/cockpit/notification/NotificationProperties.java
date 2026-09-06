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

    public static final String PREFIX = "business-cockpit.notification";

    /**
     * Interval at which notifiable user task changes are determined and sent (AC tech 4).
     */
    private Duration interval = Duration.ofMinutes(1);

    /**
     * Maximum number of delivery attempts for a notification bulk before an outbox entry is
     * considered stale and no longer retried (default 120 = two hours at a one-minute interval).
     * Delivery resumes if the entry's {@code attempts} counter is manually reset in MongoDB.
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
     * Reference e-mail medium settings.
     */
    private Smtp smtp = new Smtp();

    /**
     * Freemarker template classpath directory per medium type. Defaults to
     * {@code templates/notification/<type>/} when no explicit path is configured
     * (see {@link #templatesPath(String)}).
     */
    private Map<String, String> templates = new HashMap<>();

    /**
     * Resolves the template classpath directory for a medium type, defaulting to
     * {@code templates/notification/<type>/}.
     *
     * @param type the medium type (e.g. {@code "email"})
     * @return the classpath directory holding the medium's templates
     */
    public String templatesPath(final String type) {

        final var configured = templates.get(type);
        if (configured != null) {
            return configured;
        }
        return "templates/notification/" + type + "/";

    }

    public Duration getInterval() {
        return interval;
    }

    public void setInterval(Duration interval) {
        this.interval = interval;
    }

    public int getMaxDeliveryAttempts() {
        return maxDeliveryAttempts;
    }

    public void setMaxDeliveryAttempts(int maxDeliveryAttempts) {
        this.maxDeliveryAttempts = maxDeliveryAttempts;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public void setCleanupInterval(Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
    }

    public Duration getCleanupSentOlderThan() {
        return cleanupSentOlderThan;
    }

    public void setCleanupSentOlderThan(Duration cleanupSentOlderThan) {
        this.cleanupSentOlderThan = cleanupSentOlderThan;
    }

    public Smtp getSmtp() {
        return smtp;
    }

    public void setSmtp(Smtp smtp) {
        this.smtp = smtp;
    }

    public Map<String, String> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, String> templates) {
        this.templates = templates;
    }

    /**
     * Settings of the reference e-mail medium.
     */
    public static class Smtp {

        /**
         * Activates the reference e-mail {@code NotificationService} using the Spring Boot mail
         * sender ({@code spring.mail.*}). Disabled by default so installations without notification
         * keep their exact runtime behavior.
         */
        private boolean enabled = false;

        /**
         * The {@code From} address of notification e-mails. Should be set for real deployments;
         * many SMTP servers reject messages without a sender.
         */
        private String from;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

    }

}
