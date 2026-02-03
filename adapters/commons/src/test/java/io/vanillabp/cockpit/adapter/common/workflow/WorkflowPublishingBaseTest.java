package io.vanillabp.cockpit.adapter.common.workflow;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUiUriType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowPublishingBaseTest {

    @Mock
    private VanillaBpCockpitProperties properties;

    private TestWorkflowPublishingBase publishingBase;

    private static final String WORKER_ID = "test-worker";
    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @BeforeEach
    void setUp() {
        publishingBase = new TestWorkflowPublishingBase(WORKER_ID, properties);
    }

    @Test
    void editWorkflowCreatedOrUpdatedEvent_setsSource() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("WEBPACK_MF_REACT");

        publishingBase.callEditWorkflowCreatedOrUpdatedEvent(event);

        assertThat(event.getSource()).isEqualTo(WORKER_ID);
    }

    @Test
    void editWorkflowCreatedOrUpdatedEvent_setsUiUriPath() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/custom/workflow/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");

        publishingBase.callEditWorkflowCreatedOrUpdatedEvent(event);

        assertThat(event.getUiUriPath()).isEqualTo("/custom/workflow/path");
    }

    @Test
    void editWorkflowCreatedOrUpdatedEvent_setsUiUriType_WEBPACK_MF_REACT() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("WEBPACK_MF_REACT");

        publishingBase.callEditWorkflowCreatedOrUpdatedEvent(event);

        assertThat(event.getUiUriType()).isEqualTo(WorkflowUiUriType.WEBPACK_MF_REACT);
    }

    @Test
    void editWorkflowCreatedOrUpdatedEvent_setsUiUriType_EXTERNAL() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");

        publishingBase.callEditWorkflowCreatedOrUpdatedEvent(event);

        assertThat(event.getUiUriType()).isEqualTo(WorkflowUiUriType.EXTERNAL);
    }

    @Test
    void editWorkflowCreatedOrUpdatedEvent_withInvalidUiUriType_throwsRuntimeException() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("INVALID_TYPE");

        assertThatThrownBy(() -> publishingBase.callEditWorkflowCreatedOrUpdatedEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unsupported UI-URI-type");
    }

    private static class TestWorkflowPublishingBase extends WorkflowPublishingBase {

        protected TestWorkflowPublishingBase(String workerId, VanillaBpCockpitProperties properties) {
            super(workerId, properties);
        }

        public void callEditWorkflowCreatedOrUpdatedEvent(WorkflowCreatedEvent event) {
            editWorkflowCreatedOrUpdatedEvent(event);
        }
    }
}
