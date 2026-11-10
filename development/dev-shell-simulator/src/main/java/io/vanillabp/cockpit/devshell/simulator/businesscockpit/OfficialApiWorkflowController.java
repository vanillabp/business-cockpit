package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.gui.api.v1.Count;
import io.vanillabp.cockpit.gui.api.v1.Counts;
import io.vanillabp.cockpit.gui.api.v1.NoteRequest;
import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.gui.api.v1.WorkflowCountsRequest;
import io.vanillabp.cockpit.gui.api.v1.Workflows;
import io.vanillabp.cockpit.gui.api.v1.WorkflowsNoteRequest;
import io.vanillabp.cockpit.gui.api.v1.WorkflowsRequest;

/**
 * REST controller for the official workflow API.
 * Allows retrieving workflows and their user tasks.
 */
@RestController
@RequestMapping("/official-api/v1")
public class OfficialApiWorkflowController implements OfficialWorkflowlistApi {

  private final Logger log = LoggerFactory.getLogger(OfficialApiWorkflowController.class);

  private final WorkflowService workflowService;

  private final TaskService taskService;

  private final OfficialApiMapper mapper;

  /**
   * Creates the controller. Spring creates it, nobody else does.
   *
   * @param workflowService Keeps the workflows
   * @param taskService Keeps the user tasks
   * @param mapper Converts the workflows and user tasks for the official API
   */
  public OfficialApiWorkflowController(
      final WorkflowService workflowService,
      final TaskService taskService,
      final OfficialApiMapper mapper) {

    this.workflowService = workflowService;
    this.taskService = taskService;
    this.mapper = mapper;

  }

  /**
   * Method that returns a List of UserTasks of a specific Workflow.
   *
   * @param workflowId       unique Id for each workflow. (required)
   * @param llatcup          limitListAccordingToCurrentUsersPermissions (required)
   * @param userTasksRequest (required)
   * @return The user tasks of the workflow
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
   * Reads one workflow. Like the cockpit, the simulator answers 404 for an id it does not hold.
   *
   * @param workflowId The id of the workflow
   * @return The workflow, or 404 if there is none of that id
   */
  @Override
  public ResponseEntity<Workflow> getWorkflow(
      final String workflowId) {

    final var workflowFound = workflowService.getWorkflow(workflowId);
    if (workflowFound.isEmpty()) {
      log.info("Client asked for workflow {}, which the simulator does not hold", workflowId);
      return ResponseEntity.notFound().build();
    }

    log.info("Client retrieved workflow {}", workflowId);

    return ResponseEntity.ok(mapper.toApi(workflowFound.get()));

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

  /**
   * Counts the workflows of each filter, with the same modes and business ids as the list. Like
   * the list, the simulator reads no search queries.
   */
  @Override
  public ResponseEntity<Counts> countWorkflows(
      final WorkflowCountsRequest workflowCountsRequest,
      final OffsetDateTime initialTimestamp) {

    final var counts = workflowCountsRequest
        .getFilters()
        .stream()
        .map(filter -> new Count()
            .key(filter.getKey())
            .count(workflowService.countWorkflows(
                mapper.toModel(filter.getMode()),
                filter.getBusinessIds())))
        .toList();

    return ResponseEntity.ok(new Counts()
        .serverTimestamp(initialTimestamp != null ? initialTimestamp : OffsetDateTime.now())
        .counts(counts));

  }

  /**
   * Sets the note of a workflow the way the cockpit does: 404 for an unknown workflow and 409 for
   * an ended one.
   */
  @Override
  public ResponseEntity<Workflow> setWorkflowNote(
      final String workflowId,
      final NoteRequest noteRequest) {

    try {
      final var workflow = workflowService.setNote(
          workflowId, noteRequest == null ? null : noteRequest.getText());
      return workflow == null
          ? ResponseEntity.notFound().build()
          : ResponseEntity.ok(mapper.toApi(workflow));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

  }

  /**
   * Sets the same note on several workflows. Unlike the cockpit, the simulator changes the
   * running workflows before it meets an ended one, which is good enough for a developer's
   * machine.
   */
  @Override
  public ResponseEntity<Void> setWorkflowsNote(
      final WorkflowsNoteRequest workflowsNoteRequest) {

    try {
      workflowsNoteRequest
          .getWorkflowIds()
          .forEach(workflowId -> workflowService.setNote(workflowId, workflowsNoteRequest.getText()));
      return ResponseEntity.ok().build();
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

  }

}
