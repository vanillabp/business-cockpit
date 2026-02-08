package io.vanillabp.cockpit.adapter.camunda8.receiver.events;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Camunda8UserTaskCreatedEventTest {

    @Test
    void setAndGetKey_storesAndReturnsValue() {
        // Create event and set key
        final var event = new Camunda8UserTaskCreatedEvent();
        event.setKey(12345L);

        // Verify key is stored
        assertThat(event.getKey()).isEqualTo(12345L);
    }

    @Test
    void setAndGetTimestamp_storesAndReturnsValue() {
        // Create event and set timestamp
        final var event = new Camunda8UserTaskCreatedEvent();
        event.setTimestamp(1704067200000L);

        // Verify timestamp is stored
        assertThat(event.getTimestamp()).isEqualTo(1704067200000L);
    }

    @Test
    void setAndGetBpmnProcessId_storesAndReturnsValue() {
        // Create event and set BPMN process ID
        final var event = new Camunda8UserTaskCreatedEvent();
        event.setBpmnProcessId("order-process");

        // Verify BPMN process ID is stored
        assertThat(event.getBpmnProcessId()).isEqualTo("order-process");
    }

    @Test
    void setAndGetTenantId_storesAndReturnsValue() {
        // Create event and set tenant ID
        final var event = new Camunda8UserTaskCreatedEvent();
        event.setTenantId("tenant-1");

        // Verify tenant ID is stored
        assertThat(event.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void setAndGetWorkflowDefinitionVersion_storesAndReturnsValue() {
        // Create event and set workflow definition version
        final var event = new Camunda8UserTaskCreatedEvent();
        event.setWorkflowDefinitionVersion(3);

        // Verify workflow definition version is stored
        assertThat(event.getWorkflowDefinitionVersion()).isEqualTo(3);
    }

    @Test
    void setAndGetProcessDefinitionKey_storesAndReturnsValue() {
        // Create event and set process definition key
        final var event = new Camunda8UserTaskCreatedEvent();
        event.setProcessDefinitionKey(2251799813685249L);

        // Verify process definition key is stored
        assertThat(event.getProcessDefinitionKey()).isEqualTo(2251799813685249L);
    }

    @Test
    void setAndGetProcessInstanceKey_storesAndReturnsValue() {
        // Create event and set process instance key
        final var event = new Camunda8UserTaskCreatedEvent();
        event.setProcessInstanceKey(2251799813685255L);

        // Verify process instance key is stored
        assertThat(event.getProcessInstanceKey()).isEqualTo(2251799813685255L);
    }

    @Test
    void setAndGetElementId_storesAndReturnsValue() {
        // Create event and set element ID
        final var event = new Camunda8UserTaskCreatedEvent();
        event.setElementId("Activity_ReviewOrder");

        // Verify element ID is stored
        assertThat(event.getElementId()).isEqualTo("Activity_ReviewOrder");
    }

    @Test
    void setAndGetElementInstanceKey_storesAndReturnsValue() {
        // Create event and set element instance key
        final var event = new Camunda8UserTaskCreatedEvent();
        event.setElementInstanceKey(2251799813685260L);

        // Verify element instance key is stored
        assertThat(event.getElementInstanceKey()).isEqualTo(2251799813685260L);
    }

    @Test
    void setAndGetFormKey_storesAndReturnsValue() {
        // Create event and set form key
        final var event = new Camunda8UserTaskCreatedEvent();
        event.setFormKey("camunda-forms:bpmn:userTaskForm_1");

        // Verify form key is stored
        assertThat(event.getFormKey()).isEqualTo("camunda-forms:bpmn:userTaskForm_1");
    }

    @Test
    void setAndGetAssignee_storesAndReturnsValue() {
        // Create event and set assignee
        final var event = new Camunda8UserTaskCreatedEvent();
        event.setAssignee("john.doe");

        // Verify assignee is stored
        assertThat(event.getAssignee()).isEqualTo("john.doe");
    }

    @Test
    void setAndGetCandidateUsers_storesAndReturnsValue() {
        // Create event and set candidate users
        final var event = new Camunda8UserTaskCreatedEvent();
        final var candidateUsers = List.of("user1", "user2", "user3");
        event.setCandidateUsers(candidateUsers);

        // Verify candidate users are stored
        assertThat(event.getCandidateUsers()).containsExactly("user1", "user2", "user3");
    }

    @Test
    void setAndGetCandidateGroups_storesAndReturnsValue() {
        // Create event and set candidate groups
        final var event = new Camunda8UserTaskCreatedEvent();
        final var candidateGroups = List.of("managers", "supervisors");
        event.setCandidateGroups(candidateGroups);

        // Verify candidate groups are stored
        assertThat(event.getCandidateGroups()).containsExactly("managers", "supervisors");
    }

    @Test
    void setAndGetDueDate_storesAndReturnsValue() {
        // Create event and set due date
        final var event = new Camunda8UserTaskCreatedEvent();
        final var dueDate = OffsetDateTime.now().plusDays(7);
        event.setDueDate(dueDate);

        // Verify due date is stored
        assertThat(event.getDueDate()).isEqualTo(dueDate);
    }

    @Test
    void setAndGetFollowUpDate_storesAndReturnsValue() {
        // Create event and set follow-up date
        final var event = new Camunda8UserTaskCreatedEvent();
        final var followUpDate = OffsetDateTime.now().plusDays(3);
        event.setFollowUpDate(followUpDate);

        // Verify follow-up date is stored
        assertThat(event.getFollowUpDate()).isEqualTo(followUpDate);
    }

    @Test
    void setAndGetVariables_storesAndReturnsValue() {
        // Create event and set variables
        final var event = new Camunda8UserTaskCreatedEvent();
        final var variables = Map.of("orderId", (Object) "ORD-123", "amount", 99.99);
        event.setVariables(variables);

        // Verify variables are stored
        assertThat(event.getVariables())
                .containsEntry("orderId", "ORD-123")
                .containsEntry("amount", 99.99);
    }

    @Test
    void allFieldsNull_whenNotSet() {
        // Create event without setting any values
        final var event = new Camunda8UserTaskCreatedEvent();

        // Verify all nullable fields are null
        assertThat(event.getBpmnProcessId()).isNull();
        assertThat(event.getTenantId()).isNull();
        assertThat(event.getElementId()).isNull();
        assertThat(event.getFormKey()).isNull();
        assertThat(event.getAssignee()).isNull();
        assertThat(event.getCandidateUsers()).isNull();
        assertThat(event.getCandidateGroups()).isNull();
        assertThat(event.getDueDate()).isNull();
        assertThat(event.getFollowUpDate()).isNull();
        assertThat(event.getVariables()).isNull();
    }

}
