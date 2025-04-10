package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.gui.api.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
/**
 * REST controller for the official task list API.
 * Allows retrieving UserTasks.
 */
@RestController
@RequestMapping("/official-api/v1")
public class OfficialApiTaskController implements OfficialTasklistApi {

    private final Logger log = LoggerFactory.getLogger(OfficialApiTaskController.class);

    private final TaskService taskService;

    /**
     * Constructor for usertask.OfficialApiTaskController.
     *
     * @param taskService The usertask.TaskService for managing UserTasks.
     */
    @Autowired
    public OfficialApiTaskController(
            final TaskService taskService) {

        this.taskService = taskService;

    }

    /**
     * Retrieves a UserTask by its ID.
     *
     * @param userTaskId The ID of the UserTask to retrieve.
     * @return A ResponseEntity containing the retrieved UserTask or an error response.
     */
    @Override
    public ResponseEntity<UserTask> getUserTask(
            final String userTaskId,
            final Boolean markAsRead) {

        final UserTask userTask = taskService.getUserTask(userTaskId);

        if (userTask == null) {
            return ResponseEntity.notFound().build();
        }

        log.info("Client retrieved UserTask {}", userTaskId);

        return ResponseEntity.ok(userTask);

    }

    @Override
    public ResponseEntity<UserTasks> getUserTasks(
            final UserTasksRequest userTasksRequest,
            final OffsetDateTime initialTimestamp) {

        // Get UserTasks from service
        UserTasks userTasks = taskService.getUserTasks(userTasksRequest);

        log.info("Client retrieved all UserTasks, total: {}", userTasks.getUserTasks().size());

        return ResponseEntity.ok(userTasks);
    }

}
