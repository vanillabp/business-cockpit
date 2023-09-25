package io.vanillabp.cockpit.adapter.camunda7.usertask.events;

import java.time.OffsetDateTime;

import io.vanillabp.cockpit.adapter.common.usertask.EventWrapper;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent;

public class UserTaskDeleted implements EventWrapper {

    private final UserTaskCancelledEvent event;
    
    public UserTaskDeleted(
            final UserTaskCancelledEvent event) {
        
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