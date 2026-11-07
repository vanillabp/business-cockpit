package io.vanillabp.cockpit.simulator.workflow;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devskiller.jfairy.Fairy;

import io.vanillabp.cockpit.gui.api.v1.Count;
import io.vanillabp.cockpit.gui.api.v1.Counts;
import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import io.vanillabp.cockpit.gui.api.v1.Workflow;
import io.vanillabp.cockpit.gui.api.v1.WorkflowCountsRequest;
import io.vanillabp.cockpit.gui.api.v1.WorkflowRetrieveMode;
import io.vanillabp.cockpit.simulator.common.FairyHelper;
import io.vanillabp.cockpit.simulator.usertask.OfficialTasklistApiMapper;
import io.vanillabp.cockpit.simulator.usertask.testdata.UserTaskTestDataGenerator;
import io.vanillabp.cockpit.simulator.workflow.testdata.WorkflowTestDataGenerator;

/**
 * A fake of the Business Cockpit's official workflow list API. It answers every workflow id with
 * a random workflow and keeps it, so the next request for the same id returns the same workflow.
 * The user tasks of a workflow are random too, and kept the same way. It counts the workflows it
 * keeps. It has no list of its own.
 */
@RestController
@RequestMapping(path = "/official-api/v1")
public class OfficialWorkflowlistApiController implements OfficialWorkflowlistApi {

  /**
   * Creates the controller. Spring creates it, nobody else does.
   */
  public OfficialWorkflowlistApiController() {
  }

  private Map<String, Workflow> workflows = new HashMap<>();

  private Map<String, List<UserTask>> userTasks = new HashMap<>();

  private final static Random random = new Random(System.currentTimeMillis());

  private final static Map<String, Fairy> fairies = new HashMap<>();

  static {
    fairies.put("de", FairyHelper.buildFairy("de"));
    fairies.put("en", FairyHelper.buildFairy("en"));
  }

  @Autowired
  private OfficialWorkflowlistApiMapper mapper;

  @Autowired
  private OfficialTasklistApiMapper userTaskMapper;

  @Override
  public ResponseEntity<Workflow> getWorkflow(
      final String workflowId) {

    final var existingWorkflow = workflows.get(workflowId);
    if (existingWorkflow != null) {
      return ResponseEntity.ok(existingWorkflow);
    }

    final var createdEvent = WorkflowTestDataGenerator
        .buildCreatedEvent(random, fairies);

    final var result = mapper.toApi(createdEvent);
    result.setId(workflowId);

    workflows.put(workflowId, result);

    return ResponseEntity.ok(result);

  }

  @Override
  public ResponseEntity<List<UserTask>> getUserTasksOfWorkflow(
      final String workflowId,
      final Boolean llatcup,
      final UserTasksRequest userTasksRequest) {

    final var existingUserTasks = userTasks.get(workflowId);
    if (existingUserTasks != null) {
      return ResponseEntity.ok(existingUserTasks);
    }

    final var result = new LinkedList<UserTask>();
    userTasks.put(workflowId, result);

    final var num = random.nextInt(userTasksRequest.getMode() == UserTaskRetrieveMode.OPENTASKSWITHFOLLOWUP ? 5 : 10);
    String businessId = null;
    for (int i = 0; i < num; ++i) {

      final var createdEvent = UserTaskTestDataGenerator
          .buildCreatedEvent(random, fairies, null, null, null);
      createdEvent.setWorkflowId(workflowId);
      if (businessId == null) {
        businessId = createdEvent.getBusinessId();
      } else {
        createdEvent.setBusinessId(businessId);
      }
      result.add(userTaskMapper.toApi(createdEvent));

    }

    return ResponseEntity.ok(result);

  }

  /**
   * Counts the workflows the simulator keeps, for each filter by its mode and its business ids.
   * Like the cockpit, a filter without a mode counts the workflows in progress. The simulator reads
   * no search queries.
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
            .count(workflows
                .values()
                .stream()
                .filter(workflow -> shows(filter.getMode(), workflow))
                .filter(workflow -> (filter.getBusinessIds() == null)
                    || filter.getBusinessIds().isEmpty()
                    || filter.getBusinessIds().contains(workflow.getBusinessId()))
                .count()))
        .toList();

    return ResponseEntity.ok(new Counts()
        .serverTimestamp(initialTimestamp != null ? initialTimestamp : OffsetDateTime.now())
        .counts(counts));

  }

  /**
   * Whether a list of the given mode holds the workflow, by the rules of the cockpit.
   *
   * @param mode The mode, or {@code null} for the workflows in progress
   * @param workflow The workflow
   * @return Whether the list holds the workflow
   */
  static boolean shows(
      final WorkflowRetrieveMode mode,
      final Workflow workflow) {

    final var inProgress = workflow.getEndedAt() == null;
    return switch (mode == null ? WorkflowRetrieveMode.INPROGRESS : mode) {
      case ALL -> true;
      case INPROGRESS, ACTIVE -> inProgress;
      case DONE, INACTIVE -> !inProgress;
    };

  }

}
