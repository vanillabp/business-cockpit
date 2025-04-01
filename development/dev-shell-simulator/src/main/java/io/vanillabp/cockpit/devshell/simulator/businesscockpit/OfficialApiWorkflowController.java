package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.gui.api.v1.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     *
     * @param workflowId       unique Id for each workflow. (required)
     * @param llatcup          (required)
     * @param userTasksRequest (required)
     * @return
     */
    @Override
    public ResponseEntity<List<UserTask>> getUserTasksOfWorkflow(
            final String workflowId,
            final Boolean llatcup,
            final UserTasksRequest userTasksRequest) {

        List<UserTask> filteredUserTasks = taskService.getAllUserTasks().stream().
                filter(task -> task.getWorkflowId().equals(workflowId)).toList();

        log.info("Client retrieved user tasks of workflow {}", workflowId);

        return ResponseEntity.ok(filteredUserTasks);

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

        log.info("Client retrieved workflow {}", workflowId);

        return ResponseEntity.ok(workflowService.getWorkflow(workflowId));

    }

    @Override
    public ResponseEntity<Workflows> getWorkflows(
            WorkflowsRequest workflowsRequest,
            String requestId,
            OffsetDateTime initialTimestamp) {

        Workflows workflows = workflowService.getWorkflowsResponse(workflowsRequest);

        return ResponseEntity.ok(workflows);
    }
}
