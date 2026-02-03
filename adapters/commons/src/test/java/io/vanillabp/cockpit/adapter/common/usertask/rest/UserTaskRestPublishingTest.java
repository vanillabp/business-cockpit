package io.vanillabp.cockpit.adapter.common.usertask.rest;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTaskRestPublishingTest {

    @Mock
    private BpmsApi bpmsApi;

    @Mock
    private VanillaBpCockpitProperties properties;

    @Mock
    private UserTaskRestMapper mapper;

    private UserTaskRestPublishing publishing;

    private static final String WORKER_ID = "test-worker";
    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @BeforeEach
    void setUp() {
        publishing = new UserTaskRestPublishing(WORKER_ID, Optional.of(bpmsApi), properties, mapper);
    }

    @Test
    void validateAutowiring_withBpmsApi_doesNotThrow() {
        publishing.validateAutowiring();
    }

    @Test
    void validateAutowiring_withoutBpmsApi_throwsRuntimeException() {
        UserTaskRestPublishing publishingWithoutApi = new UserTaskRestPublishing(
                WORKER_ID, Optional.empty(), properties, mapper);

        assertThatThrownBy(publishingWithoutApi::validateAutowiring)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("You have to configure either");
    }

    @Test
    void publish_userTaskCreatedEvent_callsBpmsApi() {
        UserTaskCreatedEvent event = new UserTaskCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setUserTaskId("task-123");
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");
        when(mapper.map(any(UserTaskCreatedEvent.class))).thenReturn(new io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCreatedEvent());

        publishing.publish(event);

        verify(bpmsApi).userTaskCreatedEvent(any());
    }

    @Test
    void publish_userTaskUpdatedEvent_callsBpmsApi() {
        UserTaskUpdatedEvent event = new UserTaskUpdatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setUserTaskId("task-123");
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");
        when(mapper.map(any(UserTaskUpdatedEvent.class))).thenReturn(new io.vanillabp.cockpit.bpms.api.v1_1.UserTaskUpdatedEvent());

        publishing.publish(event);

        verify(bpmsApi).userTaskUpdatedEvent(eq("task-123"), any());
    }

    @Test
    void publish_userTaskCompletedEvent_callsBpmsApi() {
        UserTaskCompletedEvent event = new UserTaskCompletedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setUserTaskId("task-123");
        when(mapper.map(any(UserTaskCompletedEvent.class))).thenReturn(new io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCompletedEvent());

        publishing.publish(event);

        verify(bpmsApi).userTaskCompletedEvent(eq("task-123"), any());
    }

    @Test
    void publish_userTaskCancelledEvent_callsBpmsApi() {
        UserTaskCancelledEvent event = new UserTaskCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setUserTaskId("task-123");
        when(mapper.map(any(UserTaskCancelledEvent.class))).thenReturn(new io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCancelledEvent());

        publishing.publish(event);

        verify(bpmsApi).userTaskCancelledEvent(eq("task-123"), any());
    }

    @Test
    void publish_userTaskActivatedEvent_callsBpmsApi() {
        UserTaskActivatedEvent event = new UserTaskActivatedEvent();
        event.setUserTaskId("task-123");
        when(mapper.map(any(UserTaskActivatedEvent.class))).thenReturn(new io.vanillabp.cockpit.bpms.api.v1_1.UserTaskActivatedEvent());

        publishing.publish(event);

        verify(bpmsApi).userTaskActivatedEvent(eq("task-123"), any());
    }

    @Test
    void publish_userTaskSuspendedEvent_callsBpmsApi() {
        UserTaskSuspendedEvent event = new UserTaskSuspendedEvent();
        event.setUserTaskId("task-123");
        when(mapper.map(any(UserTaskSuspendedEvent.class))).thenReturn(new io.vanillabp.cockpit.bpms.api.v1_1.UserTaskSuspendedEvent());

        publishing.publish(event);

        verify(bpmsApi).userTaskSuspendedEvent(eq("task-123"), any());
    }

    @Test
    void publish_unsupportedEventType_throwsRuntimeException() {
        // Create an anonymous implementation of UserTaskEvent that is not one of the supported types
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
