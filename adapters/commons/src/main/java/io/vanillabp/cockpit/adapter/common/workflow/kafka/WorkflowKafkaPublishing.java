package io.vanillabp.cockpit.adapter.common.workflow.kafka;

import io.vanillabp.cockpit.adapter.common.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.UserTasksWorkflowProperties;
import io.vanillabp.cockpit.adapter.common.workflow.WorkflowPublishing;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUiUriType;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;

import java.util.List;

public class WorkflowKafkaPublishing implements WorkflowPublishing {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowKafkaPublishing.class);

    private final String workerId;

    private final CockpitProperties properties;

    private final UserTasksWorkflowProperties workflowsCockpitProperties;

    private final WorkflowProtobufMapper workflowMapper;

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public WorkflowKafkaPublishing(
            String workerId,
            CockpitProperties properties,
            UserTasksWorkflowProperties workflowsCockpitProperties,
            WorkflowProtobufMapper workflowMapper,
            KafkaTemplate<String, byte[]> kafkaTemplate
    ) {
        this.workerId = workerId;
        this.properties = properties;
        this.workflowsCockpitProperties = workflowsCockpitProperties;
        this.workflowMapper = workflowMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(WorkflowEvent eventObject) {
        if(eventObject instanceof WorkflowUpdatedEvent workflowUpdatedEvent){

            editWorkflowCreatedOrUpdatedEvent(workflowUpdatedEvent);
            var event = workflowMapper.map(workflowUpdatedEvent);
            this.sendWorkflowEvent(event.getClass(), event.toByteArray());

        } else if(eventObject instanceof WorkflowCreatedEvent workflowCreatedEvent){

            editWorkflowCreatedOrUpdatedEvent(workflowCreatedEvent);
            var event = workflowMapper.map(workflowCreatedEvent);
            this.sendWorkflowEvent(event.getClass(), event.toByteArray());

        } else if(eventObject instanceof WorkflowCancelledEvent workflowCancelledEvent){

            var event = workflowMapper.map(workflowCancelledEvent);
            this.sendWorkflowEvent(event.getClass(), event.toByteArray());

        } else if(eventObject instanceof WorkflowCompletedEvent workflowCompletedEvent) {

            var event = workflowMapper.map(workflowCompletedEvent);
            this.sendWorkflowEvent(event.getClass(), event.toByteArray());

        }
        // else if suspended
        // else if activated
        else {
            throw new RuntimeException(
                    "Unsupported event type '"
                            + eventObject.getClass().getName()
                            + "'!");
        }
    }

    private void editWorkflowCreatedOrUpdatedEvent(WorkflowCreatedEvent eventObject) {
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
        event.setUiUriType(WorkflowUiUriType.fromValue(uiUriType));
        event.setWorkflowModuleUri(
                StringUtils.hasText(workflowsProperties.getWorkflowModuleUri())
                        ? workflowsProperties.getWorkflowModuleUri()
                        : commonWorkflowsProperties.getWorkflowModuleUri());
    }


    private void sendWorkflowEvent(Class<?> eventClass, byte[] eventStream) {
        kafkaTemplate.send(
                properties.getKafkaTopics().getWorkflow(),
                eventClass.getName(),
                eventStream);
    }
}
