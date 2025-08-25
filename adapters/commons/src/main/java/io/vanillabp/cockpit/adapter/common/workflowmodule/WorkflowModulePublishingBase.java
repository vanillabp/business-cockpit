package io.vanillabp.cockpit.adapter.common.workflowmodule;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflowmodule.events.RegisterWorkflowModuleEvent;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

public abstract class WorkflowModulePublishingBase {

    private final Logger log = LoggerFactory.getLogger(WorkflowModulePublishingBase.class);

    protected final ObjectProvider<List<WorkflowModuleDetailsProvider>> workflowModuleDetailsProviders;

    protected final String workerId;

    protected final VanillaBpCockpitProperties properties;

    protected WorkflowModulePublishingBase(
            final String workerId,
            final VanillaBpCockpitProperties properties,
            final ObjectProvider<List<WorkflowModuleDetailsProvider>> workflowModuleDetailsProviders
            ) {

        this.workerId = workerId;
        this.properties = properties;
        this.workflowModuleDetailsProviders = workflowModuleDetailsProviders;

    }

    protected void enrichRegisterWorkflowModuleEvent(
            final RegisterWorkflowModuleEvent event) {

        event.setEventId(UUID.randomUUID().toString());
        event.setSource(workerId);
        event.setTimestamp(OffsetDateTime.now());
        event.setTaskProviderApiUriPath("/task-provider"); // TODO
        event.setWorkflowProviderApiUriPath("/workflow-provider"); // TODO
        event.setUri(properties.getWorkflowModuleUri(event.getId()));

        workflowModuleDetailsProviders.ifAvailable(providers ->
                providers
                .stream()
                .filter(detailsProvider -> detailsProvider.getWorkflowModuleId().equals(event.getId()))
                .findFirst()
                .ifPresentOrElse(
                        details -> event.setAccessibleToGroups(details.getAccessibleToGroups()),
                        () -> log.error("Could not find bean implementing interface WorkflowModuleDetailsProvider for id '{}'", event.getId())
                )
        );
    }

}
