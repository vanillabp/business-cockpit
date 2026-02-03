package io.vanillabp.cockpit.adapter.common.workflow.events;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowCreatedEventTest {

    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @Test
    void constructor_setsWorkflowModuleIdAndI18nLanguages() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);

        assertThat(event.getWorkflowModuleId()).isEqualTo(WORKFLOW_MODULE_ID);
        assertThat(event.getI18nLanguages()).isEqualTo(I18N_LANGUAGES);
    }

    @Test
    void extendsWorkflowEventImpl() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);

        assertThat(event).isInstanceOf(WorkflowEventImpl.class);
    }

    @Test
    void implementsWorkflowEvent() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);

        assertThat(event).isInstanceOf(WorkflowEvent.class);
    }

    @Test
    void canSetAndGetAllProperties() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);

        event.setEventId("event-123");
        event.setWorkflowId("workflow-456");
        event.setSource("test-source");

        assertThat(event.getEventId()).isEqualTo("event-123");
        assertThat(event.getWorkflowId()).isEqualTo("workflow-456");
        assertThat(event.getSource()).isEqualTo("test-source");
    }
}
