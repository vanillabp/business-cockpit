package io.vanillabp.cockpit.adapter.camunda7.usertask.publishing;

import io.vanillabp.cockpit.adapter.common.usertask.UserTaskPublishing;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Camunda7UserTaskEventPublisher}.
 */
@ExtendWith(MockitoExtension.class)
class Camunda7UserTaskEventPublisherTest {

    @Mock
    private UserTaskPublishing userTaskPublishing;

    private Camunda7UserTaskEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new Camunda7UserTaskEventPublisher(userTaskPublishing);
    }

    @Test
    void addEvent_collectsEvents() {
        // Arrange
        UserTaskCreatedEvent mockEvent = mock(UserTaskCreatedEvent.class);
        UserTaskEvent userTaskEvent = new UserTaskEvent(this, mockEvent);

        // Act
        publisher.addEvent(userTaskEvent);

        // Assert - no exception means success, events are collected in ThreadLocal
        verifyNoInteractions(userTaskPublishing);
    }

    @Test
    void handle_publishesCollectedEventsAfterCommit() {
        // Arrange
        UserTaskCreatedEvent mockEvent1 = mock(UserTaskCreatedEvent.class);
        UserTaskCreatedEvent mockEvent2 = mock(UserTaskCreatedEvent.class);
        UserTaskEvent userTaskEvent1 = new UserTaskEvent(this, mockEvent1);
        UserTaskEvent userTaskEvent2 = new UserTaskEvent(this, mockEvent2);
        ProcessUserTaskEvent triggerEvent = new ProcessUserTaskEvent(this);

        publisher.addEvent(userTaskEvent1);
        publisher.addEvent(userTaskEvent2);

        // Act
        publisher.handle(triggerEvent);

        // Assert
        verify(userTaskPublishing).publish(mockEvent1);
        verify(userTaskPublishing).publish(mockEvent2);
    }

    @Test
    void handle_clearsEventsAfterPublishing() {
        // Arrange
        UserTaskCreatedEvent mockEvent = mock(UserTaskCreatedEvent.class);
        UserTaskEvent userTaskEvent = new UserTaskEvent(this, mockEvent);
        ProcessUserTaskEvent triggerEvent = new ProcessUserTaskEvent(this);

        publisher.addEvent(userTaskEvent);
        publisher.handle(triggerEvent);

        // Act - handle again without adding new events
        publisher.handle(triggerEvent);

        // Assert - should only have published once (events were cleared)
        verify(userTaskPublishing, times(1)).publish(mockEvent);
    }

    @Test
    void handleRollback_clearsEventsWithoutPublishing() {
        // Arrange
        UserTaskCreatedEvent mockEvent = mock(UserTaskCreatedEvent.class);
        UserTaskEvent userTaskEvent = new UserTaskEvent(this, mockEvent);
        ProcessUserTaskEvent triggerEvent = new ProcessUserTaskEvent(this);

        publisher.addEvent(userTaskEvent);

        // Act
        publisher.handleRollback(triggerEvent);

        // Assert - nothing should be published
        verifyNoInteractions(userTaskPublishing);
    }

    @Test
    void handleRollback_allowsNewEventsAfterClear() {
        // Arrange
        UserTaskCreatedEvent mockEvent1 = mock(UserTaskCreatedEvent.class);
        UserTaskCreatedEvent mockEvent2 = mock(UserTaskCreatedEvent.class);
        UserTaskEvent userTaskEvent1 = new UserTaskEvent(this, mockEvent1);
        UserTaskEvent userTaskEvent2 = new UserTaskEvent(this, mockEvent2);
        ProcessUserTaskEvent triggerEvent = new ProcessUserTaskEvent(this);

        publisher.addEvent(userTaskEvent1);
        publisher.handleRollback(triggerEvent);

        publisher.addEvent(userTaskEvent2);

        // Act
        publisher.handle(triggerEvent);

        // Assert - only the second event should be published
        verify(userTaskPublishing, never()).publish(mockEvent1);
        verify(userTaskPublishing).publish(mockEvent2);
    }

    @Test
    void handle_clearsEventsEvenOnException() {
        // Arrange
        UserTaskCreatedEvent mockEvent = mock(UserTaskCreatedEvent.class);
        UserTaskEvent userTaskEvent = new UserTaskEvent(this, mockEvent);
        ProcessUserTaskEvent triggerEvent = new ProcessUserTaskEvent(this);

        doThrow(new RuntimeException("Test exception")).when(userTaskPublishing).publish(mockEvent);

        publisher.addEvent(userTaskEvent);

        // Act
        try {
            publisher.handle(triggerEvent);
        } catch (RuntimeException e) {
            // Expected
        }

        // Assert - adding a new event and handling should not re-publish the failed event
        publisher.handle(triggerEvent);
        verify(userTaskPublishing, times(1)).publish(mockEvent);
    }
}
