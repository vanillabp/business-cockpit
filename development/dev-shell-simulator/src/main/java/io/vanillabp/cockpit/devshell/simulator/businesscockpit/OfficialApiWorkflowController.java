package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.gui.api.v1.Workflows;
import io.vanillabp.cockpit.gui.api.v1.WorkflowsRequest;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RequestMapping("/official-api/v1")
public class OfficialApiWorkflowController implements OfficialWorkflowlistApi {

    private final Logger log = LoggerFactory.getLogger(OfficialApiWorkflowController.class);

    private final WorkflowService workflowService;

    private final TaskService taskService;

    private final OfficialApiMapper mapper;

    /**
     * Method that returns a List of UserTasks of a specific Workflow.
     *
     * @param workflowId       unique Id for each workflow. (required)
     * @param llatcup          limitListAccordingToCurrentUsersPermissions (required)
     * @param userTasksRequest (required)
     * @return
     */
    @Override
    public ResponseEntity<List<UserTask>> getUserTasksOfWorkflow(
            final String workflowId,
            final Boolean llatcup,
            final UserTasksRequest userTasksRequest) {

        final var filteredUserTasks = taskService
                .getUserTasksOfWorkflow(
                        workflowId,
                        mapper.toModel(userTasksRequest.getMode()),
                        userTasksRequest.getSort(),
                        userTasksRequest.getSortAscending(),
                        userTasksRequest.getPageNumber(),
                        userTasksRequest.getPageSize());
        final var apiUsertasks = mapper.toUserTasksApi(filteredUserTasks);

        log.info("Client retrieved user tasks of workflow {}", workflowId);

        return ResponseEntity.ok(apiUsertasks.getUserTasks());

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

        final var workflowFound = workflowService.getWorkflow(workflowId);
        final var apiWorkflow = mapper.toApi(workflowFound);

        log.info("Client retrieved workflow {}", workflowId);

        return ResponseEntity.ok(apiWorkflow);

    }

    @Override
    public ResponseEntity<Workflows> getWorkflows(
            WorkflowsRequest workflowsRequest,
            String requestId,
            OffsetDateTime initialTimestamp) {

        final var workflows = workflowService.getWorkflows(
                mapper.toModel(workflowsRequest.getMode()),
                workflowsRequest.getBusinessIds(),
                workflowsRequest.getSort(),
                workflowsRequest.getSortAscending(),
                workflowsRequest.getPageNumber(),
                workflowsRequest.getPageSize());
        final var apiWorkflows = mapper.toWorkflowsApi(workflows);

        return ResponseEntity.ok(apiWorkflows);

    }
}
