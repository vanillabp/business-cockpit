package io.vanillabp.cockpit.adapter.camunda8.receiver.events;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Camunda8UserTaskLifecycleEventTest {

    // --- Intent enum ---

    @Test
    void intent_containsCanceledAndCompleted() {
        // Verify Intent enum has expected values
        assertThat(Camunda8UserTaskLifecycleEvent.Intent.values())
                .containsExactly(
                        Camunda8UserTaskLifecycleEvent.Intent.CANCELED,
                        Camunda8UserTaskLifecycleEvent.Intent.COMPLETED);
    }

    @Test
    void getIntentValueNames_returnsListOfIntentNames() {
        // Get intent value names
        final var intentNames = Camunda8UserTaskLifecycleEvent.getIntentValueNames();

        // Verify list contains expected names
        assertThat(intentNames).containsExactly("CANCELED", "COMPLETED");
    }

    // --- Setters and Getters ---

    @Test
    void setAndGetKey_storesAndReturnsValue() {
        // Create event and set key
        final var event = new Camunda8UserTaskLifecycleEvent();
        event.setKey(12345L);

        // Verify key is stored
        assertThat(event.getKey()).isEqualTo(12345L);
    }

    @Test
    void setAndGetTimestamp_storesAndReturnsValue() {
        // Create event and set timestamp
        final var event = new Camunda8UserTaskLifecycleEvent();
        event.setTimestamp(1704067200000L);

        // Verify timestamp is stored
        assertThat(event.getTimestamp()).isEqualTo(1704067200000L);
    }

    @Test
    void setAndGetIntent_storesAndReturnsValue() {
        // Create event and set intent to COMPLETED
        final var event = new Camunda8UserTaskLifecycleEvent();
        event.setIntent(Camunda8UserTaskLifecycleEvent.Intent.COMPLETED);

        // Verify intent is stored
        assertThat(event.getIntent()).isEqualTo(Camunda8UserTaskLifecycleEvent.Intent.COMPLETED);
    }

    @Test
    void setAndGetIntent_canceled_storesAndReturnsValue() {
        // Create event and set intent to CANCELED
        final var event = new Camunda8UserTaskLifecycleEvent();
        event.setIntent(Camunda8UserTaskLifecycleEvent.Intent.CANCELED);

        // Verify intent is stored
        assertThat(event.getIntent()).isEqualTo(Camunda8UserTaskLifecycleEvent.Intent.CANCELED);
    }

    @Test
    void setAndGetFormKey_storesAndReturnsValue() {
        // Create event and set form key
        final var event = new Camunda8UserTaskLifecycleEvent();
        event.setFormKey("camunda-forms:bpmn:userTaskForm_1");

        // Verify form key is stored
        assertThat(event.getFormKey()).isEqualTo("camunda-forms:bpmn:userTaskForm_1");
    }

    @Test
    void setAndGetElementId_storesAndReturnsValue() {
        // Create event and set element ID
        final var event = new Camunda8UserTaskLifecycleEvent();
        event.setElementId("Activity_ReviewOrder");

        // Verify element ID is stored
        assertThat(event.getElementId()).isEqualTo("Activity_ReviewOrder");
    }

    @Test
    void setAndGetElementInstanceKey_storesAndReturnsValue() {
        // Create event and set element instance key
        final var event = new Camunda8UserTaskLifecycleEvent();
        event.setElementInstanceKey(2251799813685260L);

        // Verify element instance key is stored
        assertThat(event.getElementInstanceKey()).isEqualTo(2251799813685260L);
    }

    @Test
    void setAndGetBpmnProcessId_storesAndReturnsValue() {
        // Create event and set BPMN process ID
        final var event = new Camunda8UserTaskLifecycleEvent();
        event.setBpmnProcessId("order-process");

        // Verify BPMN process ID is stored
        assertThat(event.getBpmnProcessId()).isEqualTo("order-process");
    }

    @Test
    void setAndGetTenantId_storesAndReturnsValue() {
        // Create event and set tenant ID
        final var event = new Camunda8UserTaskLifecycleEvent();
        event.setTenantId("tenant-1");

        // Verify tenant ID is stored
        assertThat(event.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void setAndGetWorkflowDefinitionVersion_storesAndReturnsValue() {
        // Create event and set workflow definition version
        final var event = new Camunda8UserTaskLifecycleEvent();
        event.setWorkflowDefinitionVersion(5);

        // Verify workflow definition version is stored
        assertThat(event.getWorkflowDefinitionVersion()).isEqualTo(5);
    }

    @Test
    void setAndGetProcessInstanceKey_storesAndReturnsValue() {
        // Create event and set process instance key
        final var event = new Camunda8UserTaskLifecycleEvent();
        event.setProcessInstanceKey(2251799813685255L);

        // Verify process instance key is stored
        assertThat(event.getProcessInstanceKey()).isEqualTo(2251799813685255L);
    }

    @Test
    void setAndGetProcessDefinitionKey_storesAndReturnsValue() {
        // Create event and set process definition key
        final var event = new Camunda8UserTaskLifecycleEvent();
        event.setProcessDefinitionKey(2251799813685249L);

        // Verify process definition key is stored
        assertThat(event.getProcessDefinitionKey()).isEqualTo(2251799813685249L);
    }

    @Test
    void allFieldsNull_whenNotSet() {
        // Create event without setting any values
        final var event = new Camunda8UserTaskLifecycleEvent();

        // Verify all nullable fields are null
        assertThat(event.getIntent()).isNull();
        assertThat(event.getFormKey()).isNull();
        assertThat(event.getElementId()).isNull();
        assertThat(event.getBpmnProcessId()).isNull();
        assertThat(event.getTenantId()).isNull();
    }

}
