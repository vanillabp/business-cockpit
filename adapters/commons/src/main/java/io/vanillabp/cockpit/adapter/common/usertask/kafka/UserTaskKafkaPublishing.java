package io.vanillabp.cockpit.adapter.common.usertask.kafka;

import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksWorkflowProperties;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUiUriType;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;

import java.util.List;

public class UserTaskKafkaPublishing implements UserTaskPublishing {

    private static final Logger logger = LoggerFactory.getLogger(UserTaskKafkaPublishing.class);

    private final String workerId;

    private final UserTaskProtobufMapper userTaskMapper;

    private final CockpitProperties properties;

    private final UserTasksWorkflowProperties workflowsCockpitProperties;

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    private static final UserTaskProperties EMPTY_PROPERTIES = new UserTaskProperties();


    public UserTaskKafkaPublishing(
            String workerId,
            CockpitProperties properties,
            UserTasksWorkflowProperties workflowsCockpitProperties,
            UserTaskProtobufMapper userTaskMapper,
            KafkaTemplate<String, byte[]> kafkaTemplate
    ) {
        this.workerId = workerId;
        this.userTaskMapper = userTaskMapper;
        this.properties = properties;
        this.workflowsCockpitProperties = workflowsCockpitProperties;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(UserTaskEvent eventObject) {

        if (eventObject instanceof UserTaskUpdatedEvent userTaskUpdatedEvent){

            editUserTaskCreatedOrUpdatedEvent(userTaskUpdatedEvent);
            var event = this.userTaskMapper.map(userTaskUpdatedEvent);
            sendUserTaskEvent(event.getClass(), event.toByteArray());

        } else if (eventObject instanceof UserTaskCreatedEvent userTaskCreatedEvent){

            editUserTaskCreatedOrUpdatedEvent(userTaskCreatedEvent);
            var event = this.userTaskMapper.map(userTaskCreatedEvent);
            sendUserTaskEvent(event.getClass(), event.toByteArray());

        } else if (eventObject instanceof UserTaskCompletedEvent userTaskCompletedEvent) {

            var event = userTaskMapper.map(userTaskCompletedEvent);
            sendUserTaskEvent(event.getClass(), event.toByteArray());

        } else if (eventObject instanceof UserTaskCancelledEvent userTaskCancelledEvent) {

            var event = userTaskMapper.map(userTaskCancelledEvent);
            sendUserTaskEvent(event.getClass(), event.toByteArray());

        } else if (eventObject instanceof UserTaskActivatedEvent userTaskActivatedEvent) {

            var event = userTaskMapper.map(userTaskActivatedEvent);
            sendUserTaskEvent(event.getClass(), event.toByteArray());

        } else if (eventObject instanceof UserTaskSuspendedEvent userTaskSuspendedEvent) {

            var event = userTaskMapper.map(userTaskSuspendedEvent);
            sendUserTaskEvent(event.getClass(), event.toByteArray());

        } else {

            throw new RuntimeException(
                    "Unsupported event type '"
                            + eventObject.getClass().getName()
                            + "'!");

        }
    }

    private void editUserTaskCreatedOrUpdatedEvent(UserTaskCreatedEvent eventObject) {
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
        event.setUiUriType(UserTaskUiUriType.fromValue(uiUriType));
        event.setWorkflowModuleUri(
                StringUtils.hasText(workflowsProperties.getWorkflowModuleUri())
                        ? workflowsProperties.getWorkflowModuleUri()
                        : commonWorkflowsProperties.getWorkflowModuleUri());
    }


    private void sendUserTaskEvent(Class<?> eventClass, byte[] eventStream) {
        kafkaTemplate.send(
                properties.getKafkaTopics().getUserTask(),
                eventClass.getName(),
                eventStream);
    }

}