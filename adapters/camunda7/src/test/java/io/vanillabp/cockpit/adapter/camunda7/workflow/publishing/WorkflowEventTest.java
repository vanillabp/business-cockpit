package io.vanillabp.cockpit.adapter.camunda7.workflow.publishing;

import io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link WorkflowEvent}.
 */
class WorkflowEventTest {

    @Test
    void constructor_setsSourceAndEvent() {
        // Arrange
        Object source = new Object();
        io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent mockEvent =
                mock(WorkflowCreatedEvent.class);

        // Act
        WorkflowEvent workflowEvent = new WorkflowEvent(source, mockEvent);

        // Assert
        assertThat(workflowEvent.getSource()).isEqualTo(source);
        assertThat(workflowEvent.getEvent()).isEqualTo(mockEvent);
    }

    @Test
    void getEvent_returnsWrappedEvent() {
        // Arrange
        Object source = "test-source";
        io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent mockEvent =
                mock(WorkflowCreatedEvent.class);
        WorkflowEvent workflowEvent = new WorkflowEvent(source, mockEvent);

        // Act
        io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent result = workflowEvent.getEvent();

        // Assert
        assertThat(result).isSameAs(mockEvent);
    }

    @Test
    void event_isApplicationEvent() {
        // Arrange
        Object source = new Object();
        io.vanillabp.cockpit.adapter.common.workflow.events.WorkflowEvent mockEvent =
                mock(WorkflowCreatedEvent.class);

        // Act
        WorkflowEvent workflowEvent = new WorkflowEvent(source, mockEvent);

        // Assert
        assertThat(workflowEvent).isInstanceOf(ApplicationEvent.class);
    }
}
