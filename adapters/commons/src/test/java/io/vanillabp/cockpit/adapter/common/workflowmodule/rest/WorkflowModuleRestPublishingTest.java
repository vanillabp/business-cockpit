package io.vanillabp.cockpit.adapter.common.workflowmodule.rest;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflowmodule.events.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowModuleRestPublishingTest {

    @Mock
    private BpmsApi bpmsApi;

    @Mock
    private VanillaBpCockpitProperties properties;

    @Mock
    private ObjectProvider<List<WorkflowModuleDetailsProvider>> detailsProvidersProvider;

    @Mock
    private WorkflowModuleRestMapper mapper;

    private WorkflowModuleRestPublishing publishing;

    private static final String WORKER_ID = "test-worker";
    private static final String WORKFLOW_MODULE_ID = "test-module";

    @BeforeEach
    void setUp() {
        publishing = new WorkflowModuleRestPublishing(WORKER_ID, Optional.of(bpmsApi), properties, detailsProvidersProvider, mapper);
    }

    @Test
    void validateAutowiring_withBpmsApi_doesNotThrow() {
        publishing.validateAutowiring();
    }

    @Test
    void validateAutowiring_withoutBpmsApi_throwsRuntimeException() {
        WorkflowModuleRestPublishing publishingWithoutApi = new WorkflowModuleRestPublishing(
                WORKER_ID, Optional.empty(), properties, detailsProvidersProvider, mapper);

        assertThatThrownBy(publishingWithoutApi::validateAutowiring)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("You have to configure either");
    }

    @Test
    void publish_registerWorkflowModuleEvent_callsBpmsApi() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();
        event.setId(WORKFLOW_MODULE_ID);
        when(properties.getWorkflowModuleUri(WORKFLOW_MODULE_ID)).thenReturn("http://localhost:8080");
        when(mapper.map(any(RegisterWorkflowModuleEvent.class))).thenReturn(new io.vanillabp.cockpit.bpms.api.v1_1.RegisterWorkflowModuleEvent());

        publishing.publish(event);

        verify(bpmsApi).registerWorkflowModule(eq(WORKFLOW_MODULE_ID), any());
    }

    @Test
    void publish_unsupportedEventType_throwsRuntimeException() {
        assertThatThrownBy(() -> publishing.publish(new UnsupportedWorkflowModuleEvent()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unsupported event type");
    }

    private static class UnsupportedWorkflowModuleEvent implements io.vanillabp.cockpit.adapter.common.workflowmodule.events.WorkflowModuleEvent {
        @Override
        public String getEventId() { return null; }
        @Override
        public void setEventId(String eventId) {}
        @Override
        public String getSource() { return null; }
        @Override
        public void setSource(String source) {}
        @Override
        public java.time.OffsetDateTime getTimestamp() { return null; }
        @Override
        public void setTimestamp(java.time.OffsetDateTime timestamp) {}
        @Override
        public String getId() { return null; }
        @Override
        public void setId(String id) {}
    }
}
