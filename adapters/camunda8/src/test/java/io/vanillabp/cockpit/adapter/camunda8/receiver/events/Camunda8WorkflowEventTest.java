package io.vanillabp.cockpit.adapter.camunda8.receiver.events;

import io.vanillabp.spi.cockpit.details.DetailsEvent;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Camunda8WorkflowEventTest {

    @Test
    void setAndGetJobKey_storesAndReturnsValue() {
        final var event = new Camunda8WorkflowEvent();
        event.setJobKey(67890L);
        assertThat(event.getJobKey()).isEqualTo(67890L);
    }

    @Test
    void setAndGetTimestamp_storesAndReturnsValue() {
        final var event = new Camunda8WorkflowEvent();
        final var timestamp = OffsetDateTime.now();
        event.setTimestamp(timestamp);
        assertThat(event.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void setAndGetEvent_storesAndReturnsValue() {
        final var event = new Camunda8WorkflowEvent();
        event.setEvent(DetailsEvent.Event.CREATED);
        assertThat(event.getEvent()).isEqualTo(DetailsEvent.Event.CREATED);
    }

    @Test
    void setAndGetBpmnProcessId_storesAndReturnsValue() {
        final var event = new Camunda8WorkflowEvent();
        event.setBpmnProcessId("order-process");
        assertThat(event.getBpmnProcessId()).isEqualTo("order-process");
    }

    @Test
    void setAndGetTenantId_storesAndReturnsValue() {
        final var event = new Camunda8WorkflowEvent();
        event.setTenantId("tenant-1");
        assertThat(event.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void setAndGetProcessDefinitionVersion_storesAndReturnsValue() {
        final var event = new Camunda8WorkflowEvent();
        event.setProcessDefinitionVersion(3);
        assertThat(event.getProcessDefinitionVersion()).isEqualTo(3);
    }

    @Test
    void setAndGetProcessDefinitionKey_storesAndReturnsValue() {
        final var event = new Camunda8WorkflowEvent();
        event.setProcessDefinitionKey(2251799813685249L);
        assertThat(event.getProcessDefinitionKey()).isEqualTo(2251799813685249L);
    }

    @Test
    void setAndGetProcessInstanceKey_storesAndReturnsValue() {
        final var event = new Camunda8WorkflowEvent();
        event.setProcessInstanceKey(2251799813685255L);
        assertThat(event.getProcessInstanceKey()).isEqualTo(2251799813685255L);
    }

    @Test
    void setAndGetVariables_storesAndReturnsValue() {
        final var event = new Camunda8WorkflowEvent();
        final var variables = Map.of("orderId", (Object) "ORD-123", "customerId", "CUST-456");
        event.setVariables(variables);
        assertThat(event.getVariables())
                .containsEntry("orderId", "ORD-123")
                .containsEntry("customerId", "CUST-456");
    }

    @Test
    void allFieldsNull_whenNotSet() {
        final var event = new Camunda8WorkflowEvent();
        assertThat(event.getTimestamp()).isNull();
        assertThat(event.getEvent()).isNull();
        assertThat(event.getBpmnProcessId()).isNull();
        assertThat(event.getTenantId()).isNull();
        assertThat(event.getVariables()).isNull();
    }

    @Test
    void numericFields_defaultToZero() {
        final var event = new Camunda8WorkflowEvent();
        assertThat(event.getJobKey()).isEqualTo(0L);
        assertThat(event.getProcessDefinitionVersion()).isEqualTo(0);
        assertThat(event.getProcessDefinitionKey()).isEqualTo(0L);
        assertThat(event.getProcessInstanceKey()).isEqualTo(0L);
    }

}
