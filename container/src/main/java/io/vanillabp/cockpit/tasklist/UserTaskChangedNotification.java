package io.vanillabp.cockpit.tasklist;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.vanillabp.cockpit.commons.mongo.changestreams.OperationType;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.events.NotificationEvent;
import org.bson.Document;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.messaging.Message;

import java.util.Collection;

public class UserTaskChangedNotification extends NotificationEvent {

    private static final long serialVersionUID = 1L;

    private String userTaskId;
    
    public UserTaskChangedNotification(
            final Type type,
            final String userTaskId,
            final Collection<String> targetRoles) {
        
        super(
                "UserTask",
                type,
                targetRoles);
        
        this.userTaskId = userTaskId;
        
    }
    
    public static UserTaskChangedNotification build(
            final Message<ChangeStreamDocument<Document>, UserTask> message) {
    
        final OperationType type;
        // Since CosmosDB for MongoDB does not support OperationType yet it is
        // necessary to derived the OperationType from the document's content.
        // see https://learn.microsoft.com/en-us/azure/cosmos-db/mongodb/change-streams?tabs=javascript#current-limitations
        if (message.getRaw().getOperationTypeString() == null) {
            if (message.getBody().getCreatedAt().equals(message.getBody().getUpdatedAt())) {
                type = OperationType.INSERT;
            } else {
                type = OperationType.UPDATE;
            }
        }
        // Original MongoDB:
        else {
            type = OperationType.byMongoType(
                    message.getRaw().getOperationTypeString());
        }

        return new UserTaskChangedNotification(
                Type.valueOf(type.name()),
                message.getRaw().getDocumentKey().get(
                        message.getRaw().getDocumentKey().getFirstKey()).asString().getValue(),
                message.getBody().getTargetRoles());
        
    }
    
    public static UserTaskChangedNotification build(
            final ChangeStreamEvent<UserTask> event) {
        
        final OperationType type;
        // Since CosmosDB for MongoDB does not support OperationType yet it is
        // necessary to derive the OperationType from the document's content.
        // see https://learn.microsoft.com/en-us/azure/cosmos-db/mongodb/change-streams?tabs=javascript#current-limitations
        if (event.getRaw().getOperationTypeString() == null) {
            if (event.getBody().getCreatedAt().equals(event.getBody().getUpdatedAt())) {
                type = OperationType.INSERT;
            } else {
                type = OperationType.UPDATE;
            }
        }
        // Original MongoDB:
        else {
            type = OperationType.byMongoType(
                    event.getRaw().getOperationTypeString());
        }

        return new UserTaskChangedNotification(
                Type.valueOf(type.name()),
                event.getRaw().getDocumentKey().get(
                        event.getRaw().getDocumentKey().getFirstKey()).asString().getValue(),
                event.getBody().getTargetRoles());
        
    }
    
    public String getUserTaskId() {
        return userTaskId;
    }
    
    public void setUserTaskId(String userTaskId) {
        this.userTaskId = userTaskId;
    }
    
}
