package io.vanillabp.cockpit.extension.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.integration.extension.spi.election.OpenUserTask;
import io.vanillabp.integration.extension.spi.election.WorkflowElection;
import io.vanillabp.integration.extension.spi.election.WorkflowStart;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.service.AggregateServiceContext;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.BusinessCockpitService;

/**
 * <code>BusinessCockpitService.aggregateChanged(aggregate, userTaskIds)</code> for a BPMS half
 * which cannot build the report of a changed user task in the application's transaction: which
 * entries are planned out of what VanillaBP wrote down when it delivered the tasks, and that the
 * half is asked nothing.
 */
@ExtendWith(SuppressOutputExtension.class)
public class UserTaskChangeOfAHalfWithoutRightAwayReportsTest {

  private static final String MODULE_ID = "module";

  private static final String PROCESS_ID = "Main";

  private static final String CALLED_PROCESS_ID = "Called";

  private static final String AGGREGATE_ID = "aggregate-1";

  private static final String CASE = "100";

  private final Election election = new Election();

  private final Half camunda8 = new Half("c8", false);

  private final Half other = new Half("other", false);

  private final Extension extension = new Extension(List.of(camunda8, other));

  @Test
  @DisplayName("A task VanillaBP delivered is planned with what it wrote down, and the BPMS is asked nothing")
  public void aDeliveredTaskIsPlannedWithWhatVanillaBpWroteDown() {

    election.start = Optional.of(new WorkflowStart("c8", CASE, "3", true));
    election.open = List
        .of(new OpenUserTask("c8", CASE, CASE, PROCESS_ID, "11", "approve", "Approve", "3"));

    service().aggregateChanged(AGGREGATE_ID, "11");

    assertEquals(1, extension.planned.size(), extension.planned.toString());
    final var planned = extension.planned.getFirst();
    assertEquals(
        new UserTaskReference("c8", MODULE_ID, PROCESS_ID, "3", AGGREGATE_ID, CASE, "11", "approve", "Approve"),
        planned);
    assertEquals(0, camunda8.asked.get(), "the BPMS half was asked in the application's transaction");
    assertEquals(0, election.elected.get(), "the election was asked although the note names the adapter");

  }

  @Test
  @DisplayName("A task of a called process names the case as its workflow, and its own process and version")
  public void aTaskOfACalledProcessNamesTheCase() {

    election.start = Optional.of(new WorkflowStart("c8", CASE, "3", true));
    election.open = List
        .of(new OpenUserTask("c8", CASE, "200", CALLED_PROCESS_ID, "12", "check", "Check", "7"));

    // no task named: the empty array, because a call without one is the change of the workflow
    service().aggregateChanged(AGGREGATE_ID, new String[0]);

    assertEquals(
        List
            .of(
                new UserTaskReference(
                    "c8", MODULE_ID, CALLED_PROCESS_ID, "7", AGGREGATE_ID, CASE, "12", "check", "Check")),
        extension.planned);

  }

  @Test
  @DisplayName("Without a note of the start a task of the aggregate's own process runs in the case, and a called one is left to the dispatch")
  public void withoutANoteOnlyATaskOfTheOwnProcessNamesItsCase() {

    election.open = List
        .of(
            new OpenUserTask("c8", null, CASE, PROCESS_ID, "11", "approve", "Approve", "3"),
            new OpenUserTask("c8", null, "200", CALLED_PROCESS_ID, "12", "check", "Check", "7"));

    // no task named: the empty array, because a call without one is the change of the workflow
    service().aggregateChanged(AGGREGATE_ID, new String[0]);

    assertEquals(2, extension.planned.size(), extension.planned.toString());
    assertEquals(CASE, extension.planned.get(0).workflowId());
    // the case above a called process is not known here. The dispatch looks the task up in the
    // aggregate's own process, by its id alone
    assertEquals(
        new UserTaskReference("c8", MODULE_ID, PROCESS_ID, null, AGGREGATE_ID, null, "12", null, null),
        extension.planned.get(1));

  }

