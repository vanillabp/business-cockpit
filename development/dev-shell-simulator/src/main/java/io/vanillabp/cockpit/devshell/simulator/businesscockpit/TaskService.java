package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.gui.api.v1.Page;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class for managing UserTasks.
 * Contains the Map {@code userTasks} to store and retrieve tasks.
 */
@Service
public class TaskService {

    private final Map<String, UserTask> userTasks = new HashMap<>();

    @Autowired
    private OfficialApiMapper mapper;

    /**
     * Creates a new UserTask from details and adds it to the task map.
     *
     * @param userTaskId usertask object to be put into the userTask map.
     */

    public void createTask(
            final String userTaskId,
            final UserTask userTask) {

        if ((userTaskId == null) || (userTask == null)) {
            throw new IllegalArgumentException("UserTask ID or UserTask must not be null!");
        }

        userTasks.put(userTaskId, userTask);

    }

    /**
     * Retrieves specific UserTask from the userTask Hashmap.
     *
     * @param userTaskId unique Id for each UserTask.
     * @return userTask object
     */
    public UserTask getUserTask(
            final String userTaskId) {

        if (userTaskId == null) {
            throw new IllegalArgumentException("UserTask ID must not be null");
        }

        return userTasks.get(userTaskId);

    }

    // ID, businessId, titel
    /**
     * Creates a UserTasks object containing all user tasks with pagination.
     *
     * @param userTasksRequest Request containing pagination parameters
     * @return UserTasks object with tasks and pagination info
     */
    public UserTasks getUserTasks(final UserTasksRequest userTasksRequest) {

        var allTasks = getAllUserTasks();

        // Filter based on mode
        if (userTasksRequest.getMode().equals(UserTaskRetrieveMode.OPENTASKS)) {
            allTasks = allTasks.stream()
                    .filter(task -> task.getEndedAt() == null)
                    .toList();
        } else if (userTasksRequest.getMode().equals(UserTaskRetrieveMode.CLOSEDTASKSONLY)) {
            allTasks = allTasks.stream()
                    .filter(task -> task.getEndedAt() != null)
                    .toList();
        }

        // Extract pagination params
        int pageSize = userTasksRequest.getPageSize() != null ? userTasksRequest.getPageSize() : 20;
        int pageNumber = userTasksRequest.getPageNumber() != null ? userTasksRequest.getPageNumber() : 0;

        // Total count after filtering
        int totalElements = allTasks.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        // Calculate start and end index
        int fromIndex = Math.min(pageNumber * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<UserTask> pagedTasks = allTasks.subList(fromIndex, toIndex);

        // Build page metadata
        final var page = new Page();
        page.setNumber(pageNumber);
        page.setSize(pageSize);
        page.setTotalPages(totalPages);

        return new UserTasks()
                .serverTimestamp(OffsetDateTime.now())
                .page(page)
                .userTasks(pagedTasks);
    }



    /**
     * Retrieves all user tasks from the Hashmap.
     *
     * @return List of all stored UserTask objects.
     */
    public List<UserTask> getAllUserTasks() {

        return userTasks
                .values()
                .stream()
                .toList();

    }

    /**
     * Updates an existing UserTask in the task map.
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
        if (!userTasks.containsKey(userTaskId)) {
            throw new IllegalStateException("Task with ID " + userTaskId + " not found");
        }

        final var userTask = userTasks.get(userTaskId);
        mapOnto.accept(userTask);

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
        if (!userTasks.containsKey(userTaskId)) {
            throw new IllegalStateException("Task with ID " + userTaskId + " not found");
        }

        final var userTask = userTasks.get(userTaskId);
        mapOnto.accept(userTask);

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
        if (!userTasks.containsKey(userTaskId)) {
            throw new IllegalStateException("Task with ID " + userTaskId + " not found");
        }

        final var userTask = userTasks.get(userTaskId);
        mapOnto.accept(userTask);

    }

}
