package io.vanillabp.cockpit.extension.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
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
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.integration.extension.spi.election.OpenUserTask;
import io.vanillabp.integration.extension.spi.election.WorkflowElection;
import io.vanillabp.integration.extension.spi.election.WorkflowStart;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.service.AggregateServiceContext;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.BusinessCockpitService;

/**
 * A <code>&#64;WorkflowStartedByBpms</code> method which reports the change of the aggregate it
 * builds. Nothing can find the new workflow at that moment, and the start listener of the cockpit
 * reports it right after the start. So the report does nothing: it asks no election, no BPMS half
 * and writes no entry into the outbox. An aggregate which exists already is reported as always,
 * and VanillaBP's election says which one is which.
 */
@ExtendWith(SuppressOutputExtension.class)
public class AReportInsideTheStartOfAWorkflowDoesNothingTest {

  private static final String MODULE_ID = "module";

  private static final String PROCESS_ID = "Main";

  /** The aggregate the running start builds. */
  private static final String NEW_AGGREGATE = "new-aggregate";

  /** An aggregate which exists already, maybe returned by the same method. */
  private static final String EXISTING_AGGREGATE = "existing-aggregate";

  private final Election election = new Election();

  private final Half camunda8 = new Half("c8");

  private final Half other = new Half("other");

  private final Extension extension = new Extension(List.of(camunda8, other), election);

  @Test
  @DisplayName("The change of the aggregate a running start builds writes nothing and asks nobody")
  public void theChangeOfTheNewAggregateDoesNothing() {

    service().aggregateChanged(NEW_AGGREGATE);

    assertEquals(List.of(NEW_AGGREGATE), election.askedWhetherInsideTheStart);
    assertNothingWasAskedOrWritten();

  }

  @Test
  @DisplayName("The change of the user tasks of the aggregate a running start builds writes nothing and asks nobody")
  public void theChangeOfTheUserTasksOfTheNewAggregateDoesNothing() {

    service().aggregateChanged(NEW_AGGREGATE, "11");
    service().aggregateChanged(NEW_AGGREGATE, new String[0]);

    assertEquals(List.of(NEW_AGGREGATE, NEW_AGGREGATE), election.askedWhetherInsideTheStart);
    assertNothingWasAskedOrWritten();

  }

  @Test
  @DisplayName("The change of an aggregate which exists already is reported as always")
  public void theChangeOfAnExistingAggregateIsReported() {

    service().aggregateChanged(EXISTING_AGGREGATE);

    assertEquals(List.of(EXISTING_AGGREGATE), election.askedWhetherInsideTheStart);
    assertEquals(1, election.startsLookedUp.get(), "the note of the start was not read");
    // two adapters and no note of the start: the entry is written without an adapter
    assertEquals(List.of(EXISTING_AGGREGATE), extension.plannedWorkflowChanges);

  }

  @Test
  @DisplayName("The change of the user tasks of an aggregate which exists already is reported as always")
  public void theChangeOfTheUserTasksOfAnExistingAggregateIsReported() {

    service().aggregateChanged(EXISTING_AGGREGATE, "11");

    assertEquals(List.of(EXISTING_AGGREGATE), election.askedWhetherInsideTheStart);
    assertEquals(1, election.startsLookedUp.get(), "the note of the start was not read");
    assertEquals(1, extension.plannedUserTaskChanges.size(), extension.plannedUserTaskChanges.toString());
    assertEquals("11", extension.plannedUserTaskChanges.getFirst().userTaskId());

  }

  private void assertNothingWasAskedOrWritten() {

    assertEquals(0, election.startsLookedUp.get(), "the note of the start was read");
    assertEquals(0, election.openTasksLookedUp.get(), "the open tasks were read");
    assertEquals(0, election.elected.get(), "the election was asked for an adapter");
    assertEquals(0, camunda8.asked.get(), "a BPMS half was asked");
    assertEquals(0, other.asked.get(), "a BPMS half was asked");
    assertTrue(extension.plannedWorkflowChanges.isEmpty(), extension.plannedWorkflowChanges.toString());
    assertTrue(extension.plannedUserTaskChanges.isEmpty(), extension.plannedUserTaskChanges.toString());
    assertEquals(0, extension.publishedRightAway.get(), "a report was written into the outbox");

  }

  @SuppressWarnings("unchecked")
  private BusinessCockpitService<Object> service() {

    return new BusinessCockpitServiceFactory(extension).createService(new Context());

  }

  /**
   * VanillaBP's election while a <code>&#64;WorkflowStartedByBpms</code> method builds
   * {@link #NEW_AGGREGATE}. It wrote down nothing about either aggregate, and it counts every
   * question.
   */
  private static final class Election implements WorkflowElection {

    private final List<Object> askedWhetherInsideTheStart = new ArrayList<>();

    private final AtomicInteger startsLookedUp = new AtomicInteger();

    private final AtomicInteger openTasksLookedUp = new AtomicInteger();

    private final AtomicInteger elected = new AtomicInteger();

    @Override
    public boolean isInsideTheStartOf(
        final Object workflowAggregate) {

      askedWhetherInsideTheStart.add(workflowAggregate);
      return NEW_AGGREGATE.equals(workflowAggregate);

    }

    @Override
    public String adapterIdOfWorkflow(
        final String workflowModuleId,
        final String bpmnProcessId,
        final Object workflowAggregateId) {

      elected.incrementAndGet();
      return "other";

    }

    @Override
    public Optional<WorkflowStart> workflowStartOf(
        final String workflowModuleId,
        final String bpmnProcessId,
        final Object workflowAggregateId) {

      startsLookedUp.incrementAndGet();
      return Optional.empty();

    }

    @Override
    public List<OpenUserTask> openUserTasksOf(
        final String workflowModuleId,
        final String bpmnProcessId,
        final Object workflowAggregateId) {

      openTasksLookedUp.incrementAndGet();
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
   * test.
   */
  private static final class Extension extends BusinessCockpitExtension {

    private final List<String> plannedWorkflowChanges = new ArrayList<>();

    private final List<UserTaskReference> plannedUserTaskChanges = new ArrayList<>();

    private final AtomicInteger publishedRightAway = new AtomicInteger();

    private Extension(
        final List<BusinessCockpitBpmsBridge> bridges,
        final WorkflowElection election) {

      super(null, null, bridges, List.of(), noAggregateClasses(), null, null, null, () -> election);

    }

    /** VanillaBP's handlers, which this test never asks for anything. */
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

      plannedWorkflowChanges.add(workflowAggregateId);
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
    public boolean publishWorkflowEvent(
        final WorkflowReference workflow,
        final WorkflowEventKind eventKind,
        final String bpmsEventId,
        final OffsetDateTime timestamp,
        final EventTransaction transaction,
        final Class<?> workflowAggregateClass) {

      publishedRightAway.incrementAndGet();
      return true;

    }

    @Override
    public boolean publishUserTaskEvent(
        final UserTaskReference userTask,
        final UserTaskEventKind eventKind,
        final String bpmsEventId,
        final OffsetDateTime timestamp,
        final EventTransaction transaction,
        final Class<?> workflowAggregateClass) {

      publishedRightAway.incrementAndGet();
      return true;

    }

  }

  /** A BPMS half which counts how often it is asked about an aggregate. It knows nothing. */
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
