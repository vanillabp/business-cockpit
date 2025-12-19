package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.gui.api.v1.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * REST controller for the official task list API.
 * Allows retrieving UserTasks.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/official-api/v1")
public class OfficialApiTaskController implements OfficialTasklistApi {

    private final Logger log = LoggerFactory.getLogger(OfficialApiTaskController.class);

    private final TaskService taskService;

    private final OfficialApiMapper mapper;

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

        final var userTaskFound = taskService.getUserTask(userTaskId);
        final var apiUserTask = mapper.toApi(userTaskFound);

        log.info("Client retrieved UserTask {}", userTaskId);

        return ResponseEntity.ok(apiUserTask);

    }

    @Override
    public ResponseEntity<UserTasks> getUserTasks(
            final UserTasksRequest userTasksRequest,
            final OffsetDateTime initialTimestamp) {
        
        final var userTasks = taskService.getUserTasks(
                mapper.toModel(userTasksRequest.getMode()),
                userTasksRequest.getPageNumber(),
                userTasksRequest.getPageSize());
        final var apiUserTasks = mapper.toUserTasksApi(userTasks);

        log.info("Client retrieved UserTasks, total: {}", userTasks.getTotalElements());

        return ResponseEntity.ok(apiUserTasks);
    }

}
