package io.vanillabp.cockpit.adapter.common.usertask;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUiUriType;
import io.vanillabp.springboot.adapter.VanillaBpProperties;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class UserTaskPublishingBase {

    protected final String workerId;

    protected final VanillaBpCockpitProperties properties;

    protected UserTaskPublishingBase(
            final String workerId,
            final VanillaBpCockpitProperties properties) {

        this.workerId = workerId;
        this.properties = properties;

    }

    protected void editUserTaskCreatedOrUpdatedEvent(
            final UserTaskCreatedEvent event) {

        event.setSource(workerId);
        event.setUiUriPath(properties.getUiUriPath(event.getWorkflowModuleId()));
        try {
            event.setUiUriType(
                    UserTaskUiUriType.fromValue(
                            properties.getUiUriType(event.getWorkflowModuleId())));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unsupported UI-URI-type configured at one of these properties:\n  "
                            + VanillaBpProperties.PREFIX
                            + ".workflow-modules."
                            + event.getWorkflowModuleId()
                            + ".cockpit.ui-uri-path"
                            + "Possible values are: '"
                            + Arrays.stream(UserTaskUiUriType.values()).map(UserTaskUiUriType::getValue).collect(Collectors.joining("', '"))
                            + "'", e);
        }

    }

}
