package io.vanillabp.cockpit.adapter.common.workflow;

import java.util.List;
import java.util.Optional;

import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksWorkflowProperties;
import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import io.vanillabp.cockpit.bpms.api.v1.UiUriType;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class WorkflowPublishing {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowPublishing.class);

    private String workerId;

    private final Optional<BpmsApi> bpmsApiV1;

    private final CockpitProperties properties;
    
    private final UserTasksWorkflowProperties workflowsCockpitProperties;

    public WorkflowPublishing(
            final String workerId,
            final Optional<BpmsApi> bpmsApiV1,
            final CockpitProperties properties,
            final UserTasksWorkflowProperties workflowsCockpitProperties) {
        this.workerId = workerId;
        this.bpmsApiV1 = bpmsApiV1;
        this.properties = properties;
        this.workflowsCockpitProperties = workflowsCockpitProperties;
    }

    @PostConstruct
    @javax.annotation.PostConstruct
    public void validateAutowiring() {
        if (bpmsApiV1.isPresent()) {
            return;
        }
        
        throw new RuntimeException("You have to configure either '"
                + CockpitProperties.PREFIX
                + ".client' or '"
                + CockpitProperties.PREFIX
                + ".stream' to define were to send user task events to!");
    }
    
    public void publish(
            final String apiVersion,
            final List<?> events) {
        
        if (apiVersion.equals("v1")) {
            
            try {
                processEventV1(events);
            } catch (Exception e) {
                logger.error("Could not publish events", e);
            }
            
        } else {
            throw new RuntimeException(
                    "Unsupported BPMS-API version '"
                    + apiVersion
                    + "'! The event is to old to be processed!");
        }
    }
    
    private void processEventV1(
            final List<?> events) {
        events.forEach(eventObject -> {
            if (eventObject instanceof WorkflowCreatedOrUpdatedEvent) {
                final var event = (WorkflowCreatedOrUpdatedEvent) eventObject;
                event.setSource(workerId);

                final var commonWorkflowsProperties = workflowsCockpitProperties
                        .getWorkflows()
                        .stream()
                        .filter(props -> props.matches(event.getWorkflowModule()))
                        .findFirst()
                        .get();
                final var workflowsProperties = workflowsCockpitProperties
                        .getWorkflows()
                        .stream()
                        .filter(props -> !props.matches(event.getWorkflowModule()))
                        .filter(props -> props.matches(event.getWorkflowModule(), event.getBpmnProcessId()))
                        .findFirst()
                        .get();

                event.setWorkflowProviderApiUriPath(
                        StringUtils.hasText(workflowsProperties.getWorkflowProviderApiPath())
                        ? workflowsProperties.getWorkflowProviderApiPath()
                        : commonWorkflowsProperties.getWorkflowProviderApiPath());
                var uiUriPath = workflowsProperties.getUiUriPath();
                if (!StringUtils.hasText(uiUriPath)) {
                    uiUriPath = commonWorkflowsProperties.getUiUriPath();
                }
                if (!StringUtils.hasText(uiUriPath)) {
                    uiUriPath = event.getUiUriPath();
                } else if (StringUtils.hasText(event.getUiUriPath())) {
                    uiUriPath += event.getUiUriPath();
                }
                event.setUiUriPath(uiUriPath.replace("//", "/"));
                var uiUriType = workflowsProperties.getUiUriType();
                if (!StringUtils.hasText(uiUriType)) {
                    uiUriType = commonWorkflowsProperties.getUiUriType();
                }
                if (!StringUtils.hasText(uiUriType)) {
                    uiUriType = properties.getUiUriType();
                }
                event.setUiUriType(UiUriType.fromValue(uiUriType));
                event.setWorkflowModuleUri(
                        StringUtils.hasText(workflowsProperties.getWorkflowModuleUri())
                                ? workflowsProperties.getWorkflowModuleUri()
                                : commonWorkflowsProperties.getWorkflowModuleUri());
                if (Boolean.FALSE.equals(event.getUpdated())) {
                    bpmsApiV1.get().workflowCreatedEvent(event);
                } else {
                    bpmsApiV1.get().workflowUpdatedEvent(event.getWorkflowId(), event);
                }
            } else if (eventObject instanceof WorkflowCompletedEvent) {
                final var event = (WorkflowCompletedEvent) eventObject;
                bpmsApiV1.get().workflowCompletedEvent(event.getWorkflowId(), event);
            } else if (eventObject instanceof WorkflowCancelledEvent) {
                final var event = (WorkflowCancelledEvent) eventObject;
                bpmsApiV1.get().workflowCancelledEvent(event.getWorkflowId(), event);

            // else if suspended
            // else if activated

            } else {
                throw new RuntimeException(
                        "Unsupported event type '"
                        + eventObject.getClass().getName()
                        + "'!");
            }
        });
    }
    
}
