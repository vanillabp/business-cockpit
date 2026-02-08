package io.vanillabp.cockpit.adapter.camunda7.workflow.publishing;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ProcessWorkflowAfterTransactionEvent}.
 */
class ProcessWorkflowAfterTransactionEventTest {

    @Test
    void constructor_setsSource() {
        // Arrange
        Object source = new Object();

        // Act
        ProcessWorkflowAfterTransactionEvent event = new ProcessWorkflowAfterTransactionEvent(source);

        // Assert
        assertThat(event.getSource()).isEqualTo(source);
    }

    @Test
    void event_isApplicationEvent() {
        // Arrange
        Object source = "test-source";

        // Act
        ProcessWorkflowAfterTransactionEvent event = new ProcessWorkflowAfterTransactionEvent(source);

        // Assert
        assertThat(event).isInstanceOf(ApplicationEvent.class);
    }

    @Test
    void event_hasTimestamp() {
        // Arrange
        Object source = new Object();
        long beforeCreation = System.currentTimeMillis();

        // Act
        ProcessWorkflowAfterTransactionEvent event = new ProcessWorkflowAfterTransactionEvent(source);

        // Assert
        assertThat(event.getTimestamp()).isGreaterThanOrEqualTo(beforeCreation);
        assertThat(event.getTimestamp()).isLessThanOrEqualTo(System.currentTimeMillis());
    }
}
