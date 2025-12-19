package io.vanillabp.cockpit.adapter.camunda8.usertask.publishing;

import org.springframework.context.ApplicationEvent;

public class ProcessUserTaskAfterTransactionEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public ProcessUserTaskAfterTransactionEvent(Object source) {
        super(source);
    }

}
