package io.vanillabp.cockpit.adapter.common.workflow.kafka;

import io.vanillabp.cockpit.adapter.common.properties.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.commons.kafka.KafkaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowKafkaPublishingTest {

    @Mock
    private VanillaBpCockpitProperties properties;

    @Mock
    private CockpitProperties cockpitProperties;

    @Mock
    private KafkaProperties kafkaProperties;

    @Mock
    private KafkaProperties.Topics topicsProperties;

    @Mock
    private WorkflowProtobufMapper workflowMapper;

    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    private WorkflowKafkaPublishing publishing;

    private static final String WORKER_ID = "test-worker";
    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final String WORKFLOW_TOPIC = "workflow-topic";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @BeforeEach
    void setUp() {
        publishing = new WorkflowKafkaPublishing(WORKER_ID, properties, workflowMapper, kafkaTemplate);
    }

    private void setupKafkaTopicMocks() {
        when(properties.getCockpit()).thenReturn(cockpitProperties);
        when(cockpitProperties.getKafka()).thenReturn(kafkaProperties);
        when(kafkaProperties.getTopics()).thenReturn(topicsProperties);
        when(topicsProperties.getWorkflow()).thenReturn(WORKFLOW_TOPIC);
    }

    @Test
    void publish_workflowCreatedEvent_sendsToKafka() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setWorkflowId("workflow-123");
        setupKafkaTopicMocks();
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");

        WorkflowCreatedOrUpdatedEvent protobufEvent = WorkflowCreatedOrUpdatedEvent.newBuilder()
                .setWorkflowId("workflow-123")
                .build();
        when(workflowMapper.map(any(WorkflowCreatedEvent.class))).thenReturn(protobufEvent);

        publishing.publish(event);

        verify(kafkaTemplate).send(eq(WORKFLOW_TOPIC), eq("workflow-123"), any(byte[].class));
    }

    @Test
    void publish_workflowUpdatedEvent_sendsToKafka() {
        WorkflowUpdatedEvent event = new WorkflowUpdatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setWorkflowId("workflow-123");
        setupKafkaTopicMocks();
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");

        WorkflowCreatedOrUpdatedEvent protobufEvent = WorkflowCreatedOrUpdatedEvent.newBuilder()
                .setWorkflowId("workflow-123")
                .build();
        when(workflowMapper.map(any(WorkflowUpdatedEvent.class))).thenReturn(protobufEvent);

        publishing.publish(event);

        verify(kafkaTemplate).send(eq(WORKFLOW_TOPIC), eq("workflow-123"), any(byte[].class));
    }

    @Test
    void publish_workflowCompletedEvent_sendsToKafka() {
        WorkflowCompletedEvent event = new WorkflowCompletedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setWorkflowId("workflow-123");
        setupKafkaTopicMocks();

        io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCompletedEvent protobufEvent =
                io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCompletedEvent.newBuilder()
                        .setWorkflowId("workflow-123")
                        .build();
        when(workflowMapper.map(any(WorkflowCompletedEvent.class))).thenReturn(protobufEvent);

        publishing.publish(event);

        verify(kafkaTemplate).send(eq(WORKFLOW_TOPIC), eq("workflow-123"), any(byte[].class));
    }

    @Test
    void publish_workflowCancelledEvent_sendsToKafka() {
        WorkflowCancelledEvent event = new WorkflowCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setWorkflowId("workflow-123");
        setupKafkaTopicMocks();

        io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCancelledEvent protobufEvent =
                io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCancelledEvent.newBuilder()
                        .setWorkflowId("workflow-123")
                        .build();
        when(workflowMapper.map(any(WorkflowCancelledEvent.class))).thenReturn(protobufEvent);

        publishing.publish(event);

        verify(kafkaTemplate).send(eq(WORKFLOW_TOPIC), eq("workflow-123"), any(byte[].class));
    }

    @Test
    void publish_unsupportedEventType_throwsRuntimeException() {
        assertThatThrownBy(() -> publishing.publish(new UnsupportedWorkflowEvent()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unsupported event type");
    }

    private static class UnsupportedWorkflowEvent implements io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent {
        @Override
        public String getEventId() { return null; }
        @Override
        public void setEventId(String eventId) {}
        @Override
        public String getWorkflowId() { return null; }
        @Override
        public void setWorkflowId(String workflowId) {}
        @Override
        public String getWorkflowModuleId() { return null; }
        @Override
        public void setWorkflowModuleId(String workflowModuleId) {}
        @Override
        public String getComment() { return null; }
        @Override
        public void setComment(String comment) {}
        @Override
        public java.time.OffsetDateTime getTimestamp() { return null; }
        @Override
        public void setTimestamp(java.time.OffsetDateTime timestamp) {}
    }
}
