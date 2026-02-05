package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private WorkflowRepository workflows;

    @InjectMocks
    private WorkflowService workflowService;

    private Workflow activeWorkflow;
    private Workflow completedWorkflow;

    @BeforeEach
    void setUp() {
        // Active workflow without endedAt
        activeWorkflow = new Workflow();
        activeWorkflow.setId("wf-1");
        activeWorkflow.setBusinessId("BIZ-001");
        activeWorkflow.setCreatedAt(OffsetDateTime.now());

        // Completed workflow with endedAt
        completedWorkflow = new Workflow();
        completedWorkflow.setId("wf-2");
        completedWorkflow.setBusinessId("BIZ-002");
        completedWorkflow.setCreatedAt(OffsetDateTime.now());
        completedWorkflow.setEndedAt(OffsetDateTime.now());
    }

    // --- createWorkflow ---

    @Test
    void createWorkflow_withValidInput_savesWorkflow() {
        // Create workflow
        workflowService.createWorkflow("wf-1", activeWorkflow);

        // Workflow must be saved to repository
        verify(workflows).save(activeWorkflow);
    }

    @Test
    void createWorkflow_withNullId_throwsIllegalArgumentException() {
        // Null ID must throw IllegalArgumentException
        assertThatThrownBy(() -> workflowService.createWorkflow(null, activeWorkflow))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void createWorkflow_withNullWorkflow_throwsIllegalArgumentException() {
        // Null workflow must throw IllegalArgumentException
        assertThatThrownBy(() -> workflowService.createWorkflow("wf-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    // --- getWorkflow ---

    @Test
    void getWorkflow_withExistingId_returnsWorkflow() {
        // Repository returns the workflow
        when(workflows.findById("wf-1")).thenReturn(Optional.of(activeWorkflow));

        // Retrieve workflow and verify
        final var result = workflowService.getWorkflow("wf-1");

        assertThat(result).isSameAs(activeWorkflow);
    }

    @Test
    void getWorkflow_withNullId_throwsIllegalArgumentException() {
        // Null ID must throw IllegalArgumentException
        assertThatThrownBy(() -> workflowService.getWorkflow(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void getWorkflow_withNonExistingId_throwsIllegalStateException() {
        // Repository finds no workflow
        when(workflows.findById("unknown")).thenReturn(Optional.empty());

        // Expect IllegalStateException for non-existing workflow
        assertThatThrownBy(() -> workflowService.getWorkflow("unknown"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    // --- getAllWorkflows ---

    @Test
    void getAllWorkflows_returnsAllStoredWorkflows() {
        // Repository returns all workflows
        when(workflows.findAll()).thenReturn(List.of(activeWorkflow, completedWorkflow));

        // Query all workflows
        final var result = workflowService.getAllWorkflows();

        assertThat(result).containsExactly(activeWorkflow, completedWorkflow);
    }

    @Test
    void getAllWorkflows_withEmptyRepository_returnsEmptyList() {
        // Repository returns empty list
        when(workflows.findAll()).thenReturn(List.of());

        // Verify empty result
        final var result = workflowService.getAllWorkflows();

        assertThat(result).isEmpty();
    }

    // --- getWorkflows (paginated) ---

    @Test
    void getWorkflows_withPagination_returnsCorrectPage() {
        // Create three workflows for pagination test
        final var wf3 = new Workflow();
        wf3.setId("wf-3");
        when(workflows.findAll()).thenReturn(List.of(activeWorkflow, completedWorkflow, wf3));

        // Query first page with 2 entries
        final var firstPage = workflowService.getWorkflows(0, 2);

        assertThat(firstPage.getPageObjects()).hasSize(2);
        assertThat(firstPage.getNumber()).isEqualTo(0);
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void getWorkflows_secondPage_returnsRemainingItems() {
        // Three workflows for second page
        final var wf3 = new Workflow();
        wf3.setId("wf-3");
        when(workflows.findAll()).thenReturn(List.of(activeWorkflow, completedWorkflow, wf3));

        // Query second page with 2 entries
        final var secondPage = workflowService.getWorkflows(1, 2);

        assertThat(secondPage.getPageObjects()).hasSize(1);
        assertThat(secondPage.getNumber()).isEqualTo(1);
    }

    @Test
    void getWorkflows_withNullPaginationParams_usesDefaults() {
        // Repository returns one workflow
        when(workflows.findAll()).thenReturn(List.of(activeWorkflow));

        // Query without pagination parameters
        final var result = workflowService.getWorkflows(null, null);

        // Default values: pageNumber=0, pageSize=20
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
    }

    @Test
    void getWorkflows_beyondLastPage_returnsEmptyPageObjects() {
        // Repository returns 2 workflows
        when(workflows.findAll()).thenReturn(List.of(activeWorkflow, completedWorkflow));

        // Page number is far beyond the last page
        final var result = workflowService.getWorkflows(100, 20);

        assertThat(result.getPageObjects()).isEmpty();
    }

    @Test
    void getWorkflows_withEmptyResult_returnsEmptyPage() {
        // Repository returns no workflows
        when(workflows.findAll()).thenReturn(List.of());

        // Query empty result set
        final var result = workflowService.getWorkflows(0, 20);

        assertThat(result.getPageObjects()).isEmpty();
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    // --- updateWorkflow ---

    @Test
    void updateWorkflow_withExistingWorkflow_appliesConsumerAndSaves() {
        // Repository finds the workflow
        when(workflows.findById("wf-1")).thenReturn(Optional.of(activeWorkflow));

        // Consumer modifies the workflow
        final var consumerCalled = new AtomicBoolean(false);
        workflowService.updateWorkflow("wf-1", workflow -> {
            workflow.setComment("updated");
            consumerCalled.set(true);
        });

        // Consumer must have been called
        assertThat(consumerCalled).isTrue();
        assertThat(activeWorkflow.getComment()).isEqualTo("updated");

        // Modified workflow must be saved
        verify(workflows).save(activeWorkflow);
    }

    @Test
    void updateWorkflow_withNullId_throwsIllegalArgumentException() {
        // Null ID must throw IllegalArgumentException
        assertThatThrownBy(() -> workflowService.updateWorkflow(null, wf -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void updateWorkflow_withNonExistingWorkflow_throwsIllegalStateException() {
        // Repository finds no workflow
        when(workflows.findById("unknown")).thenReturn(Optional.empty());

        // Expect IllegalStateException
        assertThatThrownBy(() -> workflowService.updateWorkflow("unknown", wf -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    // --- completeWorkflow ---

    @Test
    void completeWorkflow_withExistingWorkflow_appliesConsumerAndSaves() {
        // Repository finds the workflow
        when(workflows.findById("wf-1")).thenReturn(Optional.of(activeWorkflow));

        // Consumer sets endedAt
        workflowService.completeWorkflow("wf-1", wf -> wf.setEndedAt(OffsetDateTime.now()));

        // Workflow must be ended and saved
        assertThat(activeWorkflow.getEndedAt()).isNotNull();
        verify(workflows).save(activeWorkflow);
    }

    @Test
    void completeWorkflow_withNullId_throwsIllegalArgumentException() {
        // Null ID must throw IllegalArgumentException
        assertThatThrownBy(() -> workflowService.completeWorkflow(null, wf -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void completeWorkflow_withNonExistingWorkflow_throwsIllegalStateException() {
        // Repository finds no workflow
        when(workflows.findById("unknown")).thenReturn(Optional.empty());

        // Expect IllegalStateException
        assertThatThrownBy(() -> workflowService.completeWorkflow("unknown", wf -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    // --- cancelWorkflow ---

    @Test
    void cancelWorkflow_withExistingWorkflow_appliesConsumerAndSaves() {
        // Repository finds the workflow
        when(workflows.findById("wf-1")).thenReturn(Optional.of(activeWorkflow));

        // Consumer sets endedAt
        workflowService.cancelWorkflow("wf-1", wf -> wf.setEndedAt(OffsetDateTime.now()));

        // Workflow must be ended and saved
        assertThat(activeWorkflow.getEndedAt()).isNotNull();
        verify(workflows).save(activeWorkflow);
    }

    /**
     * Ensures that cancelWorkflow does not throw an exception when the
     * workflow does not exist (uses ifPresent instead of orElseThrow).
     */
    @Test
    void cancelWorkflow_withNonExistingWorkflow_doesNothing() {
        // Repository finds no workflow
        when(workflows.findById("unknown")).thenReturn(Optional.empty());

        // cancelWorkflow should complete without error
        workflowService.cancelWorkflow("unknown", wf -> wf.setEndedAt(OffsetDateTime.now()));

        // findById was called, but save was not
        verify(workflows).findById("unknown");
    }

    @Test
    void cancelWorkflow_withNullId_throwsIllegalArgumentException() {
        // Null ID must throw IllegalArgumentException
        assertThatThrownBy(() -> workflowService.cancelWorkflow(null, wf -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

}
