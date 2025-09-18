package io.vanillabp.cockpit.adapter.common.workflowmodule.rest;

import io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent;

public class WorkflowModuleRestMapper {

    public RegisterWorkflowModuleEvent map(
            final io.vanillabp.cockpit.adapter.common.workflowmodule.events.RegisterWorkflowModuleEvent event) {

        if (event == null) {
            return null;
        }
        final var result = new RegisterWorkflowModuleEvent();
        result.setUri(event.getUri());
        result.setTaskProviderApiUriPath(event.getTaskProviderApiUriPath());
        result.setWorkflowProviderApiUriPath(event.getWorkflowProviderApiUriPath());
        result.setAccessibleToGroups(event.getAccessibleToGroups());
        return result;

    }

}
