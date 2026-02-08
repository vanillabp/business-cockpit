package io.vanillabp.cockpit.adapter.camunda8.receiver.events;

import io.vanillabp.spi.cockpit.details.DetailsEvent;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Camunda8UserTaskEventTest {

    @Test
    void setAndGetUserTaskKey_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        event.setUserTaskKey(12345L);
        assertThat(event.getUserTaskKey()).isEqualTo(12345L);
    }

    @Test
    void setAndGetJobKey_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        event.setJobKey(67890L);
        assertThat(event.getJobKey()).isEqualTo(67890L);
    }

    @Test
    void setAndGetTimestamp_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        final var timestamp = OffsetDateTime.now();
        event.setTimestamp(timestamp);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void setAndGetEvent_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        event.setEvent(DetailsEvent.Event.CREATED);
        assertThat(event.getEvent()).isEqualTo(DetailsEvent.Event.CREATED);
    }

    @Test
    void setAndGetBpmnProcessId_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        event.setBpmnProcessId("order-process");
        assertThat(event.getBpmnProcessId()).isEqualTo("order-process");
    }

    @Test
    void setAndGetTenantId_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        event.setTenantId("tenant-1");
        assertThat(event.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void setAndGetProcessDefinitionVersion_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        event.setProcessDefinitionVersion(3);
        assertThat(event.getProcessDefinitionVersion()).isEqualTo(3);
    }

    @Test
    void setAndGetProcessDefinitionKey_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        event.setProcessDefinitionKey(2251799813685249L);
        assertThat(event.getProcessDefinitionKey()).isEqualTo(2251799813685249L);
    }

    @Test
    void setAndGetProcessInstanceKey_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        event.setProcessInstanceKey(2251799813685255L);
        assertThat(event.getProcessInstanceKey()).isEqualTo(2251799813685255L);
    }

    @Test
    void setAndGetElementId_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        event.setElementId("Activity_ReviewOrder");
        assertThat(event.getElementId()).isEqualTo("Activity_ReviewOrder");
    }

    @Test
    void setAndGetTaskDefinition_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        event.setTaskDefinition("review-task");
        assertThat(event.getTaskDefinition()).isEqualTo("review-task");
    }

    @Test
    void setAndGetAssignee_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        event.setAssignee("john.doe");
        assertThat(event.getAssignee()).isEqualTo("john.doe");
    }

    @Test
    void setAndGetCandidateUsers_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        final var candidateUsers = List.of("user1", "user2");
        event.setCandidateUsers(candidateUsers);
        assertThat(event.getCandidateUsers()).containsExactly("user1", "user2");
    }

    @Test
    void setAndGetCandidateGroups_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        final var candidateGroups = List.of("managers", "supervisors");
        event.setCandidateGroups(candidateGroups);
        assertThat(event.getCandidateGroups()).containsExactly("managers", "supervisors");
    }

    @Test
    void setAndGetDueDate_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        final var dueDate = OffsetDateTime.now().plusDays(7);
        event.setDueDate(dueDate);
        assertThat(event.getDueDate()).isEqualTo(dueDate);
    }

    @Test
    void setAndGetFollowUpDate_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        final var followUpDate = OffsetDateTime.now().plusDays(3);
        event.setFollowUpDate(followUpDate);
        assertThat(event.getFollowUpDate()).isEqualTo(followUpDate);
    }

    @Test
    void setAndGetVariables_storesAndReturnsValue() {
        final var event = new Camunda8UserTaskEvent();
        final var variables = Map.of("orderId", (Object) "ORD-123", "amount", 99.99);
        event.setVariables(variables);
        assertThat(event.getVariables())
                .containsEntry("orderId", "ORD-123")
                .containsEntry("amount", 99.99);
    }

    @Test
    void allFieldsNull_whenNotSet() {
        final var event = new Camunda8UserTaskEvent();
        assertThat(event.getTimestamp()).isNull();
        assertThat(event.getEvent()).isNull();
        assertThat(event.getBpmnProcessId()).isNull();
        assertThat(event.getTenantId()).isNull();
        assertThat(event.getElementId()).isNull();
        assertThat(event.getTaskDefinition()).isNull();
        assertThat(event.getAssignee()).isNull();
        assertThat(event.getCandidateUsers()).isNull();
        assertThat(event.getCandidateGroups()).isNull();
        assertThat(event.getDueDate()).isNull();
        assertThat(event.getFollowUpDate()).isNull();
        assertThat(event.getVariables()).isNull();
    }

}
