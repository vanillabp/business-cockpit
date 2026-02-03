package io.vanillabp.cockpit.adapter.common.usertask.events;

import io.vanillabp.spi.cockpit.details.DetailsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserTaskEventImplTest {

    private UserTaskEventImpl event;
    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @BeforeEach
    void setUp() {
        event = new UserTaskEventImpl(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
    }

    @Test
    void constructor_setsWorkflowModuleIdAndI18nLanguages() {
        assertThat(event.getWorkflowModuleId()).isEqualTo(WORKFLOW_MODULE_ID);
        assertThat(event.getI18nLanguages()).isEqualTo(I18N_LANGUAGES);
    }

    @Test
    void setAndGetEventId() {
        event.setEventId("event-123");
        assertThat(event.getEventId()).isEqualTo("event-123");
    }

    @Test
    void setAndGetEventType() {
        event.setEventType(DetailsEvent.Event.CREATED);
        assertThat(event.getEventType()).isEqualTo(DetailsEvent.Event.CREATED);
    }

    @Test
    void setAndGetUserTaskId() {
        event.setUserTaskId("task-456");
        assertThat(event.getUserTaskId()).isEqualTo("task-456");
    }

    @Test
    void getId_returnsUserTaskId() {
        event.setUserTaskId("task-789");
        assertThat(event.getId()).isEqualTo("task-789");
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
    void setAndGetWorkflowTitle() {
        Map<String, String> title = new HashMap<>();
        title.put("en", "Workflow Title");
        title.put("de", "Workflow Titel");
        event.setWorkflowTitle(title);
        assertThat(event.getWorkflowTitle()).isEqualTo(title);
    }

    @Test
    void setAndGetWorkflowId() {
        event.setWorkflowId("workflow-123");
        assertThat(event.getWorkflowId()).isEqualTo("workflow-123");
    }

    @Test
    void setAndGetSubWorkflowId() {
        event.setSubWorkflowId("sub-workflow-456");
        assertThat(event.getSubWorkflowId()).isEqualTo("sub-workflow-456");
    }

    @Test
    void setAndGetBusinessId() {
        event.setBusinessId("business-789");
        assertThat(event.getBusinessId()).isEqualTo("business-789");
    }

    @Test
    void setAndGetTitle() {
        Map<String, String> title = new HashMap<>();
        title.put("en", "Task Title");
        title.put("de", "Aufgabentitel");
        event.setTitle(title);
        assertThat(event.getTitle()).isEqualTo(title);
    }

    @Test
    void setAndGetBpmnTaskId() {
        event.setBpmnTaskId("bpmn-task-001");
        assertThat(event.getBpmnTaskId()).isEqualTo("bpmn-task-001");
    }

    @Test
    void setAndGetTaskDefinition() {
        event.setTaskDefinition("task-def-001");
        assertThat(event.getTaskDefinition()).isEqualTo("task-def-001");
    }

    @Test
    void setAndGetTaskDefinitionTitle() {
        Map<String, String> title = new HashMap<>();
        title.put("en", "Task Definition");
        event.setTaskDefinitionTitle(title);
        assertThat(event.getTaskDefinitionTitle()).isEqualTo(title);
    }

    @Test
    void setAndGetUiUriPath() {
        event.setUiUriPath("/tasks/form");
        assertThat(event.getUiUriPath()).isEqualTo("/tasks/form");
    }

    @Test
    void setAndGetUiUriType() {
        event.setUiUriType(UserTaskUiUriType.WEBPACK_MF_REACT);
        assertThat(event.getUiUriType()).isEqualTo(UserTaskUiUriType.WEBPACK_MF_REACT);
    }

    @Test
    void setAndGetAssignee() {
        event.setAssignee("assignee@example.com");
        assertThat(event.getAssignee()).isEqualTo("assignee@example.com");
    }

    @Test
    void setAndGetCandidateUsers() {
        List<String> users = Arrays.asList("user1", "user2");
        event.setCandidateUsers(users);
        assertThat(event.getCandidateUsers()).isEqualTo(users);
    }

    @Test
    void setAndGetCandidateGroups() {
        List<String> groups = Arrays.asList("group1", "group2");
        event.setCandidateGroups(groups);
        assertThat(event.getCandidateGroups()).isEqualTo(groups);
    }

    @Test
    void setAndGetExcludedCandidateUsers() {
        List<String> excluded = Arrays.asList("excluded1", "excluded2");
        event.setExcludedCandidateUsers(excluded);
        assertThat(event.getExcludedCandidateUsers()).isEqualTo(excluded);
    }

    @Test
    void setAndGetDueDate() {
        OffsetDateTime dueDate = OffsetDateTime.now().plusDays(7);
        event.setDueDate(dueDate);
        assertThat(event.getDueDate()).isEqualTo(dueDate);
    }

    @Test
    void setAndGetFollowUpDate() {
        OffsetDateTime followUpDate = OffsetDateTime.now().plusDays(3);
        event.setFollowUpDate(followUpDate);
        assertThat(event.getFollowUpDate()).isEqualTo(followUpDate);
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
    void setAndGetDetailsFulltextSearch() {
        event.setDetailsFulltextSearch("fulltext search content");
        assertThat(event.getDetailsFulltextSearch()).isEqualTo("fulltext search content");
    }

    @Test
    void setAndGetTemplateContext() {
        Object context = new Object();
        event.setTemplateContext(context);
        assertThat(event.getTemplateContext()).isEqualTo(context);
    }

    @Test
    void setAndGetI18nLanguages() {
        List<String> newLanguages = Arrays.asList("fr", "es");
        event.setI18nLanguages(newLanguages);
        assertThat(event.getI18nLanguages()).isEqualTo(newLanguages);
    }

    @Test
    void defaultMapsAreEmpty() {
        UserTaskEventImpl newEvent = new UserTaskEventImpl(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        assertThat(newEvent.getWorkflowTitle()).isEmpty();
        assertThat(newEvent.getTitle()).isEmpty();
        assertThat(newEvent.getTaskDefinitionTitle()).isEmpty();
        assertThat(newEvent.getDetails()).isEmpty();
    }

    @Test
    void defaultListsAreEmpty() {
        UserTaskEventImpl newEvent = new UserTaskEventImpl(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        assertThat(newEvent.getCandidateUsers()).isEmpty();
        assertThat(newEvent.getCandidateGroups()).isEmpty();
        assertThat(newEvent.getExcludedCandidateUsers()).isEmpty();
    }
}
