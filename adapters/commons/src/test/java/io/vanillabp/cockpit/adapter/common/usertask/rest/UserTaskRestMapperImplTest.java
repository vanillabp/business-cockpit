package io.vanillabp.cockpit.adapter.common.usertask.rest;

import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUiUriType;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UiUriType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserTaskRestMapperImplTest {

    private UserTaskRestMapperImpl mapper;
    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @BeforeEach
    void setUp() {
        mapper = new UserTaskRestMapperImpl();
    }

    @Test
    void mapUserTaskActivatedEvent_withNull_returnsNull() {
        assertThat(mapper.map((UserTaskActivatedEvent) null)).isNull();
    }

    @Test
    void mapUserTaskActivatedEvent_mapsAllFields() {
        UserTaskActivatedEvent event = new UserTaskActivatedEvent();
        OffsetDateTime timestamp = OffsetDateTime.now();

        event.setEventId("event-123");
        event.setUserTaskId("task-456");
        event.setInitiator("user@example.com");
        event.setTimestamp(timestamp);
        event.setSource("camunda7");
        event.setComment("Test comment");

        io.vanillabp.cockpit.bpms.api.v1_1.UserTaskActivatedEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getUserTaskId()).isEqualTo("task-456");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getSource()).isEqualTo("camunda7");
        assertThat(result.getComment()).isEqualTo("Test comment");
    }

    @Test
    void mapUserTaskSuspendedEvent_withNull_returnsNull() {
        assertThat(mapper.map((UserTaskSuspendedEvent) null)).isNull();
    }

    @Test
    void mapUserTaskSuspendedEvent_mapsAllFields() {
        UserTaskSuspendedEvent event = new UserTaskSuspendedEvent();
        OffsetDateTime timestamp = OffsetDateTime.now();

        event.setEventId("event-123");
        event.setUserTaskId("task-456");
        event.setInitiator("user@example.com");
        event.setTimestamp(timestamp);
        event.setSource("camunda8");
        event.setComment("Suspended");

        io.vanillabp.cockpit.bpms.api.v1_1.UserTaskSuspendedEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getUserTaskId()).isEqualTo("task-456");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getSource()).isEqualTo("camunda8");
        assertThat(result.getComment()).isEqualTo("Suspended");
    }

    @Test
    void mapUserTaskCancelledEvent_withNull_returnsNull() {
        assertThat(mapper.map((UserTaskCancelledEvent) null)).isNull();
    }

    @Test
    void mapUserTaskCancelledEvent_mapsAllFields() {
        UserTaskCancelledEvent event = new UserTaskCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.now();
        OffsetDateTime dueDate = OffsetDateTime.now().plusDays(7);
        OffsetDateTime followUpDate = OffsetDateTime.now().plusDays(3);

        event.setEventId("event-123");
        event.setUserTaskId("task-456");
        event.setInitiator("user@example.com");
        event.setTimestamp(timestamp);
        event.setSource("camunda7");
        event.setComment("Cancelled");
        event.setBpmnProcessId("process-001");
        event.setBpmnProcessVersion("1.0.0");
        event.setWorkflowId("workflow-789");
        event.setSubWorkflowId("sub-workflow-012");
        event.setBusinessId("business-345");
        event.setBpmnTaskId("bpmn-task-001");
        event.setTaskDefinition("task-def-001");
        event.setUiUriPath("/tasks/form");
        event.setUiUriType(UserTaskUiUriType.WEBPACK_MF_REACT);
        event.setAssignee("assignee@example.com");
        event.setCandidateUsers(Arrays.asList("user1", "user2"));
        event.setCandidateGroups(Arrays.asList("group1", "group2"));
        event.setExcludedCandidateUsers(Arrays.asList("excluded1"));
        event.setDueDate(dueDate);
        event.setFollowUpDate(followUpDate);

        Map<String, String> workflowTitle = new HashMap<>();
        workflowTitle.put("en", "Workflow");
        event.setWorkflowTitle(workflowTitle);

        Map<String, String> title = new HashMap<>();
        title.put("en", "Task");
        event.setTitle(title);

        Map<String, String> taskDefTitle = new HashMap<>();
        taskDefTitle.put("en", "Task Definition");
        event.setTaskDefinitionTitle(taskDefTitle);

        Map<String, Object> details = new HashMap<>();
        details.put("key1", "value1");
        event.setDetails(details);

        event.setDetailsFulltextSearch("fulltext");

        io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCancelledEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getUserTaskId()).isEqualTo("task-456");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getSource()).isEqualTo("camunda7");
        assertThat(result.getWorkflowModuleId()).isEqualTo(WORKFLOW_MODULE_ID);
        assertThat(result.getComment()).isEqualTo("Cancelled");
        assertThat(result.getBpmnProcessId()).isEqualTo("process-001");
        assertThat(result.getBpmnProcessVersion()).isEqualTo("1.0.0");
        assertThat(result.getWorkflowTitle()).isEqualTo(workflowTitle);
        assertThat(result.getWorkflowId()).isEqualTo("workflow-789");
        assertThat(result.getSubWorkflowId()).isEqualTo("sub-workflow-012");
        assertThat(result.getBusinessId()).isEqualTo("business-345");
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getBpmnTaskId()).isEqualTo("bpmn-task-001");
        assertThat(result.getTaskDefinition()).isEqualTo("task-def-001");
        assertThat(result.getTaskDefinitionTitle()).isEqualTo(taskDefTitle);
        assertThat(result.getUiUriPath()).isEqualTo("/tasks/form");
        assertThat(result.getUiUriType()).isEqualTo(UiUriType.WEBPACK_MF_REACT);
        assertThat(result.getAssignee()).isEqualTo("assignee@example.com");
        assertThat(result.getCandidateUsers()).containsExactly("user1", "user2");
        assertThat(result.getCandidateGroups()).containsExactly("group1", "group2");
        assertThat(result.getExcludedCandidateUsers()).containsExactly("excluded1");
        assertThat(result.getDueDate()).isEqualTo(dueDate);
        assertThat(result.getFollowUpDate()).isEqualTo(followUpDate);
        assertThat(result.getDetails()).isEqualTo(details);
        assertThat(result.getDetailsFulltextSearch()).isEqualTo("fulltext");
        assertThat(result.getUpdated()).isTrue();
    }

    @Test
    void mapUserTaskCompletedEvent_withNull_returnsNull() {
        assertThat(mapper.map((UserTaskCompletedEvent) null)).isNull();
    }

    @Test
    void mapUserTaskCompletedEvent_mapsAllFields() {
        UserTaskCompletedEvent event = new UserTaskCompletedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.now();

        event.setEventId("event-123");
        event.setUserTaskId("task-456");
        event.setTimestamp(timestamp);
        event.setSource("camunda7");
        event.setUiUriType(UserTaskUiUriType.EXTERNAL);

        io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCompletedEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getUserTaskId()).isEqualTo("task-456");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getSource()).isEqualTo("camunda7");
        assertThat(result.getUiUriType()).isEqualTo(UiUriType.EXTERNAL);
        assertThat(result.getUpdated()).isTrue();
    }

    @Test
    void mapUserTaskCreatedEvent_withNull_returnsNull() {
        assertThat(mapper.map((UserTaskCreatedEvent) null)).isNull();
    }

    @Test
    void mapUserTaskCreatedEvent_mapsAllFields() {
        UserTaskCreatedEvent event = new UserTaskCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.now();

        event.setEventId("event-123");
        event.setUserTaskId("task-456");
        event.setTimestamp(timestamp);
        event.setSource("camunda8");
        event.setUiUriType(UserTaskUiUriType.WEBPACK_MF_REACT);

        io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCreatedEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getUserTaskId()).isEqualTo("task-456");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getSource()).isEqualTo("camunda8");
        assertThat(result.getUiUriType()).isEqualTo(UiUriType.WEBPACK_MF_REACT);
        assertThat(result.getUpdated()).isFalse();
    }

    @Test
    void mapUserTaskUpdatedEvent_withNull_returnsNull() {
        assertThat(mapper.map((UserTaskUpdatedEvent) null)).isNull();
    }

    @Test
    void mapUserTaskUpdatedEvent_mapsAllFields() {
        UserTaskUpdatedEvent event = new UserTaskUpdatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.now();

        event.setEventId("event-123");
        event.setUserTaskId("task-456");
        event.setTimestamp(timestamp);
        event.setSource("camunda7");
        event.setUiUriType(UserTaskUiUriType.EXTERNAL);

        io.vanillabp.cockpit.bpms.api.v1_1.UserTaskUpdatedEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getUserTaskId()).isEqualTo("task-456");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getSource()).isEqualTo("camunda7");
        assertThat(result.getUiUriType()).isEqualTo(UiUriType.EXTERNAL);
        assertThat(result.getUpdated()).isTrue();
    }

    @Test
    void mapUserTaskCancelledEvent_withEmptyCollections_mapsCorrectly() {
        UserTaskCancelledEvent event = new UserTaskCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setEventId("event-123");
        // Default lists are empty, not null

        io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCancelledEvent result = mapper.map(event);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getCandidateUsers()).isEmpty();
        assertThat(result.getCandidateGroups()).isEmpty();
        assertThat(result.getExcludedCandidateUsers()).isEmpty();
    }

    @Test
    void mapUserTaskCreatedEvent_withNullUiUriType_mapsToNull() {
        UserTaskCreatedEvent event = new UserTaskCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setEventId("event-123");
        event.setUiUriType(null);

        io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCreatedEvent result = mapper.map(event);

        assertThat(result.getUiUriType()).isNull();
    }
}
