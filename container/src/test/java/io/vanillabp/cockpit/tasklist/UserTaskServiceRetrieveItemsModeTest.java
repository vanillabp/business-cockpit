package io.vanillabp.cockpit.tasklist;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UserTaskService.RetrieveItemsMode} enum.
 */
class UserTaskServiceRetrieveItemsModeTest {

    @Test
    void values_containsAllModes() {
        // Act
        UserTaskService.RetrieveItemsMode[] values = UserTaskService.RetrieveItemsMode.values();

        // Assert
        assertThat(values).hasSize(5);
        assertThat(values).containsExactlyInAnyOrder(
                UserTaskService.RetrieveItemsMode.All,
                UserTaskService.RetrieveItemsMode.OpenTasks,
                UserTaskService.RetrieveItemsMode.OpenTasksWithoutFollowUp,
                UserTaskService.RetrieveItemsMode.OpenTasksWithFollowUp,
                UserTaskService.RetrieveItemsMode.ClosedTasksOnly
        );
    }

    @Test
    void valueOf_all_returnsCorrectEnum() {
        // Act
        UserTaskService.RetrieveItemsMode mode = UserTaskService.RetrieveItemsMode.valueOf("All");

        // Assert
        assertThat(mode).isEqualTo(UserTaskService.RetrieveItemsMode.All);
    }

    @Test
    void valueOf_openTasks_returnsCorrectEnum() {
        // Act
        UserTaskService.RetrieveItemsMode mode = UserTaskService.RetrieveItemsMode.valueOf("OpenTasks");

        // Assert
        assertThat(mode).isEqualTo(UserTaskService.RetrieveItemsMode.OpenTasks);
    }

    @Test
    void valueOf_openTasksWithoutFollowUp_returnsCorrectEnum() {
        // Act
        UserTaskService.RetrieveItemsMode mode = UserTaskService.RetrieveItemsMode.valueOf("OpenTasksWithoutFollowUp");

        // Assert
        assertThat(mode).isEqualTo(UserTaskService.RetrieveItemsMode.OpenTasksWithoutFollowUp);
    }

    @Test
    void valueOf_openTasksWithFollowUp_returnsCorrectEnum() {
        // Act
        UserTaskService.RetrieveItemsMode mode = UserTaskService.RetrieveItemsMode.valueOf("OpenTasksWithFollowUp");

        // Assert
        assertThat(mode).isEqualTo(UserTaskService.RetrieveItemsMode.OpenTasksWithFollowUp);
    }

    @Test
    void valueOf_closedTasksOnly_returnsCorrectEnum() {
        // Act
        UserTaskService.RetrieveItemsMode mode = UserTaskService.RetrieveItemsMode.valueOf("ClosedTasksOnly");

        // Assert
        assertThat(mode).isEqualTo(UserTaskService.RetrieveItemsMode.ClosedTasksOnly);
    }
}
