package io.vanillabp.cockpit.adapter.camunda8.usertask.publishing;

import org.springframework.context.ApplicationEvent;

public class ProcessUserTaskEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public ProcessUserTaskEvent(Object source) {
        super(source);
    }

}
