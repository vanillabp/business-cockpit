package io.vanillabp.cockpit.tasklist;

import io.vanillabp.cockpit.util.events.NotificationEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UserTaskChangedNotification}.
 */
class UserTaskChangedNotificationTest {

    @Test
    void constructor_withValidParameters_createsNotification() {
        // Arrange
        String userTaskId = "task-123";
        List<String> targetGroups = List.of("admin", "user");

        // Act
        UserTaskChangedNotification notification = new UserTaskChangedNotification(
                NotificationEvent.Type.INSERT,
                userTaskId,
                targetGroups);

        // Assert
        assertThat(notification.getUserTaskId()).isEqualTo(userTaskId);
        assertThat(notification.getType()).isEqualTo(NotificationEvent.Type.INSERT);
        assertThat(notification.getTargetGroups()).containsExactlyInAnyOrder("admin", "user");
    }

    @Test
    void constructor_withUpdateType_createsUpdateNotification() {
        // Act
        UserTaskChangedNotification notification = new UserTaskChangedNotification(
                NotificationEvent.Type.UPDATE,
                "task-123",
                List.of("admin"));

        // Assert
        assertThat(notification.getType()).isEqualTo(NotificationEvent.Type.UPDATE);
    }

    @Test
    void constructor_withDeleteType_createsDeleteNotification() {
        // Act
        UserTaskChangedNotification notification = new UserTaskChangedNotification(
                NotificationEvent.Type.DELETE,
                "task-123",
                List.of("admin"));

        // Assert
        assertThat(notification.getType()).isEqualTo(NotificationEvent.Type.DELETE);
    }

    @Test
    void constructor_withNullTargetGroups_createsNotification() {
        // Act
        UserTaskChangedNotification notification = new UserTaskChangedNotification(
                NotificationEvent.Type.INSERT,
                "task-123",
                null);

        // Assert
        assertThat(notification.getTargetGroups()).isNull();
    }

    @Test
    void setUserTaskId_updatesUserTaskId() {
        // Arrange
        UserTaskChangedNotification notification = new UserTaskChangedNotification(
                NotificationEvent.Type.INSERT,
                "task-123",
                null);

        // Act
        notification.setUserTaskId("task-456");

        // Assert
        assertThat(notification.getUserTaskId()).isEqualTo("task-456");
    }

    @Test
    void matchesTargetGroups_withNullTargetGroups_returnsTrue() {
        // Arrange
        UserTaskChangedNotification notification = new UserTaskChangedNotification(
                NotificationEvent.Type.INSERT,
                "task-123",
                null);

        // Act
        boolean matches = notification.matchesTargetGroups(List.of("admin", "user"));

        // Assert
        assertThat(matches).isTrue();
    }

    @Test
    void matchesTargetGroups_withMatchingGroup_returnsTrue() {
        // Arrange
        UserTaskChangedNotification notification = new UserTaskChangedNotification(
                NotificationEvent.Type.INSERT,
                "task-123",
                List.of("admin", "editor"));

        // Act
        boolean matches = notification.matchesTargetGroups(List.of("admin", "user"));

        // Assert
        assertThat(matches).isTrue();
    }

    @Test
    void matchesTargetGroups_withNoMatchingGroup_returnsFalse() {
        // Arrange
        UserTaskChangedNotification notification = new UserTaskChangedNotification(
                NotificationEvent.Type.INSERT,
                "task-123",
                List.of("editor", "viewer"));

        // Act
        boolean matches = notification.matchesTargetGroups(List.of("admin", "user"));

        // Assert
        assertThat(matches).isFalse();
    }
}
