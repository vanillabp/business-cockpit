package io.vanillabp.cockpit.adapter.camunda8.receiver.events;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Camunda8WorkflowCreatedEventTest {

    @Test
    void setAndGetKey_storesAndReturnsValue() {
        // Create event and set key
        final var event = new Camunda8WorkflowCreatedEvent();
        event.setKey(12345L);

        // Verify key is stored
        assertThat(event.getKey()).isEqualTo(12345L);
    }

    @Test
    void setAndGetTimestamp_storesAndReturnsValue() {
        // Create event and set timestamp
        final var event = new Camunda8WorkflowCreatedEvent();
        event.setTimestamp(1704067200000L);

        // Verify timestamp is stored
        assertThat(event.getTimestamp()).isEqualTo(1704067200000L);
    }

    @Test
    void setAndGetBpmnProcessId_storesAndReturnsValue() {
        // Create event and set BPMN process ID
        final var event = new Camunda8WorkflowCreatedEvent();
        event.setBpmnProcessId("order-process");

        // Verify BPMN process ID is stored
        assertThat(event.getBpmnProcessId()).isEqualTo("order-process");
    }

    @Test
    void setAndGetVersion_storesAndReturnsValue() {
        // Create event and set version
        final var event = new Camunda8WorkflowCreatedEvent();
        event.setVersion("v1.0:3");

        // Verify version is stored
        assertThat(event.getVersion()).isEqualTo("v1.0:3");
    }

    @Test
    void setAndGetProcessInstanceKey_storesAndReturnsValue() {
        // Create event and set process instance key
        final var event = new Camunda8WorkflowCreatedEvent();
        event.setProcessInstanceKey(2251799813685255L);

        // Verify process instance key is stored
        assertThat(event.getProcessInstanceKey()).isEqualTo(2251799813685255L);
    }

    @Test
    void setAndGetProcessDefinitionKey_storesAndReturnsValue() {
        // Create event and set process definition key
        final var event = new Camunda8WorkflowCreatedEvent();
        event.setProcessDefinitionKey(2251799813685249L);

        // Verify process definition key is stored
        assertThat(event.getProcessDefinitionKey()).isEqualTo(2251799813685249L);
    }

    @Test
    void setAndGetWorkflowDefinitionVersion_storesAndReturnsValue() {
        // Create event and set workflow definition version
        final var event = new Camunda8WorkflowCreatedEvent();
        event.setWorkflowDefinitionVersion(3);

        // Verify workflow definition version is stored
        assertThat(event.getWorkflowDefinitionVersion()).isEqualTo(3);
    }

    @Test
    void setAndGetTenantId_storesAndReturnsValue() {
        // Create event and set tenant ID
        final var event = new Camunda8WorkflowCreatedEvent();
        event.setTenantId("tenant-1");

        // Verify tenant ID is stored
        assertThat(event.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void setAndGetVariables_storesAndReturnsValue() {
        // Create event and set variables
        final var event = new Camunda8WorkflowCreatedEvent();
        final var variables = Map.of(
                "orderId", (Object) "ORD-123",
                "customerId", "CUST-456",
                "amount", 199.99);
        event.setVariables(variables);

        // Verify variables are stored
        assertThat(event.getVariables())
                .containsEntry("orderId", "ORD-123")
                .containsEntry("customerId", "CUST-456")
                .containsEntry("amount", 199.99);
    }

    @Test
    void allFieldsNull_whenNotSet() {
        // Create event without setting any values
        final var event = new Camunda8WorkflowCreatedEvent();

        // Verify all nullable fields are null
        assertThat(event.getBpmnProcessId()).isNull();
        assertThat(event.getVersion()).isNull();
        assertThat(event.getTenantId()).isNull();
        assertThat(event.getVariables()).isNull();
    }

    @Test
    void numericFields_defaultToZero() {
        // Create event without setting any values
        final var event = new Camunda8WorkflowCreatedEvent();

        // Verify numeric fields default to zero
        assertThat(event.getKey()).isEqualTo(0L);
        assertThat(event.getTimestamp()).isEqualTo(0L);
        assertThat(event.getProcessInstanceKey()).isEqualTo(0L);
        assertThat(event.getProcessDefinitionKey()).isEqualTo(0L);
        assertThat(event.getWorkflowDefinitionVersion()).isEqualTo(0);
    }

}
