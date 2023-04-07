package io.vanillabp.cockpit.tasklist;

import org.bson.Document;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.messaging.Message;

import com.mongodb.client.model.changestream.ChangeStreamDocument;

import io.vanillabp.cockpit.commons.mongo.changestreams.OperationType;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.util.events.NotificationEvent;

public class UserTaskChangedNotification extends NotificationEvent {

    private static final long serialVersionUID = 1L;

    private String userTaskId;
    
    public UserTaskChangedNotification(
            final Type type,
            final String userTaskId) {
        
        super(
                "UserTask",
                type,
                null);
        
        this.userTaskId = userTaskId;
        
    }
    
    public static UserTaskChangedNotification build(
            final Message<ChangeStreamDocument<Document>, UserTask> message) {
    
        final var type = Type.valueOf(
                OperationType.byMongoType(
                        message.getRaw().getOperationTypeString()).name());

        return new UserTaskChangedNotification(
                type,
                message.getRaw().getDocumentKey().get(
                        message.getRaw().getDocumentKey().getFirstKey()).asString().getValue());
        
    }
    
    public static UserTaskChangedNotification build(
            final ChangeStreamEvent<UserTask> event) {
        
        final var type = Type.valueOf(
                OperationType.byMongoType(
                        event.getRaw().getOperationTypeString()).name());

        return new UserTaskChangedNotification(
                type,
                event.getRaw().getDocumentKey().get(
                        event.getRaw().getDocumentKey().getFirstKey()).asString().getValue());
        
    }
    
    public String getUserTaskId() {
        return userTaskId;
    }
    
    public void setUserTaskId(String userTaskId) {
        this.userTaskId = userTaskId;
    }
    
}
