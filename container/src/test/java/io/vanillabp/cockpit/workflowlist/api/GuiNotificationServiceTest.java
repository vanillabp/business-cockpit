package io.vanillabp.cockpit.workflowlist.api;

import io.vanillabp.cockpit.gui.api.v1.GuiEvent;
import io.vanillabp.cockpit.util.events.NotificationEvent;
import io.vanillabp.cockpit.workflowlist.WorkflowChangedNotification;
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
 * Unit tests for {@link GuiNotificationService} (workflowlist).
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
        WorkflowChangedNotification notification = new WorkflowChangedNotification(
                NotificationEvent.Type.INSERT,
                "workflow-123",
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
        WorkflowChangedNotification notification = new WorkflowChangedNotification(
                NotificationEvent.Type.UPDATE,
                "workflow-456",
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
        WorkflowChangedNotification notification = new WorkflowChangedNotification(
                NotificationEvent.Type.DELETE,
                "workflow-789",
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
        WorkflowChangedNotification notification = new WorkflowChangedNotification(
                NotificationEvent.Type.INSERT,
                "workflow-123",
                null);

        // Act
        service.updateClients(notification);

        // Assert
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        GuiEvent event = eventCaptor.getValue();
        assertThat(event.getTargetGroups()).isNull();
    }
}