  @Test
  @DisplayName("Only the named tasks are planned, and a named task VanillaBP did not write down is planned by its id")
  public void onlyTheNamedTasksArePlanned() {

    election.start = Optional.of(new WorkflowStart("c8", CASE, "3", true));
    election.open = List
        .of(
            new OpenUserTask("c8", CASE, CASE, PROCESS_ID, "11", "approve", "Approve", "3"),
            new OpenUserTask("c8", CASE, CASE, PROCESS_ID, "13", "approve", "Approve", "3"));

    service().aggregateChanged(AGGREGATE_ID, "13", "99", "99");

    assertEquals(
        List
            .of(
                new UserTaskReference("c8", MODULE_ID, PROCESS_ID, "3", AGGREGATE_ID, CASE, "13", "approve", "Approve"),
                new UserTaskReference("c8", MODULE_ID, PROCESS_ID, null, AGGREGATE_ID, null, "99", null, null)),
        extension.planned);

  }

  @Test
  @DisplayName("Where nothing is named and VanillaBP knows of no open task, the dispatch looks for every open task")
  public void nothingNamedAndNothingWrittenDownLeavesTheSearchToTheDispatch() {

    election.start = Optional.of(new WorkflowStart("c8", CASE, "3", true));

    // no task named: the empty array, because a call without one is the change of the workflow
    service().aggregateChanged(AGGREGATE_ID, new String[0]);

    assertEquals(1, extension.planned.size(), extension.planned.toString());
    assertNull(extension.planned.getFirst().userTaskId());
    assertEquals(0, camunda8.asked.get());

  }

  @Test
  @DisplayName("A task another adapter delivered is left out, and without a note the delivering adapter picks the half")
  public void theAdapterWhichDeliveredTheTasksPicksTheHalf() {

    election.open = List
        .of(new OpenUserTask("other", null, "500", PROCESS_ID, "21", "approve", "Approve", "1"));

    // no task named: the empty array, because a call without one is the change of the workflow
    service().aggregateChanged(AGGREGATE_ID, new String[0]);

    assertEquals(1, extension.planned.size(), extension.planned.toString());
    assertEquals("other", extension.planned.getFirst().adapterId());
    assertEquals("500", extension.planned.getFirst().workflowId());
    assertEquals(0, election.elected.get(), "the election was asked although the delivered tasks name the adapter");

  }

  @Test
  @DisplayName("A half which builds the report right away is asked as before, and the delivery log is not read")
  public void aHalfWhichBuildsRightAwayIsAskedAsBefore() {

    final var rightAway = new Half("c7", true);
    final var extension = new Extension(List.of(rightAway));
    election.start = Optional.of(new WorkflowStart("c7", CASE, "3", true));

    service(extension).aggregateChanged(AGGREGATE_ID, "11");

    assertEquals(1, rightAway.asked.get());
    assertEquals(1, extension.reportedRightAway.size());
    assertTrue(extension.planned.isEmpty());
    assertEquals(0, election.openRead.get(), "the delivery log was read for a half which needs it not");

  }

  private BusinessCockpitService<Object> service() {

    return service(extension);

  }

  @SuppressWarnings("unchecked")
  private BusinessCockpitService<Object> service(
      final Extension extension) {

    return new BusinessCockpitServiceFactory(extension).createService(new Context());

  }

  /** What VanillaBP wrote down about the workflow and its tasks. */
  private static final class Election implements WorkflowElection {

    private Optional<WorkflowStart> start = Optional.empty();

    private List<OpenUserTask> open = List.of();

    private final AtomicInteger elected = new AtomicInteger();

    private final AtomicInteger openRead = new AtomicInteger();

    @Override
    public String adapterIdOfWorkflow(
        final String workflowModuleId,
        final String bpmnProcessId,
        final Object workflowAggregateId) {

      elected.incrementAndGet();
      return "c8";

    }

    @Override
    public Optional<WorkflowStart> workflowStartOf(
        final String workflowModuleId,
        final String bpmnProcessId,
        final Object workflowAggregateId) {

      return start;

    }

    @Override
    public List<OpenUserTask> openUserTasksOf(
        final String workflowModuleId,
        final String bpmnProcessId,
        final Object workflowAggregateId) {

      openRead.incrementAndGet();
      return open;

    }

  }

