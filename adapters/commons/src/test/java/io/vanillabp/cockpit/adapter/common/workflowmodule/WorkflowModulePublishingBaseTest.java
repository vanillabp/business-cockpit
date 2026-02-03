package io.vanillabp.cockpit.adapter.common.workflowmodule;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflowmodule.events.RegisterWorkflowModuleEvent;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowModulePublishingBaseTest {

    @Mock
    private VanillaBpCockpitProperties properties;

    @Mock
    private ObjectProvider<List<WorkflowModuleDetailsProvider>> detailsProvidersProvider;

    @Mock
    private WorkflowModuleDetailsProvider detailsProvider;

    private TestWorkflowModulePublishingBase publishingBase;

    private static final String WORKER_ID = "test-worker";
    private static final String WORKFLOW_MODULE_ID = "test-module";

    @BeforeEach
    void setUp() {
        publishingBase = new TestWorkflowModulePublishingBase(WORKER_ID, properties, detailsProvidersProvider);
    }

    @Test
    void enrichRegisterWorkflowModuleEvent_setsEventId() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();
        event.setId(WORKFLOW_MODULE_ID);
        when(properties.getWorkflowModuleUri(WORKFLOW_MODULE_ID)).thenReturn("http://localhost:8080");

        publishingBase.callEnrichRegisterWorkflowModuleEvent(event);

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).isNotEmpty();
    }

    @Test
    void enrichRegisterWorkflowModuleEvent_setsSource() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();
        event.setId(WORKFLOW_MODULE_ID);
        when(properties.getWorkflowModuleUri(WORKFLOW_MODULE_ID)).thenReturn("http://localhost:8080");

        publishingBase.callEnrichRegisterWorkflowModuleEvent(event);

        assertThat(event.getSource()).isEqualTo(WORKER_ID);
    }

    @Test
    void enrichRegisterWorkflowModuleEvent_setsTimestamp() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();
        event.setId(WORKFLOW_MODULE_ID);
        when(properties.getWorkflowModuleUri(WORKFLOW_MODULE_ID)).thenReturn("http://localhost:8080");

        publishingBase.callEnrichRegisterWorkflowModuleEvent(event);

        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void enrichRegisterWorkflowModuleEvent_setsApiPaths() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();
        event.setId(WORKFLOW_MODULE_ID);
        when(properties.getWorkflowModuleUri(WORKFLOW_MODULE_ID)).thenReturn("http://localhost:8080");

        publishingBase.callEnrichRegisterWorkflowModuleEvent(event);

        assertThat(event.getTaskProviderApiUriPath()).isEqualTo("/task-provider");
        assertThat(event.getWorkflowProviderApiUriPath()).isEqualTo("/workflow-provider");
    }

    @Test
    void enrichRegisterWorkflowModuleEvent_setsUri() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();
        event.setId(WORKFLOW_MODULE_ID);
        when(properties.getWorkflowModuleUri(WORKFLOW_MODULE_ID)).thenReturn("http://example.com:9090");

        publishingBase.callEnrichRegisterWorkflowModuleEvent(event);

        assertThat(event.getUri()).isEqualTo("http://example.com:9090");
    }

    @Test
    void enrichRegisterWorkflowModuleEvent_setsAccessibleToGroupsFromProvider() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();
        event.setId(WORKFLOW_MODULE_ID);
        when(properties.getWorkflowModuleUri(WORKFLOW_MODULE_ID)).thenReturn("http://localhost:8080");
        when(detailsProvider.getWorkflowModuleId()).thenReturn(WORKFLOW_MODULE_ID);
        when(detailsProvider.getAccessibleToGroups()).thenReturn(Arrays.asList("admin", "users"));

        doAnswer(invocation -> {
            var consumer = invocation.getArgument(0, java.util.function.Consumer.class);
            consumer.accept(Collections.singletonList(detailsProvider));
            return null;
        }).when(detailsProvidersProvider).ifAvailable(any());

        publishingBase.callEnrichRegisterWorkflowModuleEvent(event);

        assertThat(event.getAccessibleToGroups()).containsExactly("admin", "users");
    }

    @Test
    void enrichRegisterWorkflowModuleEvent_whenNoMatchingProvider_doesNotSetGroups() {
        RegisterWorkflowModuleEvent event = new RegisterWorkflowModuleEvent();
        event.setId(WORKFLOW_MODULE_ID);
        when(properties.getWorkflowModuleUri(WORKFLOW_MODULE_ID)).thenReturn("http://localhost:8080");
        when(detailsProvider.getWorkflowModuleId()).thenReturn("different-module");

        doAnswer(invocation -> {
            var consumer = invocation.getArgument(0, java.util.function.Consumer.class);
            consumer.accept(Collections.singletonList(detailsProvider));
            return null;
        }).when(detailsProvidersProvider).ifAvailable(any());

        publishingBase.callEnrichRegisterWorkflowModuleEvent(event);

        assertThat(event.getAccessibleToGroups()).isNull();
    }

    private static class TestWorkflowModulePublishingBase extends WorkflowModulePublishingBase {

        protected TestWorkflowModulePublishingBase(
                String workerId,
                VanillaBpCockpitProperties properties,
                ObjectProvider<List<WorkflowModuleDetailsProvider>> workflowModuleDetailsProviders) {
            super(workerId, properties, workflowModuleDetailsProviders);
        }

        public void callEnrichRegisterWorkflowModuleEvent(RegisterWorkflowModuleEvent event) {
            enrichRegisterWorkflowModuleEvent(event);
        }
    }
}
