package io.vanillabp.cockpit.extension.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.cockpit.extension.outbox.BusinessCockpitOperations;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.spi.UserTaskDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.integration.extension.spi.election.OpenUserTask;
import io.vanillabp.integration.extension.spi.election.WorkflowElection;
import io.vanillabp.integration.extension.spi.election.WorkflowStart;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.service.AggregateServiceContext;
import io.vanillabp.integration.spi.PhaseOperationRegistry;
import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;
import io.vanillabp.integration.spi.PhaseTwoRetryLater;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.BusinessCockpitService;

/**
 * An application with several adapters, where nothing VanillaBP wrote down names the adapter of an
 * aggregate: no note of the start and no open task. A report of a change does not ask the election
 * then, because the election may ask a BPMS and wait for it in the application's transaction. The
 * entry is written without an adapter, and its dispatch asks the election.
 */
@ExtendWith(SuppressOutputExtension.class)
public class AReportWithNoKnownAdapterIsResolvedWhenDispatchedTest {

  private static final String MODULE_ID = "module";

  private static final String PROCESS_ID = "Main";

  private static final String AGGREGATE_ID = "aggregate-1";

  private final Election election = new Election();

  private final Half camunda8 = new Half("c8");

  private final Half other = new Half("other");

  private final Extension extension = new Extension(List.of(camunda8, other), election);

  @Test
  @DisplayName("The change of an aggregate nothing names the adapter of is planned without an adapter, and the election is not asked")
  public void theChangeOfAnAggregateIsPlannedWithoutAnAdapter() {

    service().aggregateChanged(AGGREGATE_ID);

    assertEquals(0, election.elected.get(), "the election was asked in the application's transaction");
    assertEquals(1, extension.plannedWorkflowChanges.size(), extension.plannedWorkflowChanges.toString());
    assertNull(extension.plannedWorkflowChanges.getFirst(), "the entry names an adapter");
    assertEquals(0, camunda8.asked.get(), "a BPMS half was asked in the application's transaction");
    assertEquals(0, other.asked.get(), "a BPMS half was asked in the application's transaction");

  }

  @Test
  @DisplayName("The change of a user task nothing names the adapter of is planned without an adapter, and the election is not asked")
  public void theChangeOfAUserTaskIsPlannedWithoutAnAdapter() {

    service().aggregateChanged(AGGREGATE_ID, "11");

    assertEquals(0, election.elected.get(), "the election was asked in the application's transaction");
    assertEquals(
        List.of(new UserTaskReference(null, MODULE_ID, PROCESS_ID, null, AGGREGATE_ID, null, "11", null, null)),
        extension.plannedUserTaskChanges);
    assertEquals(0, camunda8.asked.get(), "a BPMS half was asked in the application's transaction");
    assertEquals(0, other.asked.get(), "a BPMS half was asked in the application's transaction");

  }

  @Test
  @DisplayName("A user-task change which names no task and no adapter is planned once, for every open task")
  public void aChangeNamingNoTaskIsPlannedOnce() {

    // no task named: the empty array, because a call without one is the change of the workflow
    service().aggregateChanged(AGGREGATE_ID, new String[0]);

    assertEquals(0, election.elected.get(), "the election was asked in the application's transaction");
    assertEquals(
        List.of(new UserTaskReference(null, MODULE_ID, PROCESS_ID, null, AGGREGATE_ID, null, null, null, null)),
        extension.plannedUserTaskChanges);

  }

  @Test
  @DisplayName("A task read still asks the election, because a read may wait")
  public void aTaskReadStillAsksTheElection() {

    service().getUserTask(AGGREGATE_ID, "11");

    assertEquals(1, election.elected.get(), "the election was not asked");
    assertEquals(1, other.asked.get(), "the half the election names was not asked");
    assertEquals(0, camunda8.asked.get());

  }

