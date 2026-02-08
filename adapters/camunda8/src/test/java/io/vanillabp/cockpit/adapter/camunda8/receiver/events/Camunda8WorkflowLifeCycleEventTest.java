package io.vanillabp.cockpit.adapter.camunda8.receiver.events;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Camunda8WorkflowLifeCycleEventTest {

    // --- Intent enum ---

    @Test
    void intent_containsCancelledAndCompleted() {
        // Verify Intent enum has expected values
        assertThat(Camunda8WorkflowLifeCycleEvent.Intent.values())
                .containsExactly(
                        Camunda8WorkflowLifeCycleEvent.Intent.CANCELLED,
                        Camunda8WorkflowLifeCycleEvent.Intent.COMPLETED);
    }

    // --- Setters and Getters ---

    @Test
    void setAndGetKey_storesAndReturnsValue() {
        // Create event and set key
        final var event = new Camunda8WorkflowLifeCycleEvent();
        event.setKey(12345L);

        // Verify key is stored
        assertThat(event.getKey()).isEqualTo(12345L);
    }

    @Test
    void setAndGetTimestamp_storesAndReturnsValue() {
        // Create event and set timestamp
        final var event = new Camunda8WorkflowLifeCycleEvent();
        event.setTimestamp(1704067200000L);

        // Verify timestamp is stored
        assertThat(event.getTimestamp()).isEqualTo(1704067200000L);
    }

    @Test
    void setAndGetIntent_completed_storesAndReturnsValue() {
        // Create event and set intent to COMPLETED
        final var event = new Camunda8WorkflowLifeCycleEvent();
        event.setIntent(Camunda8WorkflowLifeCycleEvent.Intent.COMPLETED);

        // Verify intent is stored
        assertThat(event.getIntent()).isEqualTo(Camunda8WorkflowLifeCycleEvent.Intent.COMPLETED);
    }

    @Test
    void setAndGetIntent_cancelled_storesAndReturnsValue() {
        // Create event and set intent to CANCELLED
        final var event = new Camunda8WorkflowLifeCycleEvent();
        event.setIntent(Camunda8WorkflowLifeCycleEvent.Intent.CANCELLED);

        // Verify intent is stored
        assertThat(event.getIntent()).isEqualTo(Camunda8WorkflowLifeCycleEvent.Intent.CANCELLED);
    }

    @Test
    void setAndGetDeleteReason_storesAndReturnsValue() {
        // Create event and set delete reason
        final var event = new Camunda8WorkflowLifeCycleEvent();
        event.setDeleteReason("Process instance terminated by user");

        // Verify delete reason is stored
        assertThat(event.getDeleteReason()).isEqualTo("Process instance terminated by user");
    }

    @Test
    void setAndGetProcessInstanceKey_storesAndReturnsValue() {
        // Create event and set process instance key
        final var event = new Camunda8WorkflowLifeCycleEvent();
        event.setProcessInstanceKey(2251799813685255L);

        // Verify process instance key is stored
        assertThat(event.getProcessInstanceKey()).isEqualTo(2251799813685255L);
    }

    @Test
    void setAndGetBpmnProcessId_storesAndReturnsValue() {
        // Create event and set BPMN process ID
        final var event = new Camunda8WorkflowLifeCycleEvent();
        event.setBpmnProcessId("order-process");

        // Verify BPMN process ID is stored
        assertThat(event.getBpmnProcessId()).isEqualTo("order-process");
    }

    @Test
    void setAndGetWorkflowDefinitionVersion_storesAndReturnsValue() {
        // Create event and set workflow definition version
        final var event = new Camunda8WorkflowLifeCycleEvent();
        event.setWorkflowDefinitionVersion(5);

        // Verify workflow definition version is stored
        assertThat(event.getWorkflowDefinitionVersion()).isEqualTo(5);
    }

    @Test
    void setAndGetProcessDefinitionKey_storesAndReturnsValue() {
        // Create event and set process definition key
        final var event = new Camunda8WorkflowLifeCycleEvent();
        event.setProcessDefinitionKey(2251799813685249L);

        // Verify process definition key is stored
        assertThat(event.getProcessDefinitionKey()).isEqualTo(2251799813685249L);
    }

    @Test
    void setAndGetTenantId_storesAndReturnsValue() {
        // Create event and set tenant ID
        final var event = new Camunda8WorkflowLifeCycleEvent();
        event.setTenantId("tenant-1");

        // Verify tenant ID is stored
        assertThat(event.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void allFieldsNull_whenNotSet() {
        // Create event without setting any values
        final var event = new Camunda8WorkflowLifeCycleEvent();

        // Verify all nullable fields are null
        assertThat(event.getIntent()).isNull();
        assertThat(event.getDeleteReason()).isNull();
        assertThat(event.getBpmnProcessId()).isNull();
        assertThat(event.getTenantId()).isNull();
    }

    @Test
    void numericFields_defaultToZero() {
        // Create event without setting any values
        final var event = new Camunda8WorkflowLifeCycleEvent();

        // Verify numeric fields default to zero
        assertThat(event.getKey()).isEqualTo(0L);
        assertThat(event.getTimestamp()).isEqualTo(0L);
        assertThat(event.getProcessInstanceKey()).isEqualTo(0L);
        assertThat(event.getProcessDefinitionKey()).isEqualTo(0L);
        assertThat(event.getWorkflowDefinitionVersion()).isEqualTo(0);
    }

}
