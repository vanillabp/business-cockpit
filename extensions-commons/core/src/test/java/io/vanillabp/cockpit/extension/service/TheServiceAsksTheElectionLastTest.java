package io.vanillabp.cockpit.extension.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
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
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.BusinessCockpitService;

/**
 * <code>BusinessCockpitService.aggregateChanged(aggregate)</code> and
 * <code>getUserTask</code> find the BPMS half without the election where VanillaBP already
 * knows the adapter: from the note of the start, from the only adapter of the application, or
 * from the open task asked about. The election may wait for a BPMS inside the caller's
 * transaction, so it comes last.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TheServiceAsksTheElectionLastTest {

  private static final String MODULE_ID = "module";

  private static final String PROCESS_ID = "Main";

  private static final String AGGREGATE_ID = "aggregate-1";

  private static final String CASE = "100";

  private final Election election = new Election();

  private final Half camunda8 = new Half("c8");

  private final Half other = new Half("other");

  private final Extension extension = new Extension(List.of(camunda8, other));

  @Test
  @DisplayName("The change of an aggregate with a note of the start does not ask the election")
  public void theChangeOfAnAggregateWithANoteDoesNotAskTheElection() {

    election.start = Optional.of(new WorkflowStart("other", CASE, "3", true));

    service().aggregateChanged(AGGREGATE_ID);

    assertEquals(1, other.asked.get(), "the half the note names was not asked");
    assertEquals(0, camunda8.asked.get());
    assertEquals(0, election.elected.get(), "the election was asked although the note names the adapter");

  }

  @Test
  @DisplayName("The change of an aggregate in an application with one adapter does not ask the election")
  public void theChangeOfAnAggregateWithOneAdapterDoesNotAskTheElection() {

    extension.only = Optional.of(other);

    service().aggregateChanged(AGGREGATE_ID);

    assertEquals(1, other.asked.get(), "the only half was not asked");
    assertEquals(0, election.elected.get(), "the election was asked although there is only one adapter");

  }

  @Test
  @DisplayName("A task read with a note of the start does not ask the election")
  public void aTaskReadWithANoteDoesNotAskTheElection() {

    election.start = Optional.of(new WorkflowStart("other", CASE, "3", true));

    service().getUserTask(AGGREGATE_ID, "11");

    assertEquals(1, other.asked.get(), "the half the note names was not asked");
    assertEquals(0, camunda8.asked.get());
    assertEquals(0, election.elected.get(), "the election was asked although the note names the adapter");

  }

  @Test
  @DisplayName("A task read in an application with one adapter does not ask the election")
  public void aTaskReadWithOneAdapterDoesNotAskTheElection() {

    extension.only = Optional.of(other);

    service().getUserTask(AGGREGATE_ID, "11");

    assertEquals(1, other.asked.get(), "the only half was not asked");
    assertEquals(0, election.elected.get(), "the election was asked although there is only one adapter");

  }

  @Test
  @DisplayName("A task read without a note takes the adapter which delivered that open task")
  public void aTaskReadTakesTheAdapterOfTheOpenTask() {

    election.open = List
        .of(
            new OpenUserTask("c8", null, CASE, PROCESS_ID, "12", "check", "Check", "3"),
            new OpenUserTask("other", null, CASE, PROCESS_ID, "11", "approve", "Approve", "3"));

    service().getUserTask(AGGREGATE_ID, "11");

    assertEquals(1, other.asked.get(), "the half which delivered the task was not asked");
    assertEquals(0, camunda8.asked.get(), "the half of another open task was asked");
    assertEquals(0, election.elected.get(), "the election was asked although the open task names the adapter");

  }

  @Test
  @DisplayName("A task read asks the election where nothing VanillaBP wrote down names the adapter")
  public void aTaskReadAsksTheElectionWhereNothingNamesTheAdapter() {

    // an open task, but not the one asked about: its adapter says nothing about task 11
    election.open = List
        .of(new OpenUserTask("other", null, CASE, PROCESS_ID, "12", "check", "Check", "3"));

    service().getUserTask(AGGREGATE_ID, "11");

    assertEquals(1, election.elected.get(), "the election was not asked");
    assertEquals(1, camunda8.asked.get(), "the half the election names was not asked");
    assertEquals(0, other.asked.get());

  }

  @SuppressWarnings("unchecked")
  private BusinessCockpitService<Object> service() {

    return new BusinessCockpitServiceFactory(extension).createService(new Context());

  }

  /** What VanillaBP wrote down about the workflow and its tasks. Its election answers "c8". */
  private static final class Election implements WorkflowElection {

    private Optional<WorkflowStart> start = Optional.empty();

    private List<OpenUserTask> open = List.of();

    private final AtomicInteger elected = new AtomicInteger();

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

  /**
   * Knows the two halves and nothing else. Reports are switched off, which keeps the
   * transaction check out of this test, and a read runs without a transaction.
   */
  private static final class Extension extends BusinessCockpitExtension {

    private Optional<BusinessCockpitBpmsBridge> only = Optional.empty();

    private Extension(
        final List<BusinessCockpitBpmsBridge> bridges) {

      super(null, null, bridges, List.of(), null, null, null, null);

    }

    @Override
    public boolean reportsWorkflows() {

      return false;

    }

    @Override
    public Optional<BusinessCockpitBpmsBridge> theOnlyBridge() {

      return only;

    }

    @Override
    public <T> T readInOneTransaction(
        final Class<?> workflowAggregateClass,
        final Supplier<T> question) {

      return question.get();

    }

  }

  /**
   * A BPMS half which counts how often it is asked about an aggregate. It knows no workflow and
   * no task, so nothing is reported and no details are read.
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
