package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service class for managing UserTasks.
 * Contains the Map {@code userTasks} to store and retrieve tasks.
 */
@Service
public class TaskService {

    private final Map<String, UserTask> userTasks = new HashMap<>();

    private final Logger log = LoggerFactory.getLogger(TaskService.class);

    /**
     * Creates a new UserTask from details and adds it to the task map.
     *
     * @param taskId usertask object to be put into the userTask map.
     */

    public void createTask(
            final String taskId,
            final UserTaskCreatedOrUpdatedEvent event) {

        if (taskId == null) {
            throw new IllegalArgumentException("UserTask ID and details cannot be null");
        }

        final UserTask userTask = new UserTask();
        userTask.setId(taskId);
        userTask.setBpmnTaskId(event.getBpmnTaskId());
        userTask.setInitiator(event.getInitiator());
        userTask.setWorkflowModuleId(event.getWorkflowModuleId());
        userTask.setComment(event.getComment());
        userTask.setBpmnProcessId(event.getBpmnProcessId());
        log.info("BpmnProcess ID: {}", userTask.getBpmnProcessId());
        userTask.setBpmnProcessVersion(event.getBpmnProcessVersion());
        userTask.setWorkflowTitle(event.getWorkflowTitle());
        userTask.setWorkflowId(event.getWorkflowId());
        userTask.setBusinessId(event.getBusinessId());
        userTask.setTitle(event.getTitle());
        userTask.setBpmnTaskId(event.getBpmnTaskId());
        userTask.setTaskDefinition(event.getTaskDefinition());
        userTask.setTaskDefinitionTitle(event.getTaskDefinitionTitle());
        userTask.setDueDate(event.getDueDate());
        userTask.setFollowUpDate(event.getFollowUpDate());
        userTask.setDetails(event.getDetails());
        userTask.setDetailsFulltextSearch(event.getDetailsFulltextSearch());

        userTasks.put(taskId, userTask);
        log.info("Created task with ID: " + taskId);
        log.info("usertask object: " + taskId);
    }

    public UserTask getUserTask(final String userTaskId) {
        log.info("Getting user task with ID: " + userTaskId);
        return userTasks.get(userTaskId);
    }

    /**
     * Updates an existing UserTask in the task map.
     *
     * @param userTaskId The ID of the UserTask to update.
     * @param details The map containing task details.
     * @throws IllegalArgumentException If the task ID is null.
     * @throws IllegalStateException If the task does not exist.
     */
    public void updateTask(final String userTaskId, final Map<String, Object> details) {
        if (userTaskId == null) {
            throw new IllegalArgumentException("UserTask ID cannot be null");
        }
        if (!userTasks.containsKey(userTaskId)) {
            throw new IllegalStateException("Task with ID " + userTaskId + " not found");
        }
        log.info("Updating user task with ID: " + userTaskId);

        userTasks.get(userTaskId).setDetails(details);
    }
}
