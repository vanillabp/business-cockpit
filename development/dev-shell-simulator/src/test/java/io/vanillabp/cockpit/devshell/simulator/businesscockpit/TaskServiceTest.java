package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.TaskService.RetrieveMode;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Page;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTaskRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private UserTaskRepository userTasks;

    @InjectMocks
    private TaskService taskService;

    private UserTask openTask;
    private UserTask closedTask;

    @BeforeEach
    void setUp() {
        // Open task without endedAt
        openTask = new UserTask();
        openTask.setId("task-1");
        openTask.setWorkflowId("wf-1");
        openTask.setCreatedAt(OffsetDateTime.now());

        // Completed task with endedAt
        closedTask = new UserTask();
        closedTask.setId("task-2");
        closedTask.setWorkflowId("wf-1");
        closedTask.setCreatedAt(OffsetDateTime.now());
        closedTask.setEndedAt(OffsetDateTime.now());
    }

    // --- createTask ---

    @Test
    void createTask_withValidInput_savesTask() {
        // Create task with valid input
        taskService.createTask("task-1", openTask);

        // Verify task is saved to repository
        verify(userTasks).save(openTask);
    }

    @Test
    void createTask_withNullId_throwsIllegalArgumentException() {
        // Null ID must throw IllegalArgumentException
        assertThatThrownBy(() -> taskService.createTask(null, openTask))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void createTask_withNullTask_throwsIllegalArgumentException() {
        // Null task must throw IllegalArgumentException
        assertThatThrownBy(() -> taskService.createTask("task-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    // --- getUserTask ---

    @Test
    void getUserTask_withExistingId_returnsTask() {
        // Repository returns the task
        when(userTasks.findById("task-1")).thenReturn(Optional.of(openTask));

        // Retrieve task and verify
        final var result = taskService.getUserTask("task-1");

        assertThat(result).isSameAs(openTask);
    }

    @Test
    void getUserTask_withNullId_throwsIllegalArgumentException() {
        // Null ID must throw IllegalArgumentException
        assertThatThrownBy(() -> taskService.getUserTask(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void getUserTask_withNonExistingId_throwsIllegalStateException() {
        // Repository finds no task
        when(userTasks.findById("unknown")).thenReturn(Optional.empty());

        // Expect IllegalStateException for non-existing task
        assertThatThrownBy(() -> taskService.getUserTask("unknown"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    // --- getUserTasks with RetrieveMode ---

    @Test
    void getUserTasks_withAllMode_returnsAllTasks() {
        // Repository returns all tasks
        when(userTasks.findAll()).thenReturn(List.of(openTask, closedTask));

        // Query all tasks in ALL mode
        final var result = taskService.getUserTasks(RetrieveMode.ALL, 0, 20);

        assertThat(result.getPageObjects()).containsExactly(openTask, closedTask);
    }

    @Test
    void getUserTasks_withOpenTasksMode_returnsOnlyOpenTasks() {
        // Repository returns only open tasks
        when(userTasks.findByEndedAtIsNull()).thenReturn(List.of(openTask));

        // Query only open tasks in OPENTASKS mode
        final var result = taskService.getUserTasks(RetrieveMode.OPENTASKS, 0, 20);

        assertThat(result.getPageObjects()).containsExactly(openTask);
    }

    @Test
    void getUserTasks_withClosedTasksMode_returnsOnlyClosedTasks() {
        // Repository returns only closed tasks
        when(userTasks.findByEndedAtIsNotNull()).thenReturn(List.of(closedTask));

        // Query only closed tasks in CLOSEDTASKSONLY mode
        final var result = taskService.getUserTasks(RetrieveMode.CLOSEDTASKSONLY, 0, 20);

        assertThat(result.getPageObjects()).containsExactly(closedTask);
    }

    /**
     * Verifies that retrieve modes OPENTASKSWITHOUTFOLLOWUP and
     * OPENTASKSWITHFOLLOWUP fall back to the default branch (findAll).
     */
    @Test
    void getUserTasks_withFallbackModes_returnsAllTasks() {
        // Repository returns all tasks (default branch)
        when(userTasks.findAll()).thenReturn(List.of(openTask, closedTask));

        // OPENTASKSWITHOUTFOLLOWUP falls back to default branch
        final var resultWithout = taskService.getUserTasks(RetrieveMode.OPENTASKSWITHOUTFOLLOWUP, 0, 20);
        assertThat(resultWithout.getPageObjects()).hasSize(2);

        // OPENTASKSWITHFOLLOWUP also falls back to default branch
        final var resultWith = taskService.getUserTasks(RetrieveMode.OPENTASKSWITHFOLLOWUP, 0, 20);
        assertThat(resultWith.getPageObjects()).hasSize(2);
    }

    // --- Pagination ---

    @Test
    void getUserTasks_withPagination_returnsCorrectPage() {
        // Create three tasks to test pagination with page size 2
        final var task3 = new UserTask();
        task3.setId("task-3");
        when(userTasks.findAll()).thenReturn(List.of(openTask, closedTask, task3));

        // Query first page with 2 entries
        final var firstPage = taskService.getUserTasks(RetrieveMode.ALL, 0, 2);
        assertThat(firstPage.getPageObjects()).hasSize(2);
        assertThat(firstPage.getNumber()).isEqualTo(0);
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);

        // Query second page with 1 entry
        final var secondPage = taskService.getUserTasks(RetrieveMode.ALL, 1, 2);
        assertThat(secondPage.getPageObjects()).hasSize(1);
        assertThat(secondPage.getNumber()).isEqualTo(1);
    }

    @Test
    void getUserTasks_withNullPaginationParams_usesDefaults() {
        // Repository returns one task
        when(userTasks.findAll()).thenReturn(List.of(openTask));

        // Query without pagination parameters (null values use defaults)
        final var result = taskService.getUserTasks(RetrieveMode.ALL, null, null);

        // Default values: pageNumber=0, pageSize=20
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(20);
    }

    @Test
    void getUserTasks_beyondLastPage_returnsEmptyPageObjects() {
        // Repository returns 2 tasks
        when(userTasks.findAll()).thenReturn(List.of(openTask, closedTask));

        // Page number is beyond the last page
        final var result = taskService.getUserTasks(RetrieveMode.ALL, 100, 20);

        assertThat(result.getPageObjects()).isEmpty();
    }

    @Test
    void getUserTasks_withEmptyResult_returnsEmptyPage() {
        // Repository returns no tasks
        when(userTasks.findAll()).thenReturn(List.of());

        // Query empty result set
        final var result = taskService.getUserTasks(RetrieveMode.ALL, 0, 20);

        assertThat(result.getPageObjects()).isEmpty();
        assertThat(result.getTotalPages()).isEqualTo(0);
    }

    // --- getUserTasksOfWorkflow ---

    @Test
    void getUserTasksOfWorkflow_withOpenTasksMode_filtersCorrectly() {
        // Repository returns open tasks of a specific workflow
        when(userTasks.findByWorkflowIdAndEndedAtIsNull("wf-1")).thenReturn(List.of(openTask));

        // Query open tasks of a workflow
        final var result = taskService.getUserTasksOfWorkflow("wf-1", RetrieveMode.OPENTASKS, 0, 20);

        assertThat(result.getPageObjects()).containsExactly(openTask);
    }

    @Test
    void getUserTasksOfWorkflow_withClosedTasksMode_filtersCorrectly() {
        // Repository returns closed tasks of a specific workflow
        when(userTasks.findByWorkflowIdAndEndedAtIsNotNull("wf-1")).thenReturn(List.of(closedTask));

        // Query closed tasks of a workflow
        final var result = taskService.getUserTasksOfWorkflow("wf-1", RetrieveMode.CLOSEDTASKSONLY, 0, 20);

        assertThat(result.getPageObjects()).containsExactly(closedTask);
    }

    @Test
    void getUserTasksOfWorkflow_withAllMode_returnsAllTasksOfWorkflow() {
        // Repository returns all tasks of the workflow
        when(userTasks.findByWorkflowId("wf-1")).thenReturn(List.of(openTask, closedTask));

        // Query all tasks of the workflow
        final var result = taskService.getUserTasksOfWorkflow("wf-1", RetrieveMode.ALL, 0, 20);

        assertThat(result.getPageObjects()).containsExactly(openTask, closedTask);
    }

    // --- getAllUserTasks ---

    @Test
    void getAllUserTasks_returnsAllStoredTasks() {
        // Repository returns all stored tasks
        when(userTasks.findAll()).thenReturn(List.of(openTask, closedTask));

        // Query all tasks
        final var result = taskService.getAllUserTasks();

        assertThat(result).containsExactly(openTask, closedTask);
    }

    // --- updateTask ---

    @Test
    void updateTask_withExistingTask_appliesConsumerAndSaves() {
        // Repository finds the task
        when(userTasks.findById("task-1")).thenReturn(Optional.of(openTask));

        // Consumer that modifies the task
        final var consumerCalled = new AtomicBoolean(false);
        taskService.updateTask("task-1", task -> {
            task.setComment("updated");
            consumerCalled.set(true);
        });

        // Consumer must have been called
        assertThat(consumerCalled).isTrue();
        assertThat(openTask.getComment()).isEqualTo("updated");

        // Modified task must be saved
        verify(userTasks).save(openTask);
    }

    @Test
    void updateTask_withNullId_throwsIllegalArgumentException() {
        // Null ID must throw IllegalArgumentException
        assertThatThrownBy(() -> taskService.updateTask(null, task -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void updateTask_withNonExistingTask_throwsIllegalStateException() {
        // Repository finds no task
        when(userTasks.findById("unknown")).thenReturn(Optional.empty());

        // Expect IllegalStateException
        assertThatThrownBy(() -> taskService.updateTask("unknown", task -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    // --- completeTask ---

    @Test
    void completeTask_withExistingTask_appliesConsumerAndSaves() {
        // Repository finds the task
        when(userTasks.findById("task-1")).thenReturn(Optional.of(openTask));

        // Consumer sets endedAt
        taskService.completeTask("task-1", task -> task.setEndedAt(OffsetDateTime.now()));

        // Task must be ended and saved
        assertThat(openTask.getEndedAt()).isNotNull();
        verify(userTasks).save(openTask);
    }

    @Test
    void completeTask_withNullId_throwsIllegalArgumentException() {
        // Null ID must throw IllegalArgumentException
        assertThatThrownBy(() -> taskService.completeTask(null, task -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void completeTask_withNonExistingTask_throwsIllegalStateException() {
        // Repository finds no task
        when(userTasks.findById("unknown")).thenReturn(Optional.empty());

        // Expect IllegalStateException
        assertThatThrownBy(() -> taskService.completeTask("unknown", task -> {}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not found");
    }

    // --- cancelTask ---

    @Test
    void cancelTask_withExistingTask_appliesConsumerAndSaves() {
        // Repository finds the task
        when(userTasks.findById("task-1")).thenReturn(Optional.of(openTask));

        // Consumer sets endedAt
        taskService.cancelTask("task-1", task -> task.setEndedAt(OffsetDateTime.now()));

        // Task must be ended and saved
        assertThat(openTask.getEndedAt()).isNotNull();
        verify(userTasks).save(openTask);
    }

    /**
     * Ensures that cancelTask does not throw an exception when the
     * task does not exist (unlike completeTask).
     */
    @Test
    void cancelTask_withNonExistingTask_doesNothing() {
        // Repository finds no task
        when(userTasks.findById("unknown")).thenReturn(Optional.empty());

        // cancelTask should complete without error
        taskService.cancelTask("unknown", task -> task.setEndedAt(OffsetDateTime.now()));

        // save must not have been called
        verify(userTasks).findById("unknown");
    }

    @Test
    void cancelTask_withNullId_throwsIllegalArgumentException() {
        // Null ID must throw IllegalArgumentException
        assertThatThrownBy(() -> taskService.cancelTask(null, task -> {}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

}
