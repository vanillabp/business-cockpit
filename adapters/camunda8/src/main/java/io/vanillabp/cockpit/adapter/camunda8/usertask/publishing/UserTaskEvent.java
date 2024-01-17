package io.vanillabp.cockpit.adapter.camunda8.usertask.publishing;

import org.springframework.context.ApplicationEvent;

public class UserTaskEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    final io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent event;

    public UserTaskEvent(
            final Object source,
            final io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent event) {
        
        super(source);
        this.event = event;
        
    }
    
    public io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent getEvent() {
        return event;
    }
    
}