  @Test
  @DisplayName("The dispatch of a workflow entry without an adapter asks the election and then the half it names")
  public void theDispatchOfAWorkflowEntryAsksTheElection() {

    final var givenBack = assertThrows(
        PhaseTwoRetryLater.class,
        () -> dispatch(workflowEntry(OffsetDateTime.now())));

    assertEquals(1, election.elected.get(), "the dispatch did not ask the election");
    assertEquals(1, other.asked.get(), "the half the election names was not asked");
    assertEquals(0, camunda8.asked.get(), "a half the election did not name was asked");
    // the half named no workflow yet, so the entry comes again, and the log names the elected half
    assertTrue(
        givenBack.getMessage().contains("adapter 'other'"),
        "the message names the elected half: %s".formatted(givenBack.getMessage()));

  }

  @Test
  @DisplayName("The dispatch of a user-task entry without an adapter asks the election and then the half it names")
  public void theDispatchOfAUserTaskEntryAsksTheElection() {

    assertThrows(PhaseTwoRetryLater.class, () -> dispatch(userTaskEntry(OffsetDateTime.now())));

    assertEquals(1, election.elected.get(), "the dispatch did not ask the election");
    assertEquals(1, other.asked.get(), "the half the election names was not asked about the task");
    assertEquals(0, camunda8.asked.get(), "a half the election did not name was asked");

  }

  @Test
  @DisplayName("An entry the election names no adapter for yet is given back to the outbox")
  public void anEntryWithoutAnAdapterYetIsGivenBack() {

    election.knowsNothing = true;

    final var givenBack = assertThrows(
        PhaseTwoRetryLater.class,
        () -> dispatch(workflowEntry(OffsetDateTime.now())));

    assertEquals(1, election.elected.get());
    assertEquals(0, camunda8.asked.get());
    assertEquals(0, other.asked.get());
    assertTrue(
        givenBack.getMessage().startsWith("VanillaBP's election names no adapter holding it yet"),
        givenBack.getMessage());

  }

  @Test
  @DisplayName("An entry the election named no adapter for during the whole window is blocked")
  public void anEntryWithoutAnAdapterAfterTheWindowIsBlocked() {

    election.knowsNothing = true;
    final var happened = OffsetDateTime.now().minusMinutes(10).minusSeconds(1);

    final var blocked = assertThrows(PhaseTwoPermanentFailure.class, () -> dispatch(workflowEntry(happened)));

    assertTrue(PhaseTwoPermanentFailure.isPermanent(blocked));

  }

  private void dispatch(
      final PhaseTwoCall call) {

    final var registry = new PhaseOperationRegistry();
    extension.registerOperations(registry);
    registry
        .dispatchFor(call.operation())
        .orElseThrow()
        .dispatch(call, false);

  }

  private static PhaseTwoCall workflowEntry(
      final OffsetDateTime happened) {

    final var args = new LinkedHashMap<String, String>();
    args.put(BusinessCockpitOperations.ARG_EVENT_KIND, "UPDATED");
    args.put(BusinessCockpitOperations.ARG_EVENT_ID, "event-1");
    args.put(BusinessCockpitOperations.ARG_TIMESTAMP, happened.toString());
    args.put(BusinessCockpitOperations.ARG_RESOLVED_WHEN_DISPATCHED, "true");
    return PhaseTwoCall
        .of(BusinessCockpitOperations.publishWorkflowEvent(), MODULE_ID, PROCESS_ID, AGGREGATE_ID, null, args);

  }

  private static PhaseTwoCall userTaskEntry(
      final OffsetDateTime happened) {

    final var args = new LinkedHashMap<String, String>();
    args.put(BusinessCockpitOperations.ARG_EVENT_KIND, "UPDATED");
    args.put(BusinessCockpitOperations.ARG_USER_TASK_ID, "11");
    args.put(BusinessCockpitOperations.ARG_EVENT_ID, "event-1");
    args.put(BusinessCockpitOperations.ARG_TIMESTAMP, happened.toString());
    args.put(BusinessCockpitOperations.ARG_RESOLVED_WHEN_DISPATCHED, "true");
    return PhaseTwoCall
        .of(BusinessCockpitOperations.publishUserTaskEvent(), MODULE_ID, PROCESS_ID, AGGREGATE_ID, null, args);

  }

  @SuppressWarnings("unchecked")
  private BusinessCockpitService<Object> service() {

    return new BusinessCockpitServiceFactory(extension).createService(new Context());

  }

