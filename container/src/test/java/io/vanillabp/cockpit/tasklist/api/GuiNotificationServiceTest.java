package io.vanillabp.cockpit.tasklist.api;

import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.tasklist.UserTaskChangedNotification;
import io.vanillabp.cockpit.util.events.NotificationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link GuiNotificationService} (tasklist).
 */
@ExtendWith(MockitoExtension.class)
class GuiNotificationServiceTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private GuiNotificationService service;

    @Captor
    private ArgumentCaptor<GuiEvent> eventCaptor;

    @Test
    void updateClients_withInsertNotification_publishesGuiEvent() {
        // Arrange
        UserTaskChangedNotification notification = new UserTaskChangedNotification(
                NotificationEvent.Type.INSERT,
                "task-123",
                List.of("admin"));

        // Act
        service.updateClients(notification);

        // Assert
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        GuiEvent event = eventCaptor.getValue();
        assertThat(event).isNotNull();
        assertThat(event.getTargetGroups()).containsExactly("admin");
    }

    @Test
    void updateClients_withUpdateNotification_publishesGuiEvent() {
        // Arrange
        UserTaskChangedNotification notification = new UserTaskChangedNotification(
                NotificationEvent.Type.UPDATE,
                "task-456",
                List.of("user", "editor"));

        // Act
        service.updateClients(notification);

        // Assert
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        GuiEvent event = eventCaptor.getValue();
        assertThat(event).isNotNull();
        assertThat(event.getTargetGroups()).containsExactlyInAnyOrder("user", "editor");
    }

    @Test
    void updateClients_withDeleteNotification_publishesGuiEvent() {
        // Arrange
        UserTaskChangedNotification notification = new UserTaskChangedNotification(
                NotificationEvent.Type.DELETE,
                "task-789",
                null);

        // Act
        service.updateClients(notification);

        // Assert
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        GuiEvent event = eventCaptor.getValue();
        assertThat(event).isNotNull();
    }

    @Test
    void updateClients_withNullTargetGroups_publishesGuiEvent() {
        // Arrange
        UserTaskChangedNotification notification = new UserTaskChangedNotification(
                NotificationEvent.Type.INSERT,
                "task-123",
                null);

        // Act
        service.updateClients(notification);

        // Assert
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        GuiEvent event = eventCaptor.getValue();
        assertThat(event.getTargetGroups()).isNull();
    }
}
