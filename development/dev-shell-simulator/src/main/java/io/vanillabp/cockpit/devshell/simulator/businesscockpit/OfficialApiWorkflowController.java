package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for the official workflow API.
 * Allows retrieving UserTasks.
 */
@RestController
@RequestMapping("/official-api/v1")
public class OfficialApiWorkflowController implements OfficialWorkflowlistApi {

    private final Logger log = LoggerFactory.getLogger(OfficialApiWorkflowController.class);

    private final WorkflowService workflowService;

    private final TaskService taskService;

    public OfficialApiWorkflowController(
            final WorkflowService workflowService,
            final TaskService taskService) {

        this.workflowService = workflowService;
        this.taskService = taskService;
    }

    /**
     * Method that returns a List of UserTasks of a specific Workflow.
     * (Currently just returns all UserTasks in the userTask map from the TaskService)
     *
     * @param workflowId unique Id for each workflow. (required)
     * @param llatcup  (required)
     * @param userTasksRequest  (required)
     * @return
     */
    @Override
    public ResponseEntity<List<UserTask>> getUserTasksOfWorkflow(
            final String workflowId,
            final Boolean llatcup,
            final UserTasksRequest userTasksRequest) {

        log.info("Received request to retrieve user tasks of workflow with id {}", workflowId);

        return ResponseEntity.ok(taskService.getAllUserTasks());
    }

    /**
     * Method that returns a specific Workflow based on workflowId
     *
     * @param workflowId unique Id for each workflow. (required)
     * @return returns the requested workflow.
     */
    @Override
    public ResponseEntity<Workflow> getWorkflow(
            final String workflowId) {

        log.info("Received request to retrieve workflow with id {}", workflowId);

        return ResponseEntity.ok(workflowService.getWorkflow(workflowId));
    }
}
