package io.vanillabp.cockpit.notification.model;

import java.time.OffsetDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * The one document which keeps the notification pollers of a cluster in step. It holds the lease,
 * so only one node runs a cycle at a time, and it holds the cursor of the scan, so no notification
 * is lost over a restart.
 */
@Document(collection = NotificationPollState.COLLECTION_NAME)
public class NotificationPollState {

  /** The MongoDB collection which holds the state document. */
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

  /**
   * Creates an empty state. The poller fills it, and Spring Data uses it to read the state.
   */
  public NotificationPollState() {
  }

  /**
   * The id of the document, always {@link #SINGLETON_ID}.
   *
   * @return The id
   */
  public String getId() {
    return id;
  }

  /**
   * The id of the document, always {@link #SINGLETON_ID}.
   *
   * @param id The id
   */
  public void setId(
      String id) {
    this.id = id;
  }

  /**
   * The version Spring Data counts up on every save, so that two nodes cannot take the lease at the same time.
   *
   * @return The version
   */
  public Long getVersion() {
    return version;
  }

  /**
   * The version Spring Data counts up on every save, so that two nodes cannot take the lease at the same time.
   *
   * @param version The version
   */
  public void setVersion(
      Long version) {
    this.version = version;
  }

  /**
   * Until when the current lease is held. A cycle may run once this is in the past.
   *
   * @return The end of the lease
   */
  public OffsetDateTime getLockedUntil() {
    return lockedUntil;
  }

  /**
   * Until when the current lease is held. A cycle may run once this is in the past.
   *
   * @param lockedUntil The end of the lease
   */
  public void setLockedUntil(
      OffsetDateTime lockedUntil) {
    this.lockedUntil = lockedUntil;
  }

  /**
   * The node which holds the lease. It is only there to help with diagnostics.
   *
   * @return The name of the node
   */
  public String getOwner() {
    return owner;
  }

  /**
   * The node which holds the lease. It is only there to help with diagnostics.
   *
   * @param owner The name of the node
   */
  public void setOwner(
      String owner) {
    this.owner = owner;
  }

  /**
   * Up to which time the changes of user tasks have already been processed.
   *
   * @return The time the next scan starts at
   */
  public OffsetDateTime getCursor() {
    return cursor;
  }

  /**
   * Up to which time the changes of user tasks have already been processed.
   *
   * @param cursor The time the next scan starts at
   */
  public void setCursor(
      OffsetDateTime cursor) {
    this.cursor = cursor;
  }

}
