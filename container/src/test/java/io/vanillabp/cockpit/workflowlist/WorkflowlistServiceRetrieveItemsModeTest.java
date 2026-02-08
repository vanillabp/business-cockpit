package io.vanillabp.cockpit.workflowlist;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WorkflowlistService.RetrieveItemsMode} enum.
 */
class WorkflowlistServiceRetrieveItemsModeTest {

    @Test
    void values_containsAllModes() {
        // Act
        WorkflowlistService.RetrieveItemsMode[] values = WorkflowlistService.RetrieveItemsMode.values();

        // Assert
        assertThat(values).hasSize(3);
        assertThat(values).containsExactlyInAnyOrder(
                WorkflowlistService.RetrieveItemsMode.All,
                WorkflowlistService.RetrieveItemsMode.Active,
                WorkflowlistService.RetrieveItemsMode.Inactive
        );
    }

    @Test
    void valueOf_all_returnsCorrectEnum() {
        // Act
        WorkflowlistService.RetrieveItemsMode mode = WorkflowlistService.RetrieveItemsMode.valueOf("All");

        // Assert
        assertThat(mode).isEqualTo(WorkflowlistService.RetrieveItemsMode.All);
    }

    @Test
    void valueOf_active_returnsCorrectEnum() {
        // Act
        WorkflowlistService.RetrieveItemsMode mode = WorkflowlistService.RetrieveItemsMode.valueOf("Active");

        // Assert
        assertThat(mode).isEqualTo(WorkflowlistService.RetrieveItemsMode.Active);
    }

    @Test
    void valueOf_inactive_returnsCorrectEnum() {
        // Act
        WorkflowlistService.RetrieveItemsMode mode = WorkflowlistService.RetrieveItemsMode.valueOf("Inactive");

        // Assert
        assertThat(mode).isEqualTo(WorkflowlistService.RetrieveItemsMode.Inactive);
    }
}
