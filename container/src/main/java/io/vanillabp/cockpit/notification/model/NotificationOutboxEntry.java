package io.vanillabp.cockpit.notification.model;

import io.vanillabp.cockpit.notification.NotificationType;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A pending or sent notification for exactly one recipient, one user task and one medium.
 * <p>
 * The outbox is the persistence that makes delivery cluster-safe and crash-resilient (AC tech 8/9):
 * entries survive restarts and are retried until sent. A unique index on
 * {@code (userTaskId, notificationType, recipientUserId, medium)} makes inserts idempotent so the
 * delta-scan may re-propose the same notification without producing duplicates.
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
