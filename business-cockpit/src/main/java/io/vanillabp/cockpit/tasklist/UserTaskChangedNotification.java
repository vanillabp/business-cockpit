package io.vanillabp.cockpit.tasklist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.bson.Document;
import org.springframework.data.mongodb.core.messaging.Message;

import com.mongodb.client.model.changestream.ChangeStreamDocument;

import io.vanillabp.cockpit.mongo.changestreams.LatestChange;
import io.vanillabp.cockpit.mongo.changestreams.OperationType;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.events.NotificationEvent;

/**
 * Tells the open browsers that a user task changed. The GUI notification service turns it into an
 * event of the update stream.
 */
public class UserTaskChangedNotification extends NotificationEvent {

  private static final long serialVersionUID = 1L;

  /** The field a user task keeps who made its latest change in. */
  private static final String FIELD_OF_THE_STORED_INITIATOR = "initiator";

  /** The id of the user task which changed. */
  private String userTaskId;

  /**
   * Who made this one change, as the cockpit stores it, or {@code null} if the change event does
   * not say it. See {@link LatestChange#initiatorCarriedBy}.
   */
  private String initiatorOfThisChange;

  /**
   * Creates the notification.
   *
   * @param type The kind of change
   * @param userTaskId The id of the user task which changed
   * @param targetGroups The groups whose members may be told, or {@code null} for everybody
   */
  public UserTaskChangedNotification(
      final Type type,
      final String userTaskId,
      final Collection<String> targetGroups) {

    this(type, userTaskId, targetGroups, null);

  }

  /**
   * Creates the notification of a change which says who made it.
   *
   * @param type The kind of change
   * @param userTaskId The id of the user task which changed
   * @param targetGroups The groups whose members may be told, or {@code null} for everybody
   * @param initiatorOfThisChange Who made this change, as the cockpit stores it, or {@code null}
   *        if that is not known
   */
  public UserTaskChangedNotification(
      final Type type,
      final String userTaskId,
      final Collection<String> targetGroups,
      final String initiatorOfThisChange) {

    super(
        "UserTask", type, targetGroups);

    this.userTaskId = userTaskId;
    this.initiatorOfThisChange = initiatorOfThisChange;

  }

  /**
   * Builds the notification from the change event alone, without the document behind it.
   * <p>
   * An {@code update} is the one operation MongoDB reports without the changed document, and the
   * cockpit subscribes without a lookup. So reading the document here would lose exactly those
   * changes: giving a task back, for example, which is an {@code $unset} and therefore an
   * {@code update}. Everything a client needs is in the event: which kind of entity changed, its
   * id, what happened to it and who made the change, see {@link LatestChange}. The target groups
   * are left out on purpose: who learns about a change is decided per update stream and not per
   * event, and nothing reads them.
   *
   * @param message The change event of MongoDB
   * @return The notification
   */
  public static UserTaskChangedNotification build(
      final Message<ChangeStreamDocument<Document>, UserTask> message) {

    return new UserTaskChangedNotification(
        Type.valueOf(operationOf(message).name()), message.getRaw().getDocumentKey().get(
            message.getRaw().getDocumentKey().getFirstKey()).asString().getValue(), null, LatestChange
                .initiatorCarriedBy(message.getRaw(), FIELD_OF_THE_STORED_INITIATOR)
                .orElse(null));

  }

  /**
   * Whether the change only says who read the task: an update of nothing but {@code readBy} and
   * {@code version}. Having read a task belongs to the reader alone and is no change of the task
   * for anybody else, so the cockpit sends no event for it, see decision 102 in the repository's
   * DECISIONS.md.
   * <p>
   * Only an update tells which fields it changed. The cockpit writes the read state with an update
   * of its own, so this is enough. Azure Cosmos DB for MongoDB does not describe an update, and
   * there such a change still produces an event.
   *
   * @param message The change event of MongoDB
   * @return {@code true} if nothing but the read state changed
   */
  public static boolean changesOnlyWhoReadTheTask(
      final Message<ChangeStreamDocument<Document>, UserTask> message) {

    final var raw = message.getRaw();
    if ((raw == null) || (raw.getUpdateDescription() == null)) {
      return false;
    }
    final var description = raw.getUpdateDescription();
    final var changedFields = new ArrayList<String>();
    if (description.getUpdatedFields() != null) {
      changedFields.addAll(description.getUpdatedFields().keySet());
    }
    if (description.getRemovedFields() != null) {
      changedFields.addAll(description.getRemovedFields());
    }
    final var readState = changedFields
        .stream()
        .filter(field -> !"version".equals(field))
        .toList();
    return !readState.isEmpty() && readState
        .stream()
        .allMatch(field -> field.equals(UserTaskService.FIELD_READ_BY) || field.startsWith(UserTaskService.FIELD_READ_BY
            + "."));

  }

  /**
   * Azure Cosmos DB for MongoDB does not report the operation type, see
   * <a href="https://learn.microsoft.com/en-us/azure/cosmos-db/mongodb/change-streams?tabs=javascript#current-limitations">its
   * limitations</a>. There it has to be read from the document. In that mode the event carries
   * the document, because the cockpit subscribes with a lookup. If the lookup no longer found the
   * document, we only know that something changed, not what. Calling it an update is the honest
   * answer, and a client reloads by id either way.
   */
  private static OperationType operationOf(
      final Message<ChangeStreamDocument<Document>, UserTask> message) {

    final var operationType = message.getRaw().getOperationTypeString();
    if (operationType != null) {
      return OperationType.byMongoType(operationType);
    }
    final var userTask = message.getBody();
    return (userTask != null) && Objects.equals(userTask.getCreatedAt(), userTask.getUpdatedAt())
        ? OperationType.INSERT
        : OperationType.UPDATE;

  }


  /**
   * The id of the user task which changed.
   *
   * @return The id
   */
  public String getUserTaskId() {
    return userTaskId;
  }

  /**
   * Sets the id of the user task which changed.
   *
   * @param userTaskId The id
   */
  public void setUserTaskId(
      String userTaskId) {
    this.userTaskId = userTaskId;
  }

  /**
   * Who made this one change, as the cockpit stores it.
   *
   * @return The id of the user or the name the cockpit stores for the system or for itself, or
   *         nothing if the change event did not say it
   */
  public Optional<String> getInitiatorOfThisChange() {
    return Optional.ofNullable(initiatorOfThisChange);
  }

}
