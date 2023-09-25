package io.vanillabp.cockpit.adapter.camunda7.usertask.publishing;

import org.springframework.context.ApplicationEvent;

public class UserTaskEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    final Object event;
    
    final String apiVersion;
    
    public UserTaskEvent(
            final Object source,
            final Object event,
            final String apiVersion) {
        
        super(source);
        this.event = event;
        this.apiVersion = apiVersion;
        
    }
    
    public Object getEvent() {
        return event;
    }
    
    public String getApiVersion() {
        return apiVersion;
    }
    
}
