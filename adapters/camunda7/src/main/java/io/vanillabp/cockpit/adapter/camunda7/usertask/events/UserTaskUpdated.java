package io.vanillabp.cockpit.adapter.camunda7.usertask.events;

import java.util.List;

import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;

public class UserTaskUpdated extends UserTaskCreated {

    public UserTaskUpdated(
            final UserTaskCreatedOrUpdatedEvent event,
            final String workflowModuleId,
            final List<String> i18nLanguages) {
        
        super(event, workflowModuleId, i18nLanguages);
        event.setUpdated(Boolean.TRUE);
        
    }

}
