package io.vanillabp.cockpit.adapter.camunda7.workflow.events;

import java.time.OffsetDateTime;

import io.vanillabp.cockpit.adapter.common.workflow.EventWrapper;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;

public class WorkflowCompleted implements EventWrapper {

    private final WorkflowCompletedEvent event;
    
    private final String apiVersion;
    
    public WorkflowCompleted(
            final WorkflowCompletedEvent event,
            final String apiVersion) {
        
        this.event = event;
        this.apiVersion = apiVersion;
        
    }
    
    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public Object getEvent() {
        
        return event;
        
    }

    @Override
    public void setId(String id) {
        event.setId(id);
    }

    @Override
    public void setTimestamp(OffsetDateTime timestamp) {
        event.setTimestamp(timestamp);
    }

    @Override
    public void setComment(String comment) {
        event.setComment(comment);
    }
    
    @Override
    public void setWorkflowId(String workflowId) {
        event.setWorkflowId(workflowId);
    }

    public void setInitiator(String initiator) {
        event.initiator(initiator);
    }

    public void setBpmnProcessId(String bpmnProcessId) {
        event.setBpmnProcessId(bpmnProcessId);
    }

    public void setBpmnProcessVersion(String bpmnProcessVersion) {
        event.setBpmnProcessVersion(bpmnProcessVersion);
    }
    
}
