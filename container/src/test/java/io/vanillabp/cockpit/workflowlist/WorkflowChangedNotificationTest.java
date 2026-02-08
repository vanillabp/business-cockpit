package io.vanillabp.cockpit.workflowlist;

import io.vanillabp.cockpit.util.events.NotificationEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WorkflowChangedNotification}.
 */
class WorkflowChangedNotificationTest {

    @Test
    void constructor_withValidParameters_createsNotification() {
        // Arrange
        String workflowId = "workflow-123";
        List<String> targetGroups = List.of("admin", "user");

        // Act
        WorkflowChangedNotification notification = new WorkflowChangedNotification(
                NotificationEvent.Type.INSERT,
                workflowId,
                targetGroups);

        // Assert
        assertThat(notification.getWorkflowId()).isEqualTo(workflowId);
        assertThat(notification.getType()).isEqualTo(NotificationEvent.Type.INSERT);
        assertThat(notification.getTargetGroups()).containsExactlyInAnyOrder("admin", "user");
    }

    @Test
    void constructor_withUpdateType_createsUpdateNotification() {
        // Act
        WorkflowChangedNotification notification = new WorkflowChangedNotification(
                NotificationEvent.Type.UPDATE,
                "workflow-123",
                List.of("admin"));

        // Assert
        assertThat(notification.getType()).isEqualTo(NotificationEvent.Type.UPDATE);
    }

    @Test
    void constructor_withDeleteType_createsDeleteNotification() {
        // Act
        WorkflowChangedNotification notification = new WorkflowChangedNotification(
                NotificationEvent.Type.DELETE,
                "workflow-123",
                List.of("admin"));

        // Assert
        assertThat(notification.getType()).isEqualTo(NotificationEvent.Type.DELETE);
    }

    @Test
    void constructor_withNullTargetGroups_createsNotification() {
        // Act
        WorkflowChangedNotification notification = new WorkflowChangedNotification(
                NotificationEvent.Type.INSERT,
                "workflow-123",
                null);

        // Assert
        assertThat(notification.getTargetGroups()).isNull();
    }

    @Test
    void setWorkflowId_updatesWorkflowId() {
        // Arrange
        WorkflowChangedNotification notification = new WorkflowChangedNotification(
                NotificationEvent.Type.INSERT,
                "workflow-123",
                null);

        // Act
        notification.setWorkflowId("workflow-456");

        // Assert
        assertThat(notification.getWorkflowId()).isEqualTo("workflow-456");
    }

    @Test
    void matchesTargetGroups_withNullTargetGroups_returnsTrue() {
        // Arrange
        WorkflowChangedNotification notification = new WorkflowChangedNotification(
                NotificationEvent.Type.INSERT,
                "workflow-123",
                null);

        // Act
        boolean matches = notification.matchesTargetGroups(List.of("admin", "user"));

        // Assert
        assertThat(matches).isTrue();
    }

    @Test
    void matchesTargetGroups_withMatchingGroup_returnsTrue() {
        // Arrange
        WorkflowChangedNotification notification = new WorkflowChangedNotification(
                NotificationEvent.Type.INSERT,
                "workflow-123",
                List.of("admin", "editor"));

        // Act
        boolean matches = notification.matchesTargetGroups(List.of("admin", "user"));

        // Assert
        assertThat(matches).isTrue();
    }

    @Test
    void matchesTargetGroups_withNoMatchingGroup_returnsFalse() {
        // Arrange
        WorkflowChangedNotification notification = new WorkflowChangedNotification(
                NotificationEvent.Type.INSERT,
                "workflow-123",
                List.of("editor", "viewer"));

        // Act
        boolean matches = notification.matchesTargetGroups(List.of("admin", "user"));

        // Assert
        assertThat(matches).isFalse();
    }
}
