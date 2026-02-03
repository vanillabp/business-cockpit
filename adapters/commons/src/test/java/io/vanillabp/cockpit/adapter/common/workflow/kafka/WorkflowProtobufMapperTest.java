package io.vanillabp.cockpit.adapter.common.workflow.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUiUriType;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowProtobufMapperTest {

    private WorkflowProtobufMapper mapper;
    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @BeforeEach
    void setUp() {
        mapper = new WorkflowProtobufMapper(new ObjectMapper());
    }

    @Test
    void mapWorkflowCreatedEvent_mapsRequiredFields() {
        WorkflowCreatedEvent event = createWorkflowCreatedEvent();

        WorkflowCreatedOrUpdatedEvent result = mapper.map(event);

        assertThat(result.getWorkflowId()).isEqualTo("workflow-123");
        assertThat(result.getBpmnProcessId()).isEqualTo("process-001");
        assertThat(result.getWorkflowModuleId()).isEqualTo(WORKFLOW_MODULE_ID);
        assertThat(result.getUiUriPath()).isEqualTo("/workflow/details");
        assertThat(result.getUiUriType()).isEqualTo("EXTERNAL");
        assertThat(result.getUpdated()).isFalse();
    }

    @Test
    void mapWorkflowUpdatedEvent_setsUpdatedToTrue() {
        WorkflowUpdatedEvent event = createWorkflowUpdatedEvent();

        WorkflowCreatedOrUpdatedEvent result = mapper.map(event);

        assertThat(result.getUpdated()).isTrue();
    }

    @Test
    void mapWorkflowCreatedEvent_mapsOptionalFields() {
        WorkflowCreatedEvent event = createWorkflowCreatedEvent();
        event.setBusinessId("business-456");
        event.setInitiator("user@example.com");
        event.setSource("camunda7");
        event.setComment("Test comment");
        event.setBpmnProcessVersion("2.0.0");
        event.setDetailsFulltextSearch("fulltext search");
        event.setAccessibleToUsers(Arrays.asList("user1", "user2"));
        event.setAccessibleToGroups(Arrays.asList("group1", "group2"));

        Map<String, String> title = new HashMap<>();
        title.put("en", "Workflow Title");
        event.setTitle(title);

        Map<String, Object> details = new HashMap<>();
        details.put("key1", "value1");
        event.setDetails(details);

        WorkflowCreatedOrUpdatedEvent result = mapper.map(event);

        assertThat(result.getBusinessId()).isEqualTo("business-456");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getSource()).isEqualTo("camunda7");
        assertThat(result.getTitleMap()).containsEntry("en", "Workflow Title");
        assertThat(result.getComment()).isEqualTo("Test comment");
        assertThat(result.getBpmnProcessVersion()).isEqualTo("2.0.0");
        assertThat(result.hasDetails()).isTrue();
        assertThat(result.getDetailsFulltextSearch()).isEqualTo("fulltext search");
        assertThat(result.getAccessibleToUsersList()).containsExactly("user1", "user2");
        assertThat(result.getAccessibleToGroupsList()).containsExactly("group1", "group2");
    }

    @Test
    void mapWorkflowCompletedEvent_mapsAllFields() {
        WorkflowCompletedEvent event = new WorkflowCompletedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        event.setEventId("event-123");
        event.setWorkflowId("workflow-456");
        event.setTimestamp(timestamp);
        event.setInitiator("user@example.com");
        event.setSource("camunda8");
        event.setComment("Completed");
        event.setBpmnProcessId("process-001");
        event.setBpmnProcessVersion("1.0.0");

        io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCompletedEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getWorkflowId()).isEqualTo("workflow-456");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getSource()).isEqualTo("camunda8");
        assertThat(result.getComment()).isEqualTo("Completed");
        assertThat(result.getBpmnProcessId()).isEqualTo("process-001");
        assertThat(result.getBpmnProcessVersion()).isEqualTo("1.0.0");
    }

    @Test
    void mapWorkflowCancelledEvent_mapsAllFields() {
        WorkflowCancelledEvent event = new WorkflowCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        event.setEventId("event-123");
        event.setWorkflowId("workflow-456");
        event.setTimestamp(timestamp);
        event.setInitiator("user@example.com");
        event.setSource("camunda7");
        event.setComment("Cancelled");
        event.setBpmnProcessId("process-001");
        event.setBpmnProcessVersion("1.0.0");

        io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCancelledEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getWorkflowId()).isEqualTo("workflow-456");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getSource()).isEqualTo("camunda7");
        assertThat(result.getComment()).isEqualTo("Cancelled");
        assertThat(result.getBpmnProcessId()).isEqualTo("process-001");
        assertThat(result.getBpmnProcessVersion()).isEqualTo("1.0.0");
    }

    @Test
    void mapTimestamp_mapsCorrectly() {
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 45, 123456789, ZoneOffset.UTC);

        com.google.protobuf.Timestamp result = mapper.mapTimestamp(timestamp);

        assertThat(result.getSeconds()).isEqualTo(timestamp.toInstant().getEpochSecond());
        assertThat(result.getNanos()).isEqualTo(123456789);
    }

    @Test
    void mapDetailsToProtobuf_mapsCorrectly() {
        Map<String, Object> details = new HashMap<>();
        details.put("stringKey", "stringValue");
        details.put("numericKey", 42);

        var result = mapper.mapDetailsToProtobuf(details);

        assertThat(result.getDetailsMap()).containsKey("stringKey");
        assertThat(result.getDetailsMap()).containsKey("numericKey");
    }

    private WorkflowCreatedEvent createWorkflowCreatedEvent() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        event.setEventId("event-123");
        event.setWorkflowId("workflow-123");
        event.setTimestamp(timestamp);
        event.setBpmnProcessId("process-001");
        event.setUiUriPath("/workflow/details");
        event.setUiUriType(WorkflowUiUriType.EXTERNAL);

        return event;
    }

    private WorkflowUpdatedEvent createWorkflowUpdatedEvent() {
        WorkflowUpdatedEvent event = new WorkflowUpdatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, ZoneOffset.UTC);

        event.setEventId("event-123");
        event.setWorkflowId("workflow-123");
        event.setTimestamp(timestamp);
        event.setBpmnProcessId("process-001");
        event.setUiUriPath("/workflow/details");
        event.setUiUriType(WorkflowUiUriType.WEBPACK_MF_REACT);

        return event;
    }
}
