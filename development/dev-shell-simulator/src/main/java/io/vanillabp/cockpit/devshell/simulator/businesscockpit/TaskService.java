package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Page;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTaskRepository;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Keeps the user tasks the simulator received. They are stored in the simulator's in-memory
 * database.
 */
@Service
public class TaskService {

    /**
     * Which user tasks a query returns. The names are the same as in the cockpit's API.
     */
    public enum RetrieveMode {
        /** All tasks, open and closed. */
        ALL,
        /** Only the tasks which have not ended yet. */
        OPENTASKS,
        /** Treated like {@link #ALL} here, because the simulator knows no follow-up dates. */
        OPENTASKSWITHOUTFOLLOWUP,
        /** Another name for OPENTASKS, the same as in the cockpit. */
        OPENTASKSWITHFOLLOWUP,
        /** Only the tasks which have ended. */
        CLOSEDTASKSONLY
    };

    @Autowired
    private UserTaskRepository userTasks;

    /**
     * Creates the service. Spring creates it, nobody else does.
     */
    public TaskService() {
    }

    /**
     * Stores a new user task.
     *
     * @param userTaskId The id of the user task
     * @param userTask The user task to store
     */
    public void createTask(
            final String userTaskId,
            final UserTask userTask) {

        if ((userTaskId == null) || (userTask == null)) {
            throw new IllegalArgumentException("UserTask ID or UserTask must not be null!");
        }

        userTasks.save(userTask);

    }

    /**
     * Reads one user task.
     *
     * @param userTaskId unique Id for each UserTask.
     * @return userTask object
     */
    public UserTask getUserTask(
            final String userTaskId) {

        if (userTaskId == null) {
            throw new IllegalArgumentException("UserTask ID must not be null");
        }

        return userTasks
                .findById(userTaskId)
                .orElseThrow(() -> new IllegalStateException("User task with ID " + userTasks + " not found"));

    }