  /**
   * VanillaBP wrote down nothing about the aggregate: no note of the start, no open task. Its
   * election answers "other", or throws where it knows nothing either.
   */
  private static final class Election implements WorkflowElection {

    private final AtomicInteger elected = new AtomicInteger();

    private boolean knowsNothing;

    @Override
    public String adapterIdOfWorkflow(
        final String workflowModuleId,
        final String bpmnProcessId,
        final Object workflowAggregateId) {

      elected.incrementAndGet();
      if (knowsNothing) {
        throw new IllegalStateException("No configured BPMS knows the workflow");
      }
      return "other";

    }

    @Override
    public Optional<WorkflowStart> workflowStartOf(
        final String workflowModuleId,
        final String bpmnProcessId,
        final Object workflowAggregateId) {

      return Optional.empty();

    }

    @Override
    public List<OpenUserTask> openUserTasksOf(
        final String workflowModuleId,
        final String bpmnProcessId,
        final Object workflowAggregateId) {

      return List.of();

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

  /**
   * Knows the two halves, of which none is the only one, and records what would be written into
   * the outbox. Reports are taken as switched off, which keeps the transaction check out of this
   * test, and a read runs without a transaction.
   */
  private static final class Extension extends BusinessCockpitExtension {

    private final List<String> plannedWorkflowChanges = new ArrayList<>();

    private final List<UserTaskReference> plannedUserTaskChanges = new ArrayList<>();

    private Extension(
        final List<BusinessCockpitBpmsBridge> bridges,
        final WorkflowElection election) {

      super(null, null, bridges, List.of(), noAggregateClasses(), null, null, null, () -> election);

    }

    /** VanillaBP's handlers, of which the dispatch only asks for the aggregate class. */
    private static ExtensionHandlers noAggregateClasses() {

      return (ExtensionHandlers) Proxy
          .newProxyInstance(
              ExtensionHandlers.class.getClassLoader(),
              new Class<?>[]{
                  ExtensionHandlers.class
              },
              (
                  proxy,
                  method,
                  args) -> Optional.empty());

    }

    @Override
    public boolean reportsWorkflows() {

      return false;

    }

    @Override
    public boolean reportsUserTasks() {

      return false;

    }

    @Override
    public Optional<BusinessCockpitBpmsBridge> theOnlyBridge() {

      return Optional.empty();

    }

    @Override
    public boolean publishWorkflowChangeResolvedWhenDispatched(
        final String adapterId,
        final String workflowModuleId,
        final String bpmnProcessId,
        final String workflowAggregateId,
        final String workflowId,
        final String processVersion,
        final OffsetDateTime timestamp,
        final Class<?> workflowAggregateClass) {

      plannedWorkflowChanges.add(adapterId);
      return true;

    }

    @Override
    public boolean publishUserTaskChangeResolvedWhenDispatched(
        final UserTaskReference userTask,
        final OffsetDateTime timestamp,
        final Class<?> workflowAggregateClass) {

      plannedUserTaskChanges.add(userTask);
      return true;

    }

    @Override
    public <T> T readInOneTransaction(
        final Class<?> workflowAggregateClass,
        final Supplier<T> question) {

      return question.get();

    }

  }

  /**
   * A BPMS half which counts how often it is asked about an aggregate. It knows no workflow and no
   * task, so a dispatch gives its entry back to the outbox.
   */
  private static final class Half implements BusinessCockpitBpmsBridge {

    private final String adapterId;

    private final AtomicInteger asked = new AtomicInteger();

    private Half(
        final String adapterId) {

      this.adapterId = adapterId;

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

      // a half which builds a task report in the application's transaction, like Camunda 7.
      // Without a known adapter its change is still resolved when it is dispatched
      return true;

    }

    @Override
    public Optional<UserTaskDetailsPrefill> prefilledUserTaskDetails(
        final UserTaskReference userTask) {

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

      asked.incrementAndGet();
      return List.of();

    }

    @Override
    public List<UserTaskReference> userTasksOfAggregate(
        final String workflowModuleId,
        final String bpmnProcessId,
        final String workflowAggregateId,
        final List<String> userTaskIds) {

      asked.incrementAndGet();
      return List.of();

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
