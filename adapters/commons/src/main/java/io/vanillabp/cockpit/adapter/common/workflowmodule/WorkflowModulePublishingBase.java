package io.vanillabp.cockpit.adapter.common.workflowmodule;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflowmodule.events.RegisterWorkflowModuleEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public abstract class WorkflowModulePublishingBase {

    protected final String workerId;

    protected final VanillaBpCockpitProperties properties;

    protected WorkflowModulePublishingBase(
            final String workerId,
            final VanillaBpCockpitProperties properties) {

        this.workerId = workerId;
        this.properties = properties;

    }

    protected void editRegisterWorkflowModuleEvent(
            final RegisterWorkflowModuleEvent event) {

        event.setEventId(UUID.randomUUID().toString());
        event.setSource(workerId);
        event.setTimestamp(OffsetDateTime.now());
        event.setTaskProviderApiUriPath("/task-provider"); // TODO
        event.setWorkflowProviderApiUriPath("/workflow-provider"); // TODO
        event.setUri(properties.getWorkflowModuleUri(event.getId()));

    }

}
