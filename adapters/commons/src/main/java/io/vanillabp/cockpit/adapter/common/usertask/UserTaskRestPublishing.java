package io.vanillabp.cockpit.adapter.common.usertask;

import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.events.*;
import io.vanillabp.cockpit.bpms.api.v1.BpmsApi;
import io.vanillabp.cockpit.bpms.api.v1.UiUriType;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

public class UserTaskRestPublishing implements UserTaskPublishing {

    private static final Logger logger = LoggerFactory.getLogger(UserTaskRestPublishing.class);

    private static final UserTaskProperties EMPTY_PROPERTIES = new UserTaskProperties();

    private String workerId;

    private final Optional<BpmsApi> bpmsApiV1;

    private final CockpitProperties properties;

    private final UserTasksWorkflowProperties workflowsCockpitProperties;

    private final UserTaskRestMapper userTaskRestMapper;

    public UserTaskRestPublishing(
            final String workerId,
            final Optional<BpmsApi> bpmsApiV1,
            final CockpitProperties properties,
            final UserTasksWorkflowProperties workflowsCockpitProperties,
            final UserTaskRestMapper userTaskRestMapper) {

        this.workerId = workerId;
        this.bpmsApiV1 = bpmsApiV1;
        this.properties = properties;
        this.workflowsCockpitProperties = workflowsCockpitProperties;
        this.userTaskRestMapper = userTaskRestMapper;
        
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

    @Override
    public void publish(
            final String apiVersion,
            final List<UserTaskEvent> events) {
        
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
    
    public void processEventV1(
            final List<?> events) {

        events.forEach(eventObject -> {

            if (eventObject instanceof UserTaskUpdatedEvent userTaskUpdatedEvent){

                final var event = this.userTaskRestMapper.map(userTaskUpdatedEvent);
                sendUserTaskCreatedOrUpdatedEvent(event);

            } else if (eventObject instanceof UserTaskCreatedEvent userTaskCreatedEvent){

                final var event = this.userTaskRestMapper.map(userTaskCreatedEvent);
                sendUserTaskCreatedOrUpdatedEvent(event);

            } else if (eventObject instanceof UserTaskCompletedEvent userTaskCompletedEvent) {
                
                final var event = userTaskRestMapper.map(userTaskCompletedEvent);
                bpmsApiV1.get().userTaskCompletedEvent(event.getUserTaskId(), event);
                
            } else if (eventObject instanceof UserTaskCancelledEvent userTaskCancelledEvent) {
                
                final var event = userTaskRestMapper.map(userTaskCancelledEvent);
                bpmsApiV1.get().userTaskCancelledEvent(event.getUserTaskId(), event);
                
            } else if (eventObject instanceof UserTaskActivatedEvent userTaskActivatedEvent) {
                
                final var event = userTaskRestMapper.map(userTaskActivatedEvent);
                bpmsApiV1.get().userTaskActivatedEvent(event.getUserTaskId(), event);
                
            } else if (eventObject instanceof UserTaskSuspendedEvent userTaskSuspendedEvent) {

                final var event = userTaskRestMapper.map(userTaskSuspendedEvent);
                bpmsApiV1.get().userTaskSuspendedEvent(event.getUserTaskId(), event);
                
            } else {
                
                throw new RuntimeException(
                        "Unsupported event type '"
                        + eventObject.getClass().getName()
                        + "'!");
                
            }
            
        });
        
    }

    private void sendUserTaskCreatedOrUpdatedEvent(UserTaskCreatedOrUpdatedEvent eventObject) {
        final var event = eventObject;
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
        final var userTaskProperties = workflowsProperties
                .getUserTasks()
                .getOrDefault(event.getTaskDefinition(), EMPTY_PROPERTIES);

        event.setTaskProviderApiUriPath(
                StringUtils.hasText(workflowsProperties.getTaskProviderApiPath())
                        ? workflowsProperties.getTaskProviderApiPath()
                        : commonWorkflowsProperties.getTaskProviderApiPath());
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
        var uiUriType = userTaskProperties.getUiUriType();
        if (!StringUtils.hasText(uiUriType)) {
            uiUriType = workflowsProperties.getUiUriType();
        }
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
            bpmsApiV1.get().userTaskCreatedEvent(event);
        } else {
            bpmsApiV1.get().userTaskUpdatedEvent(event.getUserTaskId(), event);
        }
    }

}