    private static Sort resolveSort(
            final String sortField,
            final Boolean sortAscending) {

        final String effectiveSortField = sortField != null ? sortField : "createdAt";
        final Sort.Direction direction = Boolean.TRUE.equals(sortAscending)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, effectiveSortField);
    }

    // ID, businessId, titel
    /**
     * Reads one page of the user tasks.
     *
     * @param retrieveMode Which tasks to return: all, only open ones or only closed ones
     * @param sortField The field to sort by, {@code createdAt} if {@code null}
     * @param sortAscending {@code true} to sort ascending, otherwise descending
     * @param pageNumberRequested The number of the page, starting at 0. 0 if {@code null}.
     * @param pageSizeRequested The number of tasks of a page. 20 if {@code null}.
     * @return The page of tasks
     */
    public Page<UserTask> getUserTasks(
            final RetrieveMode retrieveMode,
            final String sortField,
            final Boolean sortAscending,
            final Integer pageNumberRequested,
            final Integer pageSizeRequested) {

        final Sort sort = resolveSort(sortField, sortAscending);

        final var allTasks = switch (retrieveMode) {
            case OPENTASKS, OPENTASKSWITHFOLLOWUP -> userTasks.findByEndedAtIsNull(sort);
            case CLOSEDTASKSONLY ->  userTasks.findByEndedAtIsNotNull(sort);
            default -> userTasks.findAll(sort);
        };

        // Set default pagination values if not provided
        int pageSize = pageSizeRequested != null ? pageSizeRequested : 20;
        int pageNumber = pageNumberRequested != null ? pageNumberRequested : 0;

        // Total count after filtering
        long totalElements = allTasks.size();
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;

        // Calculate start and end index
        int fromIndex = (int) Math.min((long) pageNumber * pageSize, totalElements);
        int toIndex = (int) Math.min((long) fromIndex + pageSize, totalElements);
        List<UserTask> pagedTasks = allTasks.subList(fromIndex, toIndex);

        // Build the Page metadata
        return Page
                .<UserTask>builder()
                .number(pageNumber)
                .size(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .pageObjects(pagedTasks)
                .build();
    }

    /**
     * Reads one page of the user tasks of one workflow.
     *
     * @param workflowId The id of the workflow
     * @param retrieveMode Which tasks to return: all, only open ones or only closed ones
     * @param sortField The field to sort by, {@code createdAt} if {@code null}
     * @param sortAscending {@code true} to sort ascending, otherwise descending
     * @param pageNumberRequested The number of the page, starting at 0. 0 if {@code null}.
     * @param pageSizeRequested The number of tasks of a page. 20 if {@code null}.
     * @return The page of tasks
     */
    public Page<UserTask> getUserTasksOfWorkflow(
            final String workflowId,
            final RetrieveMode retrieveMode,
            final String sortField,
            final Boolean sortAscending,
            final Integer pageNumberRequested,
            final Integer pageSizeRequested) {

        final Sort sort = resolveSort(sortField, sortAscending);

        final var allTasks = switch (retrieveMode) {
            case OPENTASKS, OPENTASKSWITHFOLLOWUP -> userTasks.findByWorkflowIdAndEndedAtIsNull(workflowId, sort);
            case CLOSEDTASKSONLY ->  userTasks.findByWorkflowIdAndEndedAtIsNotNull(workflowId, sort);
            default -> userTasks.findByWorkflowId(workflowId, sort);
        };

        // Set default pagination values if not provided
        int pageSize = pageSizeRequested != null ? pageSizeRequested : 20;
        int pageNumber = pageNumberRequested != null ? pageNumberRequested : 0;

        // Total count after filtering
        long totalElements = allTasks.size();
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;

        // Calculate start and end index
        int fromIndex = (int) Math.min((long) pageNumber * pageSize, totalElements);
        int toIndex = (int) Math.min((long) fromIndex + pageSize, totalElements);
        List<UserTask> pagedTasks = allTasks.subList(fromIndex, toIndex);

        // Build the Page metadata
        return Page
                .<UserTask>builder()
                .number(pageNumber)
                .size(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .pageObjects(pagedTasks)
                .build();
    }

    /**
     * Reads all user tasks.
     *
     * @return List of all stored UserTask objects.
     */
    public List<UserTask> getAllUserTasks() {

        return userTasks
                .findAll();

    }

    /**
     * Updates a user task which is stored already.
     *
     * @param userTaskId The ID of the UserTask to update.
     * @param mapOnto      Mapper.
     * @throws IllegalArgumentException If the task ID is null.
     * @throws IllegalStateException    If the task does not exist.
     */
    public void updateTask(
            final String userTaskId,
            final Consumer<UserTask> mapOnto) {

        if (userTaskId == null) {
            throw new IllegalArgumentException("UserTask ID or event cannot be null!");
        }

        final var userTaskFound = userTasks
                .findById(userTaskId)
                .orElseThrow(() -> new IllegalStateException("User task with ID " + userTaskId + " not found"));

        mapOnto.accept(userTaskFound);

        userTasks.save(userTaskFound);

    }

    /**
     * Method that gets called when the user task is to be completed. The Main Purpose is to set the end date.
     *
     * @param userTaskId The ID of the UserTask to be completed.
     * @param mapOnto      Mapper.
     */
    public void completeTask(
            final String userTaskId,
            final Consumer<UserTask> mapOnto) {

        if (userTaskId == null) {
            throw new IllegalArgumentException("UserTask ID or event cannot be null!");
        }

        final var userTaskFound = userTasks
                .findById(userTaskId)
                .orElseThrow(() -> new IllegalStateException("User task with ID " + userTaskId + " not found"));

        mapOnto.accept(userTaskFound);

        userTasks.save(userTaskFound);

    }

    /**
     * Method that gets called when the user task is to be cancelled. The Main Purpose is to set the end date.
     *
     * @param userTaskId The ID of the UserTask to be completed.
     * @param mapOnto      Mapper.
     */
    public void cancelTask(
            final String userTaskId,
            final Consumer<UserTask> mapOnto) {

        if (userTaskId == null) {
            throw new IllegalArgumentException("UserTask ID cannot be null!");
        }

        userTasks
                .findById(userTaskId)
                .ifPresent(userTaskFound -> {
                    mapOnto.accept(userTaskFound);
                    userTasks.save(userTaskFound);
                });

    }

}