  /** The aggregate is its own id, which is all the service reads of it here. */
  private final class Context implements AggregateServiceContext {

    @Override
    public Class<?> getWorkflowAggregateClass() {

      return String.class;

    }

    @Override
    public String getWorkflowModuleId() {

      return MODULE_ID;

    }

    @Override
    public String getBpmnProcessId() {

      return PROCESS_ID;

    }

    @Override
    public Object getWorkflowAggregateId(
        final Object workflowAggregate) {

      return workflowAggregate;

    }

    @Override
    public Object loadWorkflowAggregate(
        final Object workflowAggregateId) {

      return workflowAggregateId;

    }

    @Override
    public Object saveWorkflowAggregate(
        final Object workflowAggregate) {

      return workflowAggregate;

    }

    @Override
    public ExtensionHandlers getHandlers() {

      return null;

    }

    @Override
    public WorkflowElection getElection() {

      return election;

    }

  }

  /** Records what would be written into the outbox, and takes every report as switched on. */
  private static final class Extension extends BusinessCockpitExtension {

    private final List<UserTaskReference> planned = new ArrayList<>();

    private final List<UserTaskReference> reportedRightAway = new ArrayList<>();

    private Extension(
        final List<BusinessCockpitBpmsBridge> bridges) {

      super(null, null, bridges, List.of(), null, null, null, null, null);

    }

    @Override
    public boolean reportsUserTasks() {

      // a report nobody writes needs no transaction, which keeps the transaction check out of
      // this test
      return false;

    }

    @Override
    public Optional<BusinessCockpitBpmsBridge> theOnlyBridge() {

      return Optional.empty();

    }

    @Override
    public boolean publishUserTaskChangeResolvedWhenDispatched(
        final UserTaskReference userTask,
        final OffsetDateTime timestamp,
        final Class<?> workflowAggregateClass) {

      planned.add(userTask);
      return true;

    }

    @Override
    public boolean publishUserTaskEvent(
        final UserTaskReference userTask,
        final UserTaskEventKind kind,
        final String bpmsEventId,
        final OffsetDateTime timestamp,
        final EventTransaction transaction,
        final Class<?> workflowAggregateClass) {

      reportedRightAway.add(userTask);
      return true;

    }

  }

  /** A BPMS half which counts how often it is asked about the tasks of an aggregate. */
  private static final class Half implements BusinessCockpitBpmsBridge {

    private final String adapterId;

    private final boolean rightAway;

    private final AtomicInteger asked = new AtomicInteger();

    private Half(
        final String adapterId,
        final boolean rightAway) {

      this.adapterId = adapterId;
      this.rightAway = rightAway;

    }

    @Override
    public String adapterId() {

      return adapterId;

    }

    @Override
    public String adapterType() {

      return "test";

    }

    @Override
    public boolean reportsAChangedUserTaskRightAway() {

      return rightAway;

    }

    @Override
    public Optional<UserTaskDetailsPrefill> prefilledUserTaskDetails(
        final UserTaskReference userTask) {

      asked.incrementAndGet();
      return Optional.empty();

    }

    @Override
    public Optional<WorkflowDetailsPrefill> prefilledWorkflowDetails(
        final WorkflowReference workflow) {

      return Optional.empty();

    }

    @Override
    public List<WorkflowReference> workflowsOfAggregate(
        final String workflowModuleId,
        final String bpmnProcessId,
        final String workflowAggregateId) {

      return List.of();

    }

    @Override
    public List<UserTaskReference> userTasksOfAggregate(
        final String workflowModuleId,
        final String bpmnProcessId,
        final String workflowAggregateId,
        final List<String> userTaskIds) {

      asked.incrementAndGet();
      return List
          .of(
              new UserTaskReference(
                  adapterId, workflowModuleId, bpmnProcessId, "3", workflowAggregateId, CASE, "11", "approve", "Approve"));

    }

    @Override
    public Optional<UserTaskReference> userTaskOfAggregate(
        final String workflowModuleId,
        final String bpmnProcessId,
        final String workflowAggregateId,
        final String userTaskId) {

      asked.incrementAndGet();
      return Optional.empty();

    }

  }

}
