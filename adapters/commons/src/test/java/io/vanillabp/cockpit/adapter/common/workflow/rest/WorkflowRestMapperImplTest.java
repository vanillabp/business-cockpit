package io.vanillabp.cockpit.adapter.common.workflow.rest;

import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCancelledEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCompletedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUiUriType;
import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1_1.UiUriType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowRestMapperImplTest {

    private WorkflowRestMapperImpl mapper;
    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @BeforeEach
    void setUp() {
        mapper = new WorkflowRestMapperImpl();
    }

    @Test
    void mapWorkflowCancelledEvent_withNull_returnsNull() {
        assertThat(mapper.map((WorkflowCancelledEvent) null)).isNull();
    }

    @Test
    void mapWorkflowCancelledEvent_mapsAllFields() {
        WorkflowCancelledEvent event = new WorkflowCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.now();

        event.setEventId("event-123");
        event.setWorkflowId("workflow-456");
        event.setInitiator("user@example.com");
        event.setTimestamp(timestamp);
        event.setSource("camunda7");
        event.setComment("Cancelled");
        event.setBpmnProcessId("process-001");
        event.setBpmnProcessVersion("1.0.0");

        io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCancelledEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getWorkflowId()).isEqualTo("workflow-456");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getSource()).isEqualTo("camunda7");
        assertThat(result.getComment()).isEqualTo("Cancelled");
        assertThat(result.getBpmnProcessId()).isEqualTo("process-001");
        assertThat(result.getBpmnProcessVersion()).isEqualTo("1.0.0");
    }

    @Test
    void mapWorkflowCompletedEvent_withNull_returnsNull() {
        assertThat(mapper.map((WorkflowCompletedEvent) null)).isNull();
    }

    @Test
    void mapWorkflowCompletedEvent_mapsAllFields() {
        WorkflowCompletedEvent event = new WorkflowCompletedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.now();

        event.setEventId("event-123");
        event.setWorkflowId("workflow-456");
        event.setBusinessId("business-789");
        event.setInitiator("user@example.com");
        event.setTimestamp(timestamp);
        event.setSource("camunda8");
        event.setComment("Completed");
        event.setBpmnProcessId("process-001");
        event.setBpmnProcessVersion("1.0.0");
        event.setUiUriPath("/workflow/details");
        event.setUiUriType(WorkflowUiUriType.WEBPACK_MF_REACT);
        event.setDetailsFulltextSearch("fulltext");
        event.setAccessibleToUsers(Arrays.asList("user1", "user2"));
        event.setAccessibleToGroups(Arrays.asList("group1", "group2"));

        Map<String, String> title = new HashMap<>();
        title.put("en", "Workflow Title");
        event.setTitle(title);

        Map<String, Object> details = new HashMap<>();
        details.put("key1", "value1");
        event.setDetails(details);

        io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCompletedEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getWorkflowId()).isEqualTo("workflow-456");
        assertThat(result.getBusinessId()).isEqualTo("business-789");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getSource()).isEqualTo("camunda8");
        assertThat(result.getWorkflowModuleId()).isEqualTo(WORKFLOW_MODULE_ID);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getComment()).isEqualTo("Completed");
        assertThat(result.getBpmnProcessId()).isEqualTo("process-001");
        assertThat(result.getBpmnProcessVersion()).isEqualTo("1.0.0");
        assertThat(result.getUiUriPath()).isEqualTo("/workflow/details");
        assertThat(result.getUiUriType()).isEqualTo(UiUriType.WEBPACK_MF_REACT);
        assertThat(result.getDetails()).isEqualTo(details);
        assertThat(result.getDetailsFulltextSearch()).isEqualTo("fulltext");
        assertThat(result.getAccessibleToUsers()).containsExactly("user1", "user2");
        assertThat(result.getAccessibleToGroups()).containsExactly("group1", "group2");
        assertThat(result.getUpdated()).isTrue();
    }

    @Test
    void mapWorkflowCreatedEvent_withNull_returnsNull() {
        assertThat(mapper.map((WorkflowCreatedEvent) null)).isNull();
    }

    @Test
    void mapWorkflowCreatedEvent_mapsAllFields() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.now();

        event.setEventId("event-123");
        event.setWorkflowId("workflow-456");
        event.setBusinessId("business-789");
        event.setInitiator("user@example.com");
        event.setTimestamp(timestamp);
        event.setSource("camunda7");
        event.setComment("Created");
        event.setBpmnProcessId("process-001");
        event.setBpmnProcessVersion("1.0.0");
        event.setUiUriPath("/workflow/details");
        event.setUiUriType(WorkflowUiUriType.EXTERNAL);
        event.setDetailsFulltextSearch("fulltext");
        event.setAccessibleToUsers(Arrays.asList("user1"));
        event.setAccessibleToGroups(Arrays.asList("group1"));

        Map<String, String> title = new HashMap<>();
        title.put("en", "Workflow Title");
        event.setTitle(title);

        Map<String, Object> details = new HashMap<>();
        details.put("key1", "value1");
        event.setDetails(details);

        io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getWorkflowId()).isEqualTo("workflow-456");
        assertThat(result.getBusinessId()).isEqualTo("business-789");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getSource()).isEqualTo("camunda7");
        assertThat(result.getWorkflowModuleId()).isEqualTo(WORKFLOW_MODULE_ID);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getComment()).isEqualTo("Created");
        assertThat(result.getBpmnProcessId()).isEqualTo("process-001");
        assertThat(result.getBpmnProcessVersion()).isEqualTo("1.0.0");
        assertThat(result.getUiUriPath()).isEqualTo("/workflow/details");
        assertThat(result.getUiUriType()).isEqualTo(UiUriType.EXTERNAL);
        assertThat(result.getDetails()).isEqualTo(details);
        assertThat(result.getDetailsFulltextSearch()).isEqualTo("fulltext");
        assertThat(result.getAccessibleToUsers()).containsExactly("user1");
        assertThat(result.getAccessibleToGroups()).containsExactly("group1");
        assertThat(result.getUpdated()).isFalse();
    }

    @Test
    void mapWorkflowUpdatedEvent_withNull_returnsNull() {
        assertThat(mapper.map((WorkflowUpdatedEvent) null)).isNull();
    }

    @Test
    void mapWorkflowUpdatedEvent_mapsAllFields() {
        WorkflowUpdatedEvent event = new WorkflowUpdatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        OffsetDateTime timestamp = OffsetDateTime.now();

        event.setEventId("event-123");
        event.setWorkflowId("workflow-456");
        event.setBusinessId("business-789");
        event.setInitiator("user@example.com");
        event.setTimestamp(timestamp);
        event.setSource("camunda8");
        event.setComment("Updated");
        event.setBpmnProcessId("process-001");
        event.setBpmnProcessVersion("1.0.0");
        event.setUiUriPath("/workflow/details");
        event.setUiUriType(WorkflowUiUriType.WEBPACK_MF_REACT);
        event.setDetailsFulltextSearch("fulltext");
        event.setAccessibleToUsers(Arrays.asList("user1", "user2"));
        event.setAccessibleToGroups(Arrays.asList("group1"));

        Map<String, String> title = new HashMap<>();
        title.put("en", "Updated Workflow");
        event.setTitle(title);

        Map<String, Object> details = new HashMap<>();
        details.put("key1", "value1");
        event.setDetails(details);

        io.vanillabp.cockpit.bpms.api.v1_1.WorkflowUpdatedEvent result = mapper.map(event);

        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getWorkflowId()).isEqualTo("workflow-456");
        assertThat(result.getBusinessId()).isEqualTo("business-789");
        assertThat(result.getInitiator()).isEqualTo("user@example.com");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);
        assertThat(result.getSource()).isEqualTo("camunda8");
        assertThat(result.getWorkflowModuleId()).isEqualTo(WORKFLOW_MODULE_ID);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getComment()).isEqualTo("Updated");
        assertThat(result.getBpmnProcessId()).isEqualTo("process-001");
        assertThat(result.getBpmnProcessVersion()).isEqualTo("1.0.0");
        assertThat(result.getUiUriPath()).isEqualTo("/workflow/details");
        assertThat(result.getUiUriType()).isEqualTo(UiUriType.WEBPACK_MF_REACT);
        assertThat(result.getDetails()).isEqualTo(details);
        assertThat(result.getDetailsFulltextSearch()).isEqualTo("fulltext");
        assertThat(result.getAccessibleToUsers()).containsExactly("user1", "user2");
        assertThat(result.getAccessibleToGroups()).containsExactly("group1");
        assertThat(result.getUpdated()).isTrue();
    }

    @Test
    void mapWorkflowCompletedEvent_withNullUiUriType_doesNotFail() {
        WorkflowCompletedEvent event = new WorkflowCompletedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setEventId("event-123");
        event.setUiUriType(null);

        io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCompletedEvent result = mapper.map(event);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("event-123");
        assertThat(result.getUiUriType()).isNull();
    }

    @Test
    void mapWorkflowCreatedEvent_withNullUiUriType_mapsToNull() {
        WorkflowCreatedEvent event = new WorkflowCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        event.setEventId("event-123");
        event.setUiUriType(null);

        io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent result = mapper.map(event);

        assertThat(result.getUiUriType()).isNull();
    }
}
