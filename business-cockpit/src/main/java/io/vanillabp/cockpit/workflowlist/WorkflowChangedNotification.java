package io.vanillabp.cockpit.workflowlist;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.bson.Document;
import org.springframework.data.mongodb.core.messaging.Message;

import com.mongodb.client.model.changestream.ChangeStreamDocument;

import io.vanillabp.cockpit.mongo.changestreams.LatestChange;
import io.vanillabp.cockpit.mongo.changestreams.OperationType;
import io.vanillabp.cockpit.util.events.NotificationEvent;
import io.vanillabp.cockpit.workflowlist.model.Workflow;

/**
 * Tells the open browsers that a workflow changed. The GUI notification service turns it into an
 * event of the update stream.
 */
public class WorkflowChangedNotification extends NotificationEvent {

  private static final long serialVersionUID = 1L;

  /** The field a workflow keeps who made its latest change in. */
  private static final String FIELD_OF_THE_STORED_INITIATOR = "initiatorOfLatestChange";

  /** The id of the workflow which changed. */
  private String workflowId;

  /**
   * Who made this one change, as the cockpit stores it, or {@code null} if the change event does
   * not say it. See {@link LatestChange#initiatorCarriedBy}.
   */
  private String initiatorOfThisChange;

  /**
   * Creates the notification.
   *
   * @param type The kind of change
   * @param workflowId The id of the workflow which changed
   * @param targetGroups The groups whose members may be told, or {@code null} for everybody
   */
  public WorkflowChangedNotification(
      final Type type,
      final String workflowId,
      final Collection<String> targetGroups) {

    this(type, workflowId, targetGroups, null);

  }

  /**
   * Creates the notification of a change which says who made it.
   *
   * @param type The kind of change
   * @param workflowId The id of the workflow which changed
   * @param targetGroups The groups whose members may be told, or {@code null} for everybody
   * @param initiatorOfThisChange Who made this change, as the cockpit stores it, or {@code null}
   *        if that is not known
   */
  public WorkflowChangedNotification(
      final Type type,
      final String workflowId,
      final Collection<String> targetGroups,
      final String initiatorOfThisChange) {

    super(
        "Workflow", type, targetGroups);

    this.workflowId = workflowId;
    this.initiatorOfThisChange = initiatorOfThisChange;

  }

  /**
   * Builds the notification from the change event alone, without the document behind it. The
   * reason is the same as for a user task, see
   * {@code UserTaskChangedNotification#build(Message)}.
   *
   * @param message The change event of MongoDB
   * @return The notification
   */
  public static WorkflowChangedNotification build(
      final Message<ChangeStreamDocument<Document>, Workflow> message) {

    return new WorkflowChangedNotification(
        Type.valueOf(operationOf(message).name()), message.getRaw().getDocumentKey().get(
            message.getRaw().getDocumentKey().getFirstKey()).asString().getValue(), null, LatestChange
                .initiatorCarriedBy(message.getRaw(), FIELD_OF_THE_STORED_INITIATOR)
                .orElse(null));

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
      final Message<ChangeStreamDocument<Document>, Workflow> message) {

    final var operationType = message.getRaw().getOperationTypeString();
    if (operationType != null) {
      return OperationType.byMongoType(operationType);
    }
    final var workflow = message.getBody();
    return (workflow != null) && Objects.equals(workflow.getCreatedAt(), workflow.getUpdatedAt())
        ? OperationType.INSERT
        : OperationType.UPDATE;

  }


  /**
   * The id of the workflow which changed.
   *
   * @return The id
   */
  public String getWorkflowId() {
    return workflowId;
  }

  /**
   * Sets the id of the workflow which changed.
   *
   * @param workflowId The id
   */
  public void setWorkflowId(
      String workflowId) {
    this.workflowId = workflowId;
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
