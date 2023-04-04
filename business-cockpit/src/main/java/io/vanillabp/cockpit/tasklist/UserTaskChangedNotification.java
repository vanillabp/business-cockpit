package io.vanillabp.cockpit.tasklist;

import org.bson.Document;
import org.springframework.data.mongodb.core.messaging.Message;

import com.mongodb.client.model.changestream.ChangeStreamDocument;

import io.vanillabp.cockpit.commons.mongo.ChangeStreamUtils.OperationType;
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

    public UserTaskChangedNotification(
            final Message<ChangeStreamDocument<Document>, UserTask> message) {

        this(
                Type
                        .valueOf(OperationType
                                .byMongoType(message.getRaw().getOperationTypeString())
                        .name()),
                message
                        .getBody()
                        .getId());
        
    }
    
    public String getUserTaskId() {
        return userTaskId;
    }
    
    public void setUserTaskId(String userTaskId) {
        this.userTaskId = userTaskId;
    }
    
}
