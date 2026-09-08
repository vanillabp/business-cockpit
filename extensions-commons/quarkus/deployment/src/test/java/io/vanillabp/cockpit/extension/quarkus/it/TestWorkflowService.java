package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.List;
import java.util.Map;

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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The application under test: a workflow service reporting business data to the Business
 * Cockpit, the Quarkus twin of the Spring Boot module's test application.
 */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = TestAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "TestProcess"))
public class TestWorkflowService {

  /** What the provider matched by the task definition writes into the aggregate. */
  public static final String APPROVE_NOTE = "approved by the details provider";

  @Inject
  ProcessService<TestAggregate> processService;

  @Inject
  BusinessCockpitService<TestAggregate> businessCockpitService;

  /**
   * @return The service injected the way <code>ProcessService</code> is
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
   * @param aggregate The workflow aggregate, loaded by VanillaBP
   * @param prefilled What the BPMS reported
   * @param event What happened to the task
   * @return The very object it was given
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
