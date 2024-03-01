package io.vanillabp.cockpit.adapter.common.workflow;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUiUriType;
import io.vanillabp.springboot.adapter.VanillaBpProperties;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class WorkflowPublishingBase {

    protected final String workerId;

    protected final VanillaBpCockpitProperties properties;

    protected WorkflowPublishingBase(
            final String workerId,
            final VanillaBpCockpitProperties properties) {

        this.workerId = workerId;
        this.properties = properties;

    }

    protected void editWorkflowCreatedOrUpdatedEvent(
            final WorkflowCreatedEvent event) {

        event.setSource(workerId);
        event.setWorkflowProviderApiUriPath("/workflow-provider"); // TODO
        event.setWorkflowModuleUri(properties.getWorkflowModuleUri(event.getWorkflowModuleId()));
        event.setUiUriPath(properties.getUiUriPath(event.getWorkflowModuleId()));
        try {
            event.setUiUriType(
                    WorkflowUiUriType.fromValue(
                            properties.getUiUriType(event.getWorkflowModuleId())));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unsupported UI-URI-type configured at one of these properties:\n  "
                            + VanillaBpProperties.PREFIX
                            + ".workflow-modules."
                            + event.getWorkflowModuleId()
                            + ".cockpit.ui-uri-path"
                            + "Possible values are: '"
                            + Arrays.stream(WorkflowUiUriType.values()).map(WorkflowUiUriType::getValue).collect(Collectors.joining("', '"))
                            + "'", e);
        }

    }

}
