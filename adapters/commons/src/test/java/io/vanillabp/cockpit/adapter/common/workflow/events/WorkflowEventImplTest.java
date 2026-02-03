package io.vanillabp.cockpit.adapter.common.workflow.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowEventImplTest {

    private WorkflowEventImpl event;
    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @BeforeEach
    void setUp() {
        event = new WorkflowEventImpl(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
    }

    @Test
    void constructor_setsWorkflowModuleIdAndI18nLanguages() {
        assertThat(event.getWorkflowModuleId()).isEqualTo(WORKFLOW_MODULE_ID);
        assertThat(event.getI18nLanguages()).isEqualTo(I18N_LANGUAGES);
    }

    @Test
    void defaultConstructor_createsEmptyEvent() {
        WorkflowEventImpl emptyEvent = new WorkflowEventImpl();
        assertThat(emptyEvent.getWorkflowModuleId()).isNull();
        assertThat(emptyEvent.getI18nLanguages()).isNull();
    }

    @Test
    void setAndGetEventId() {
        event.setEventId("event-123");
        assertThat(event.getEventId()).isEqualTo("event-123");
    }

    @Test
    void setAndGetWorkflowId() {
        event.setWorkflowId("workflow-456");
        assertThat(event.getWorkflowId()).isEqualTo("workflow-456");
    }

    @Test
    void setAndGetBusinessId() {
        event.setBusinessId("business-789");
        assertThat(event.getBusinessId()).isEqualTo("business-789");
    }

    @Test
    void setAndGetInitiator() {
        event.setInitiator("user@example.com");
        assertThat(event.getInitiator()).isEqualTo("user@example.com");
    }

    @Test
    void setAndGetTimestamp() {
        OffsetDateTime timestamp = OffsetDateTime.now();
        event.setTimestamp(timestamp);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void getEventTimestamp_returnsTimestamp() {
        OffsetDateTime timestamp = OffsetDateTime.now();
        event.setTimestamp(timestamp);
        assertThat(event.getEventTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void setAndGetSource() {
        event.setSource("camunda7");
        assertThat(event.getSource()).isEqualTo("camunda7");
    }

    @Test
    void setAndGetTitle() {
        Map<String, String> title = new HashMap<>();
        title.put("en", "Workflow Title");
        title.put("de", "Workflow Titel");
        event.setTitle(title);
        assertThat(event.getTitle()).isEqualTo(title);
    }

    @Test
    void setAndGetComment() {
        event.setComment("Test comment");
        assertThat(event.getComment()).isEqualTo("Test comment");
    }

    @Test
    void setAndGetBpmnProcessId() {
        event.setBpmnProcessId("process-001");
        assertThat(event.getBpmnProcessId()).isEqualTo("process-001");
    }

    @Test
    void setAndGetBpmnProcessVersion() {
        event.setBpmnProcessVersion("1.0.0");
        assertThat(event.getBpmnProcessVersion()).isEqualTo("1.0.0");
    }

    @Test
    void setAndGetUiUriPath() {
        event.setUiUriPath("/workflow/details");
        assertThat(event.getUiUriPath()).isEqualTo("/workflow/details");
    }

    @Test
    void setAndGetUiUriType() {
        event.setUiUriType(WorkflowUiUriType.WEBPACK_MF_REACT);
        assertThat(event.getUiUriType()).isEqualTo(WorkflowUiUriType.WEBPACK_MF_REACT);
    }

    @Test
    void setAndGetDetails() {
        Map<String, Object> details = new HashMap<>();
        details.put("key1", "value1");
        details.put("key2", 123);
        event.setDetails(details);
        assertThat(event.getDetails()).isEqualTo(details);
    }

    @Test
    void getDetailsCharacteristics_returnsNull() {
        assertThat(event.getDetailsCharacteristics()).isNull();
    }

    @Test
    void setAndGetDetailsFulltextSearch() {
        event.setDetailsFulltextSearch("fulltext search content");
        assertThat(event.getDetailsFulltextSearch()).isEqualTo("fulltext search content");
    }

    @Test
    void setAndGetI18nLanguages() {
        List<String> newLanguages = Arrays.asList("fr", "es");
        event.setI18nLanguages(newLanguages);
        assertThat(event.getI18nLanguages()).isEqualTo(newLanguages);
    }

    @Test
    void setAndGetTemplateContext() {
        Object context = new Object();
        event.setTemplateContext(context);
        assertThat(event.getTemplateContext()).isEqualTo(context);
    }

    @Test
    void setAndGetAccessibleToUsers() {
        List<String> users = Arrays.asList("user1", "user2");
        event.setAccessibleToUsers(users);
        assertThat(event.getAccessibleToUsers()).isEqualTo(users);
    }

    @Test
    void setAndGetAccessibleToGroups() {
        List<String> groups = Arrays.asList("group1", "group2");
        event.setAccessibleToGroups(groups);
        assertThat(event.getAccessibleToGroups()).isEqualTo(groups);
    }

    @Test
    void defaultMapsAreEmpty() {
        WorkflowEventImpl newEvent = new WorkflowEventImpl(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        assertThat(newEvent.getTitle()).isEmpty();
        assertThat(newEvent.getDetails()).isEmpty();
    }

    @Test
    void implementsWorkflowEvent() {
        assertThat(event).isInstanceOf(WorkflowEvent.class);
    }
}
