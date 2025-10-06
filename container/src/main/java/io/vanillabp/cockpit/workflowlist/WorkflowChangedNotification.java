package io.vanillabp.cockpit.workflowlist;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.vanillabp.cockpit.commons.mongo.changestreams.OperationType;
import io.vanillabp.cockpit.util.events.NotificationEvent;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.util.Collection;
import org.bson.Document;
import org.springframework.data.mongodb.core.ChangeStreamEvent;
import org.springframework.data.mongodb.core.messaging.Message;

public class WorkflowChangedNotification extends NotificationEvent {

    private static final long serialVersionUID = 1L;

    private String workflowId;

    public WorkflowChangedNotification(
            final Type type,
            final String workflowId,
            final Collection<String> targetGroups,
	    final Collection<String> targetUsers) {
        
        super(
                "Workflow",
                type,
                targetGroups,
		targetUsers);
        
        this.workflowId = workflowId;
        
    }
    
    public static WorkflowChangedNotification build(
            final Message<ChangeStreamDocument<Document>, Workflow> message) {
    
        final OperationType type;
        // Since CosmosDB for MongoDB does not support OperationType yet it is
        // necessary to derive the OperationType from the document's content.
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

        return new WorkflowChangedNotification(
                Type.valueOf(type.name()),
                message.getRaw().getDocumentKey().get(
                        message.getRaw().getDocumentKey().getFirstKey()).asString().getValue(),
                null,
		null);
        
    }
    
    public static WorkflowChangedNotification build(
            final ChangeStreamEvent<Workflow> event) {
        
        final OperationType type;
        // Since CosmosDB for MongoDB does not support OperationType yet it is
        // necessary to derived the OperationType from the document's content.
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

        return new WorkflowChangedNotification(
                Type.valueOf(type.name()),
                event.getRaw().getDocumentKey().get(
                        event.getRaw().getDocumentKey().getFirstKey()).asString().getValue(),
                null,
		null);
        
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }
    
}
