package io.vanillabp.cockpit.adapter.camunda7.usertask;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Camunda7UserTaskEventHandler}.
 *
 * Note: The Camunda7UserTaskEventHandler uses a static map for task handlers,
 * and has a potential NPE when no handler matches (the orElseGet lambda tries
 * to access connectableFound[0] which is null when no filter match occurs).
 */
@ExtendWith(MockitoExtension.class)
class Camunda7UserTaskEventHandlerTest {

    @Mock
    private Camunda7UserTaskHandler taskHandler;

    private Camunda7UserTaskEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new Camunda7UserTaskEventHandler();
    }

    @Test
    void addTaskHandler_registersHandlerWithoutException() {
        // Arrange
        Camunda7Connectable connectable = new Camunda7Connectable(
                "test-process-unique-handler", "1.0", "UserTask_1", "reviewTask"
        );

        // Act
        handler.addTaskHandler(connectable, taskHandler);

        // Assert - no exception means success
        verifyNoInteractions(taskHandler);
    }

    @Test
    void addTaskHandler_canAddMultipleHandlers() {
        // Arrange
        Camunda7Connectable connectable1 = new Camunda7Connectable(
                "process-1", "1.0", "Task_1", "task1"
        );
        Camunda7Connectable connectable2 = new Camunda7Connectable(
                "process-2", "1.0", "Task_2", "task2"
        );
        Camunda7UserTaskHandler handler2 = mock(Camunda7UserTaskHandler.class);

        // Act
        handler.addTaskHandler(connectable1, taskHandler);
        handler.addTaskHandler(connectable2, handler2);

        // Assert - no exception means success
        verifyNoInteractions(taskHandler);
        verifyNoInteractions(handler2);
    }
}
