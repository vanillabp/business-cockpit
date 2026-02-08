package io.vanillabp.cockpit.adapter.camunda7.workflow.publishing;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WorkflowAfterTransactionEvent}.
 */
class WorkflowAfterTransactionEventTest {

    @Test
    void constructor_setsSourceAndProcessInstanceId() {
        // Arrange
        Object source = new Object();
        String processInstanceId = "123456";

        // Act
        WorkflowAfterTransactionEvent event = new WorkflowAfterTransactionEvent(source, processInstanceId);

        // Assert
        assertThat(event.getSource()).isEqualTo(source);
        assertThat(event.getProcessInstanceId()).isEqualTo(processInstanceId);
    }

    @Test
    void getProcessInstanceId_returnsCorrectValue() {
        // Arrange
        Object source = "test-source";
        String processInstanceId = "PI-789";
        WorkflowAfterTransactionEvent event = new WorkflowAfterTransactionEvent(source, processInstanceId);

        // Act
        String result = event.getProcessInstanceId();

        // Assert
        assertThat(result).isEqualTo(processInstanceId);
    }

    @Test
    void event_isApplicationEvent() {
        // Arrange
        Object source = new Object();

        // Act
        WorkflowAfterTransactionEvent event = new WorkflowAfterTransactionEvent(source, "test-id");

        // Assert
        assertThat(event).isInstanceOf(ApplicationEvent.class);
    }

    @Test
    void event_hasTimestamp() {
        // Arrange
        Object source = new Object();
        long beforeCreation = System.currentTimeMillis();

        // Act
        WorkflowAfterTransactionEvent event = new WorkflowAfterTransactionEvent(source, "test-id");

        // Assert
        assertThat(event.getTimestamp()).isGreaterThanOrEqualTo(beforeCreation);
        assertThat(event.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    }

    @Test
    void constructor_handlesNullProcessInstanceId() {
        // Arrange
        Object source = new Object();

        // Act
        WorkflowAfterTransactionEvent event = new WorkflowAfterTransactionEvent(source, null);

        // Assert
        assertThat(event.getProcessInstanceId()).isNull();
    }
}
