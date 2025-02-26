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
            final WorkflowService workflowService, TaskService taskService) {

        this.workflowService = workflowService;
        this.taskService = taskService;
    }

    @Override
    public ResponseEntity<List<UserTask>> getUserTasksOfWorkflow(
            final String workflowId,
            final Boolean llatcup,
            final UserTasksRequest userTasksRequest) {

        return ResponseEntity.ok(taskService.getAllUserTasks());
    }

    @Override
    public ResponseEntity<Workflow> getWorkflow(
            final String workflowId) {

        return ResponseEntity.ok(workflowService.getWorkflow(workflowId));
    }
}
