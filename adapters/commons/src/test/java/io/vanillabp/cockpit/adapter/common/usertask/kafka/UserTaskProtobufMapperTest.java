package io.vanillabp.cockpit.adapter.common.usertask.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskActivatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCancelledEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCompletedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUiUriType;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UserTaskProtobufMapperTest {

    private UserTaskProtobufMapper mapper;
    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @BeforeEach
    void setUp() {
        mapper = new UserTaskProtobufMapper(new ObjectMapper());
    }

    @Test
    void mapUserTaskCreatedEvent_mapsRequiredFields() {
        UserTaskCreatedEvent event = createUserTaskCreatedEvent();

        UserTaskCreatedOrUpdatedEvent result = mapper.map(event);

        assertThat(result.getUserTaskId()).isEqualTo("task-123");
        assertThat(result.getBpmnProcessId()).isEqualTo("process-001");
        assertThat(result.getTaskDefinition()).isEqualTo("task-def-001");
        assertThat(result.getWorkflowModuleId()).isEqualTo(WORKFLOW_MODULE_ID);
        assertThat(result.getUiUriPath()).isEqualTo("/tasks/form");
        assertThat(result.getUiUriType()).isEqualTo("WEBPACK_MF_REACT");
        assertThat(result.getUpdated()).isFalse();
    }

    @Test
    void mapUserTaskUpdatedEvent_setsUpdatedToTrue() {
        UserTaskUpdatedEvent event = createUserTaskUpdatedEvent();

        UserTaskCreatedOrUpdatedEvent result = mapper.map(event);

        assertThat(result.getUpdated()).isTrue();
    }

    @Test
    void mapUserTaskCreatedEvent_mapsOptionalFields() {
        UserTaskCreatedEvent event = createUserTaskCreatedEvent();
        event.setInitiator("user@example.com");
        event.setSource("camunda7");
        event.setComment("Test comment");
        event.setBpmnProcessVersion("1.0.0");
        event.setWorkflowId("workflow-456");
        event.setSubWorkflowId("sub-workflow-789");
        event.setBusinessId("business-012");
        event.setBpmnTaskId("bpmn-task-345");
        event.setAssignee("assignee@example.com");
        event.setCandidateUsers(Arrays.asList("user1", "user2"));
        event.setCandidateGroups(Arrays.asList("group1", "group2"));
        event.setExcludedCandidateUsers(Arrays.asList("excluded1"));
        event.setDueDate(OffsetDateTime.of(2024, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC));
        event.setFollowUpDate(OffsetDateTime.of(2024, 12, 15, 12, 0, 0, 0, ZoneOffset.UTC));
        event.setDetailsFulltextSearch("fulltext search");

        Map<String, String> workflowTitle = new HashMap<>();
        workflowTitle.put("en", "Workflow");
        event.setWorkflowTitle(workflowTitle);

        Map<String, String> taskDefTitle = new HashMap<>();
        taskDefTitle.put("en", "Task Definition");
        event.setTaskDefinitionTitle(taskDefTitle);

        Map<String, Object> details = new HashMap<>();
        details.put("key1", "value1");
        event.setDetails(details);

        UserTaskCreatedOrUpdatedEvent result = mapper.map(event);

        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getSource()).isEqualTo("camunda7");
        assertThat(result.getComment()).isEqualTo("Test comment");
        assertThat(result.getBpmnProcessVersion()).isEqualTo("1.0.0");
        assertThat(result.getWorkflowId()).isEqualTo("workflow-456");
        assertThat(result.getSubWorkflowId()).isEqualTo("sub-workflow-789");
        assertThat(result.getBusinessId()).isEqualTo("business-012");
        assertThat(result.getBpmnTaskId()).isEqualTo("bpmn-task-345");
        assertThat(result.getAssignee()).isEqualTo("assignee@example.com");
        assertThat(result.getCandidateUsersList()).containsExactly("user1", "user2");
        assertThat(result.getCandidateGroupsList()).containsExactly("group1", "group2");
        assertThat(result.getExcludedCandidateUsersList()).containsExactly("excluded1");
        assertThat(result.hasDetails()).isTrue();
        assertThat(result.getDetailsFulltextSearch()).isEqualTo("fulltext search");
    }

    @Test
    void mapUserTaskCompletedEvent_mapsAllFields() {
        UserTaskCompletedEvent event = new UserTaskCompletedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        event.setEventId("event-123");
        event.setUserTaskId("task-456");
        event.setTimestamp(timestamp);
        event.setInitiator("user@example.com");
        event.setSource("camunda8");
        event.setComment("Completed");

        io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCompletedEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getUserTaskId()).isEqualTo("task-456");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getSource()).isEqualTo("camunda8");
        assertThat(result.getComment()).isEqualTo("Completed");
    }

    @Test
    void mapUserTaskActivatedEvent_mapsAllFields() {
        UserTaskActivatedEvent event = new UserTaskActivatedEvent();
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        event.setEventId("event-123");
        event.setUserTaskId("task-456");
        event.setTimestamp(timestamp);
        event.setInitiator("user@example.com");
        event.setSource("camunda7");
        event.setComment("Activated");

        io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskActivatedEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getUserTaskId()).isEqualTo("task-456");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getSource()).isEqualTo("camunda7");
        assertThat(result.getComment()).isEqualTo("Activated");
    }

    @Test
    void mapUserTaskSuspendedEvent_mapsAllFields() {
        UserTaskSuspendedEvent event = new UserTaskSuspendedEvent();
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        event.setEventId("event-123");
        event.setUserTaskId("task-456");
        event.setTimestamp(timestamp);
        event.setInitiator("user@example.com");
        event.setSource("camunda8");
        event.setComment("Suspended");

        io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskSuspendedEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getUserTaskId()).isEqualTo("task-456");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getSource()).isEqualTo("camunda8");
        assertThat(result.getComment()).isEqualTo("Suspended");
    }

    @Test
    void mapUserTaskCancelledEvent_mapsAllFields() {
        UserTaskCancelledEvent event = new UserTaskCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        event.setEventId("event-123");
        event.setUserTaskId("task-456");
        event.setTimestamp(timestamp);
        event.setInitiator("user@example.com");
        event.setSource("camunda7");
        event.setComment("Cancelled");

        io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCancelledEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getUserTaskId()).isEqualTo("task-456");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getSource()).isEqualTo("camunda7");
        assertThat(result.getComment()).isEqualTo("Cancelled");
    }

    @Test
    void mapTimeStamp_mapsCorrectly() {
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 45, 123456789, ZoneOffset.UTC);

        com.google.protobuf.Timestamp result = mapper.mapTimeStamp(timestamp);

        assertThat(result.getSeconds()).isEqualTo(timestamp.toInstant().getEpochSecond());
        assertThat(result.getNanos()).isEqualTo(123456789);
    }

    @Test
    void mapDetailsToProtobuf_mapsCorrectly() {
        Map<String, Object> details = new HashMap<>();
        details.put("stringKey", "stringValue");
        details.put("numericKey", 42);
        details.put("booleanKey", true);

        var result = mapper.mapDetailsToProtobuf(details);

        assertThat(result.getDetailsMap()).containsKey("stringKey");
        assertThat(result.getDetailsMap()).containsKey("numericKey");
        assertThat(result.getDetailsMap()).containsKey("booleanKey");
    }

    private UserTaskCreatedEvent createUserTaskCreatedEvent() {
        UserTaskCreatedEvent event = new UserTaskCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        event.setEventId("event-123");
        event.setUserTaskId("task-123");
        event.setTimestamp(timestamp);
        event.setBpmnProcessId("process-001");
        event.setTaskDefinition("task-def-001");
        event.setUiUriPath("/tasks/form");
        event.setUiUriType(UserTaskUiUriType.WEBPACK_MF_REACT);

        Map<String, String> title = new HashMap<>();
        title.put("en", "Task Title");
        event.setTitle(title);

        return event;
    }

    private UserTaskUpdatedEvent createUserTaskUpdatedEvent() {
        UserTaskUpdatedEvent event = new UserTaskUpdatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        event.setEventId("event-123");
        event.setUserTaskId("task-123");
        event.setTimestamp(timestamp);
        event.setBpmnProcessId("process-001");
        event.setTaskDefinition("task-def-001");
        event.setUiUriPath("/tasks/form");
        event.setUiUriType(UserTaskUiUriType.WEBPACK_MF_REACT);

        Map<String, String> title = new HashMap<>();
        title.put("en", "Updated Task Title");
        event.setTitle(title);

        return event;
    }
}
