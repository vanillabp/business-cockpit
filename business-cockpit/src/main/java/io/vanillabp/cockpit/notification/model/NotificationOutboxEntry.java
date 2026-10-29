package io.vanillabp.cockpit.notification.model;

import io.vanillabp.cockpit.notification.NotificationType;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A pending or sent notification for exactly one recipient, one user task and one medium.
 * <p>
 * The outbox is what makes a delivery survive a crash and work in a cluster. Its entries outlive
 * a restart and are retried until they are sent. A unique index on
 * {@code (userTaskId, notificationType, recipientUserId, medium)} makes an insert idempotent, so
 * the scan may propose the same notification again without producing a second message.
 */
@Document(collection = NotificationOutboxEntry.COLLECTION_NAME)
public class NotificationOutboxEntry {

    /** The MongoDB collection which holds the outbox. */
    public static final String COLLECTION_NAME = "notification_outbox";

    @Id
    private String id;

    @Version
    private Long version;

    private String userTaskId;

    private NotificationType notificationType;

    /** The medium type ({@code NotificationService#getType()}). */
    private String medium;

    private String recipientUserId;

    /** Whether the workflow module forced this notification (rendered as a hint). */
    private boolean forced;

    /**
     * How often the delivery of this entry's bulk has been attempted. Once it reaches the
     * configured maximum, the entry counts as stale and is no longer retried. Delivery starts
     * again once somebody resets the counter to 0 in MongoDB.
     */
    private int attempts;

    private OffsetDateTime createdAt;

    /** {@code null} while pending; set once the containing bulk was sent successfully. */
    private OffsetDateTime sentAt;

    /**
     * Creates an empty entry. The poller fills it, and Spring Data uses it to read an entry.
     */
    public NotificationOutboxEntry() {
    }

    /**
     * The MongoDB id of the entry.
     *
     * @return The id
     */
    public String getId() {
        return id;
    }

    /**
     * The MongoDB id of the entry.
     *
     * @param id The id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * The version Spring Data counts up on every save, so that two nodes cannot overwrite each other.
     *
     * @return The version
     */
    public Long getVersion() {
        return version;
    }

    /**
     * The version Spring Data counts up on every save, so that two nodes cannot overwrite each other.
     *
     * @param version The version
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * The user task the notification is about.
     *
     * @return The id of the user task
     */
    public String getUserTaskId() {
        return userTaskId;
    }

    /**
     * The user task the notification is about.
     *
     * @param userTaskId The id of the user task
     */
    public void setUserTaskId(String userTaskId) {
        this.userTaskId = userTaskId;
    }

    /**
     * The kind of change the notification is about.
     *
     * @return The kind of change
     */
    public NotificationType getNotificationType() {
        return notificationType;
    }

    /**
     * The kind of change the notification is about.
     *
     * @param notificationType The kind of change
     */
    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    /**
     * The medium the notification is sent through, e.g. {@code "email"}.
     *
     * @return The medium type
     */
    public String getMedium() {
        return medium;
    }

    /**
     * The medium the notification is sent through, e.g. {@code "email"}.
     *
     * @param medium The medium type
     */
    public void setMedium(String medium) {
        this.medium = medium;
    }

    /**
     * The user who gets the notification.
     *
     * @return The id of the recipient
     */
    public String getRecipientUserId() {
        return recipientUserId;
    }

    /**
     * The user who gets the notification.
     *
     * @param recipientUserId The id of the recipient
     */
    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    /**
     * Whether the workflow module forced this notification. The message then shows a hint.
     *
     * @return {@code true} if the notification was forced
     */
    public boolean isForced() {
        return forced;
    }

    /**
     * Whether the workflow module forced this notification. The message then shows a hint.
     *
     * @param forced {@code true} if the notification was forced
     */
    public void setForced(boolean forced) {
        this.forced = forced;
    }

    /**
     * How often the delivery of this entry's bulk has been attempted.
     *
     * @return The number of attempts
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * How often the delivery of this entry's bulk has been attempted.
     *
     * @param attempts The number of attempts
     */
    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    /**
     * When the entry was written into the outbox.
     *
     * @return The time of creation
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * When the entry was written into the outbox.
     *
     * @param createdAt The time of creation
     */
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * When the bulk of this entry was sent, or {@code null} while it is pending.
     *
     * @return The time of sending
     */
    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    /**
     * When the bulk of this entry was sent, or {@code null} while it is pending.
     *
     * @param sentAt The time of sending
     */
    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

}
