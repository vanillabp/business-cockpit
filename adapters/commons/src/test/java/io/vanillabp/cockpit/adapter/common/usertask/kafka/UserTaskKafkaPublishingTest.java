package io.vanillabp.cockpit.adapter.common.usertask.kafka;

import io.vanillabp.cockpit.adapter.common.properties.CockpitProperties;
import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEventImpl;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.commons.kafka.KafkaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTaskKafkaPublishingTest {

    @Mock
    private VanillaBpCockpitProperties properties;

    @Mock
    private CockpitProperties cockpitProperties;

    @Mock
    private KafkaProperties kafkaProperties;

    @Mock
    private KafkaProperties.Topics topicsProperties;

    @Mock
    private UserTaskProtobufMapper userTaskMapper;

    @Mock
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    private UserTaskKafkaPublishing publishing;

    private static final String WORKER_ID = "test-worker";
    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final String USER_TASK_TOPIC = "user-task-topic";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @BeforeEach
    void setUp() {
        publishing = new UserTaskKafkaPublishing(WORKER_ID, properties, userTaskMapper, kafkaTemplate);
    }

    private void setupKafkaTopicMocks() {
        when(properties.getCockpit()).thenReturn(cockpitProperties);
        when(cockpitProperties.getKafka()).thenReturn(kafkaProperties);
        when(kafkaProperties.getTopics()).thenReturn(topicsProperties);
        when(topicsProperties.getUserTask()).thenReturn(USER_TASK_TOPIC);
    }

    @Test
    void publish_userTaskCreatedEvent_sendsToKafka() {
        UserTaskCreatedEvent event = new UserTaskCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setUserTaskId("task-123");
        setupKafkaTopicMocks();
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");

        UserTaskCreatedOrUpdatedEvent protobufEvent = UserTaskCreatedOrUpdatedEvent.newBuilder()
                .setUserTaskId("task-123")
                .build();
        when(userTaskMapper.map(any(UserTaskCreatedEvent.class))).thenReturn(protobufEvent);

        publishing.publish(event);

        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(kafkaTemplate).send(eq(USER_TASK_TOPIC), eq("task-123"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).isNotNull();
    }

    @Test
    void publish_userTaskUpdatedEvent_sendsToKafka() {
        UserTaskUpdatedEvent event = new UserTaskUpdatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setUserTaskId("task-123");
        setupKafkaTopicMocks();
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");

        UserTaskCreatedOrUpdatedEvent protobufEvent = UserTaskCreatedOrUpdatedEvent.newBuilder()
                .setUserTaskId("task-123")
                .build();
        when(userTaskMapper.map(any(UserTaskUpdatedEvent.class))).thenReturn(protobufEvent);

        publishing.publish(event);

        verify(kafkaTemplate).send(eq(USER_TASK_TOPIC), eq("task-123"), any(byte[].class));
    }

    @Test
    void publish_userTaskCompletedEvent_sendsToKafka() {
        // Note: UserTaskCompletedEvent extends UserTaskEventImpl, so it's caught by that branch
        // and processed as a created/updated event (maps via map(UserTaskEventImpl) overload)
        UserTaskCompletedEvent event = new UserTaskCompletedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setUserTaskId("task-123");
        setupKafkaTopicMocks();
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");

        UserTaskCreatedOrUpdatedEvent protobufEvent = UserTaskCreatedOrUpdatedEvent.newBuilder()
                .setUserTaskId("task-123")
                .build();
        // The code calls map(UserTaskEventImpl) due to instanceof check order
        when(userTaskMapper.map(any(UserTaskEventImpl.class))).thenReturn(protobufEvent);

        publishing.publish(event);

        verify(kafkaTemplate).send(eq(USER_TASK_TOPIC), eq("task-123"), any(byte[].class));
    }

    @Test
    void publish_userTaskCancelledEvent_sendsToKafka() {
        // Note: UserTaskCancelledEvent extends UserTaskEventImpl, so it's caught by that branch
        // and processed as a created/updated event (maps via map(UserTaskEventImpl) overload)
        UserTaskCancelledEvent event = new UserTaskCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setUserTaskId("task-123");
        setupKafkaTopicMocks();
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");

        UserTaskCreatedOrUpdatedEvent protobufEvent = UserTaskCreatedOrUpdatedEvent.newBuilder()
                .setUserTaskId("task-123")
                .build();
        // The code calls map(UserTaskEventImpl) due to instanceof check order
        when(userTaskMapper.map(any(UserTaskEventImpl.class))).thenReturn(protobufEvent);

        publishing.publish(event);

        verify(kafkaTemplate).send(eq(USER_TASK_TOPIC), eq("task-123"), any(byte[].class));
    }

    @Test
    void publish_userTaskActivatedEvent_sendsToKafka() {
        UserTaskActivatedEvent event = new UserTaskActivatedEvent();
        event.setUserTaskId("task-123");
        setupKafkaTopicMocks();

        io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskActivatedEvent protobufEvent =
                io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskActivatedEvent.newBuilder()
                        .setUserTaskId("task-123")
                        .build();
        when(userTaskMapper.map(any(UserTaskActivatedEvent.class))).thenReturn(protobufEvent);

        publishing.publish(event);

        verify(kafkaTemplate).send(eq(USER_TASK_TOPIC), eq("task-123"), any(byte[].class));
    }

    @Test
    void publish_userTaskSuspendedEvent_sendsToKafka() {
        UserTaskSuspendedEvent event = new UserTaskSuspendedEvent();
        event.setUserTaskId("task-123");
        setupKafkaTopicMocks();

        io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskSuspendedEvent protobufEvent =
                io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskSuspendedEvent.newBuilder()
                        .setUserTaskId("task-123")
                        .build();
        when(userTaskMapper.map(any(UserTaskSuspendedEvent.class))).thenReturn(protobufEvent);

        publishing.publish(event);

        verify(kafkaTemplate).send(eq(USER_TASK_TOPIC), eq("task-123"), any(byte[].class));
    }

    @Test
    void publish_unsupportedEventType_throwsRuntimeException() {
        assertThatThrownBy(() -> publishing.publish(new UnsupportedUserTaskEvent()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unsupported event type");
    }

    private static class UnsupportedUserTaskEvent implements io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent {
        @Override
        public String getEventId() { return null; }
        @Override
        public void setEventId(String id) {}
        @Override
        public io.vanillabp.spi.cockpit.details.DetailsEvent.Event getEventType() { return null; }
        @Override
        public String getUserTaskId() { return null; }
        @Override
        public void setUserTaskId(String id) {}
        @Override
        public String getWorkflowModuleId() { return null; }
        @Override
        public void setWorkflowModuleId(String id) {}
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
