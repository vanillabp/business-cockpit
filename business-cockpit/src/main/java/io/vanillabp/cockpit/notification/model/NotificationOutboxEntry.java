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

    public String getUserTaskId() {
        return userTaskId;
    }

    public void setUserTaskId(String userTaskId) {
        this.userTaskId = userTaskId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public String getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(String recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public boolean isForced() {
        return forced;
    }

    public void setForced(boolean forced) {
        this.forced = forced;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

}
