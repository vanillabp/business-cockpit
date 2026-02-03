package io.vanillabp.cockpit.adapter.common.workflow.rest;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
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
class WorkflowRestPublishingTest {

    @Mock
    private BpmsApi bpmsApi;

    @Mock
    private VanillaBpCockpitProperties properties;

    @Mock
    private WorkflowRestMapper mapper;

    private WorkflowRestPublishing publishing;

    private static final String WORKER_ID = "test-worker";
    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @BeforeEach
    void setUp() {
        publishing = new WorkflowRestPublishing(WORKER_ID, Optional.of(bpmsApi), properties, mapper);
    }

    @Test
    void validateAutowiring_withBpmsApi_doesNotThrow() {
        publishing.validateAutowiring();
    }

    @Test
    void validateAutowiring_withoutBpmsApi_throwsRuntimeException() {
        WorkflowRestPublishing publishingWithoutApi = new WorkflowRestPublishing(
                WORKER_ID, Optional.empty(), properties, mapper);

        assertThatThrownBy(publishingWithoutApi::validateAutowiring)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("You have to configure either");
    }

    @Test
    void publish_workflowCreatedEvent_callsBpmsApi() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setWorkflowId("workflow-123");
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");
        when(mapper.map(any(WorkflowCreatedEvent.class))).thenReturn(new io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent());

        publishing.publish(event);

        verify(bpmsApi).workflowCreatedEvent(any());
    }

    @Test
    void publish_workflowUpdatedEvent_callsBpmsApi() {
        WorkflowUpdatedEvent event = new WorkflowUpdatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setWorkflowId("workflow-123");
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");
        when(mapper.map(any(WorkflowUpdatedEvent.class))).thenReturn(new io.vanillabp.cockpit.bpms.api.v1_1.WorkflowUpdatedEvent());

        publishing.publish(event);

        verify(bpmsApi).workflowUpdatedEvent(eq("workflow-123"), any());
    }

    @Test
    void publish_workflowCompletedEvent_callsBpmsApi() {
        WorkflowCompletedEvent event = new WorkflowCompletedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setWorkflowId("workflow-123");
        when(mapper.map(any(WorkflowCompletedEvent.class))).thenReturn(new io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCompletedEvent());

        publishing.publish(event);

        verify(bpmsApi).workflowCompletedEvent(eq("workflow-123"), any());
    }

    @Test
    void publish_workflowCancelledEvent_callsBpmsApi() {
        WorkflowCancelledEvent event = new WorkflowCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setWorkflowId("workflow-123");
        when(mapper.map(any(WorkflowCancelledEvent.class))).thenReturn(new io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCancelledEvent());

        publishing.publish(event);

        verify(bpmsApi).workflowCancelledEvent(eq("workflow-123"), any());
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
