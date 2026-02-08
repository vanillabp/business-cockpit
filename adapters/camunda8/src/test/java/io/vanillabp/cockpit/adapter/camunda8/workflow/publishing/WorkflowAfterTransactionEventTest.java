package io.vanillabp.cockpit.adapter.camunda8.workflow.publishing;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowAfterTransactionEventTest {

    @Test
    void constructor_withSourceAndProcessInstanceId_createsInstance() {
        // Create event with source and process instance ID
        final var source = new Object();
        final var event = new WorkflowAfterTransactionEvent(source, "process-123");

        // Should be created successfully
        assertThat(event).isNotNull();
        assertThat(event.getSource()).isSameAs(source);
        assertThat(event.getProcessInstanceId()).isEqualTo("process-123");
    }

    @Test
    void getProcessInstanceId_returnsConfiguredValue() {
        // Create event
        final var event = new WorkflowAfterTransactionEvent(new Object(), "workflow-abc");

        // Verify process instance ID
        assertThat(event.getProcessInstanceId()).isEqualTo("workflow-abc");
    }

    @Test
    void extendsApplicationEvent() {
        // Create instance
        final var event = new WorkflowAfterTransactionEvent(new Object(), "123");

        // Should extend ApplicationEvent
        assertThat(event).isInstanceOf(ApplicationEvent.class);
    }

    @Test
    void getTimestamp_returnsNonZero() {
        // Create event
        final var event = new WorkflowAfterTransactionEvent(new Object(), "123");

        // Timestamp should be set
        assertThat(event.getTimestamp()).isGreaterThan(0);
    }

    @Test
    void constructor_withNullProcessInstanceId_allowsNull() {
        // Create event with null process instance ID
        final var event = new WorkflowAfterTransactionEvent(new Object(), null);

        // Should allow null
        assertThat(event.getProcessInstanceId()).isNull();
    }

}
