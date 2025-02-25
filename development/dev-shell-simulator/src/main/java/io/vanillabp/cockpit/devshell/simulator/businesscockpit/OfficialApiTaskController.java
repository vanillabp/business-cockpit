package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.gui.api.v1.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

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
    public OfficialApiTaskController(TaskService taskService) {
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


        UserTask userTask = taskService.getUserTask(userTaskId);

        if (userTask == null) {
            return ResponseEntity.notFound().build();
        }

        log.info("UserTask retrieved: {}", userTask);

        return ResponseEntity.ok(userTask);
    }


}
