package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            final UserTaskCreatedOrUpdatedEvent event) {

        if ((userTaskId == null) || (event == null)) {
            throw new IllegalArgumentException("UserTask ID or event must not be null!");
        }

        final var userTask = mapper.toApi(event);
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
     * @param event      UserTaskCreatedOrUpdatedEvent that contains all variables to update.
     * @throws IllegalArgumentException If the task ID is null.
     * @throws IllegalStateException    If the task does not exist.
     */
    public void updateTask(
            final String userTaskId,
            final UserTaskCreatedOrUpdatedEvent event) {

        if ((userTaskId == null) || (event == null)) {
            throw new IllegalArgumentException("UserTask ID or event cannot be null!");
        }
        if (!userTasks.containsKey(userTaskId)) {
            throw new IllegalStateException("Task with ID " + userTaskId + " not found");
        }

        final var userTask = userTasks.get(userTaskId);
        mapper.ontoApi(userTask, event);

    }

    /**
     * Method that gets called when the user task is to be completed. The Main Purpose is to set the end date.
     *
     * @param userTaskId The ID of the UserTask to be completed.
     * @param event      UserTaskCreatedOrUpdatedEvent that contains all variables to update.
     */
    public void completeTask(
            final String userTaskId,
            final UserTaskCompletedEvent event) {

        if ((userTaskId == null) || (event == null)) {
            throw new IllegalArgumentException("UserTask ID or event cannot be null!");
        }
        if (!userTasks.containsKey(userTaskId)) {
            throw new IllegalStateException("Task with ID " + userTaskId + " not found");
        }

        final var userTask = userTasks.get(userTaskId);
        mapper.ontoApi(userTask, event);

    }

    /**
     * Method that gets called when the user task is to be cancelled. The Main Purpose is to set the end date.
     *
     * @param userTaskId The ID of the UserTask to be completed.
     * @param event      UserTaskCreatedOrUpdatedEvent that contains all variables to update.
     */
    public void cancelTask(
            final String userTaskId,
            final UserTaskCancelledEvent event) {

        if ((userTaskId == null) || (event == null)) {
            throw new IllegalArgumentException("UserTask ID or event cannot be null!");
        }
        if (!userTasks.containsKey(userTaskId)) {
            throw new IllegalStateException("Task with ID " + userTaskId + " not found");
        }

        final var userTask = userTasks.get(userTaskId);
        mapper.ontoApi(userTask, event);

    }

}
