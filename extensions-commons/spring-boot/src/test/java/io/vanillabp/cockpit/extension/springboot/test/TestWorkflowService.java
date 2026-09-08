package io.vanillabp.cockpit.extension.springboot.test;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import io.vanillabp.spi.cockpit.BusinessCockpitService;
import io.vanillabp.spi.cockpit.details.DetailsEvent;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;
import io.vanillabp.spi.process.ProcessService;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/**
 * The application under test: a workflow service reporting business data to the Business
 * Cockpit, with one details provider per way of matching a method.
 */
@Service
@WorkflowService(workflowAggregateClass = TestAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "TestProcess"))
public class TestWorkflowService {

  /** What the provider matched by the task definition writes into the details. */
  public static final String APPROVE_NOTE = "approved by the details provider";

  private final ProcessService<TestAggregate> processService;

  private final BusinessCockpitService<TestAggregate> businessCockpitService;

  public TestWorkflowService(
      final ProcessService<TestAggregate> processService,
      final BusinessCockpitService<TestAggregate> businessCockpitService) {

    this.processService = processService;
    this.businessCockpitService = businessCockpitService;

  }

  /**
   * @return The service injected the same way <code>ProcessService</code> is, which is what a
   *         test asserting the optional injection reads
   */
  public BusinessCockpitService<TestAggregate> businessCockpit() {

    return businessCockpitService;

  }

  /**
   * @return The process service, so that the test can start a workflow
   */
  public ProcessService<TestAggregate> processes() {

    return processService;

  }

  /**
   * Matched by the task definition of the user task. It writes into the object it was given
   * and into the workflow aggregate, which is what a details provider is for.
   *
   * @param aggregate The workflow aggregate, loaded by VanillaBP
   * @param prefilled What the BPMS reported, to be enriched
   * @param event What happened to the task
   * @return The very object it was given, which is the common case
   */
  @UserTaskDetailsProvider(taskDefinition = "approve")
  public UserTaskDetails approve(
      final TestAggregate aggregate,
      final PrefilledUserTaskDetails prefilled,
      @DetailsEvent final DetailsEvent.Event event) {

    aggregate.setNote(APPROVE_NOTE);
    prefilled.setDetails(Map.of("customer", aggregate.getCustomer(), "event", event.name()));
    prefilled.setCandidateGroups(List.of("approvers"));
    return prefilled;

  }

  /**
   * Matched by its own name, because the annotation names neither an element nor a task
   * definition. It answers with an object of its own, the other case a copy-back has to serve.
   *
   * @param prefilled What the BPMS reported
   * @return A details object built by the application
   */
  @UserTaskDetailsProvider
  public UserTaskDetails inspect(
      final PrefilledUserTaskDetails prefilled) {

    return new OwnUserTaskDetails(prefilled);

  }

  /**
   * Matched by the BPMN element id, which is the other of the two ways an annotation names what
   * a method serves.
   *
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @UserTaskDetailsProvider(id = "Activity_decide")
  public UserTaskDetails decideAboutTheOrder(
      final PrefilledUserTaskDetails prefilled) {

    prefilled.setDetails(Map.of("matchedBy", "the BPMN element id"));
    return prefilled;

  }

  /**
   * A details provider which is not public, the way a developer writes one who assumes the
   * annotation is enough. The scan reads the PUBLIC methods of a workflow service class, so
   * this method is invoked by nobody - and unlike an unserved <code>&#64;WorkflowTask</code>
   * nothing else would ever say so, since a user task without a provider is simply reported
   * with the details the BPMS carried. VanillaBP names it while the application boots.
   *
   * @param prefilled What the BPMS reported
   * @return The details nobody asks this method for
   */
  @UserTaskDetailsProvider(taskDefinition = "unseen")
  protected UserTaskDetails unseenByTheScan(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

  /**
   * The one provider a BPMN process may have for its workflow.
   *
   * @param aggregate The workflow aggregate
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @WorkflowDetailsProvider
  public WorkflowDetails workflowDetails(
      final TestAggregate aggregate,
      final PrefilledWorkflowDetails prefilled) {

    prefilled.setDetails(Map.of("customer", aggregate.getCustomer()));
    prefilled.setComment("workflow of "
        + aggregate.getCustomer());
    return prefilled;

  }

}
