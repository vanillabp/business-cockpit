package io.vanillabp.cockpit.notification.model;

import java.time.OffsetDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Singleton document coordinating the notification poller across a cluster: it holds the lease
 * (so only one node runs a cycle at a time, AC tech 8) and the delta-scan cursor (so notifications
 * are not lost across restarts, AC tech 9).
 */
@Document(collection = NotificationPollState.COLLECTION_NAME)
public class NotificationPollState {

    public static final String COLLECTION_NAME = "notification_poll_state";

    /** Fixed id of the single state document. */
    public static final String SINGLETON_ID = "poller";

    @Id
    private String id;

    @Version
    private Long version;

    /** Until when the current lease is held; a cycle may run once this is in the past. */
    private OffsetDateTime lockedUntil;

    /** Node currently holding the lease (diagnostics). */
    private String owner;

    /** Up to which timestamp user task changes have already been processed. */
    private OffsetDateTime cursor;

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

    public OffsetDateTime getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(OffsetDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public OffsetDateTime getCursor() {
        return cursor;
    }

    public void setCursor(OffsetDateTime cursor) {
        this.cursor = cursor;
    }

}
