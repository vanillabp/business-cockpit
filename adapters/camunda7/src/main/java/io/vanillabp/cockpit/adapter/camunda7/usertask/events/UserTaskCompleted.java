package io.vanillabp.cockpit.adapter.camunda7.usertask.events;

import java.time.OffsetDateTime;

import io.vanillabp.cockpit.adapter.common.usertask.EventWrapper;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent;

public class UserTaskCompleted implements EventWrapper {

    private final UserTaskCompletedEvent event;
    
    public UserTaskCompleted(
            final UserTaskCompletedEvent event) {
        
        this.event = event;
        
    }

    @Override
    public Object getEvent() {
        
        return event;
        
    }

    public void setId(String id) {
        event.setId(id);
    }

    public void setUserTaskId(String userTaskId) {
        event.setUserTaskId(userTaskId);
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        event.setTimestamp(timestamp);
    }

    public void setComment(String comment) {
        event.setComment(comment);
    }
    
}
