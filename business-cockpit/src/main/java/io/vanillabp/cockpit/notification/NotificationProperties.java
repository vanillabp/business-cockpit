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

    /** What a template directory starts with to be read from the classpath. */
    public static final String CLASSPATH_PREFIX = "classpath:";

    /**
     * The classpath spelling a workflow module may write for the templates of its titles, kept here
     * so that the same value means the same thing on both sides of the cockpit. Every jar is
     * searched either way, because the class loader is asked per template.
     */
    public static final String EVERY_CLASSPATH_PREFIX = "classpath*:";

    /** What a template directory starts with to be read from the file system. */
    public static final String FILE_PREFIX = "file:";

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
     * Freemarker template directory per medium type, written with {@code classpath:} or
     * {@code file:} in front of it. Defaults to
     * {@code classpath:templates/notification/<type>/} when no path is configured
     * (see {@link #templatesPath(String)}).
     */
    private Map<String, String> templates = new HashMap<>();

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
    public String templatesPath(final String type) {

        final var configured = templates.get(type);
        if (configured != null) {
            return configured;
        }
        return CLASSPATH_PREFIX + "templates/notification/" + type + "/";

    }

    /**
     * @param type the medium type (e.g. {@code "email"})
     * @return the property key naming that medium's template directory, for a message which asks
     *         the operator to change it
     */
    public static String templatesKey(final String type) {

        return PREFIX + ".templates." + type;

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
