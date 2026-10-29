package io.vanillabp.cockpit.tasklist;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.vanillabp.cockpit.commons.mongo.changestreams.OperationType;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.events.NotificationEvent;
import java.util.Collection;
import java.util.Objects;
import org.bson.Document;
import org.springframework.data.mongodb.core.messaging.Message;

/**
 * Tells the open browsers that a user task changed. The GUI notification service turns it into an
 * event of the update stream.
 */
public class UserTaskChangedNotification extends NotificationEvent {

    private static final long serialVersionUID = 1L;

    /** The id of the user task which changed. */
    private String userTaskId;
    
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
        
        super(
                "UserTask",
                type,
                targetGroups);
        
        this.userTaskId = userTaskId;
        
    }
    
    /**
     * Builds the notification from the change event alone, without the document behind it.
     * <p>
     * An {@code update} is the one operation MongoDB reports without the changed document, and the
     * cockpit subscribes without a lookup. So reading the document here would lose exactly those
     * changes: giving a task back, for example, which is an {@code $unset} and therefore an
     * {@code update}. Everything a client needs is in the event: which kind of entity changed, its
     * id and what happened to it. The target groups are left out on purpose: who learns about a
     * change is decided per update stream and not per event, and nothing reads them.
     *
     * @param message The change event of MongoDB
     * @return The notification
     */
    public static UserTaskChangedNotification build(
            final Message<ChangeStreamDocument<Document>, UserTask> message) {

        return new UserTaskChangedNotification(
                Type.valueOf(operationOf(message).name()),
                message.getRaw().getDocumentKey().get(
                        message.getRaw().getDocumentKey().getFirstKey()).asString().getValue(),
                null);

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
        return (userTask != null)
                && Objects.equals(userTask.getCreatedAt(), userTask.getUpdatedAt())
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
    public void setUserTaskId(String userTaskId) {
        this.userTaskId = userTaskId;
    }
    
}
