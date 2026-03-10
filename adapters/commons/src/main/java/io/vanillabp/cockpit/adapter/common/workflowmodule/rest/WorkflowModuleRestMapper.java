package io.vanillabp.cockpit.adapter.common.workflowmodule.rest;

import io.vanillabp.cockpit.bpms.api.v1_1.GroupHierarchy;
import io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent;
import java.util.LinkedList;
import java.util.Optional;

public class WorkflowModuleRestMapper {

    @SuppressWarnings("unchecked")
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
        Optional
                .ofNullable(event.getGroupHierarchy())
                .map(h -> h
                        .entrySet()
                        .stream()
                        .map(entry -> new GroupHierarchy()
                                .group(entry.getKey())
                                .targets(new LinkedList<>(entry.getValue())))
                        .toList())
                .ifPresent(result::setGroupHierarchy);
        return result;

    }

}
