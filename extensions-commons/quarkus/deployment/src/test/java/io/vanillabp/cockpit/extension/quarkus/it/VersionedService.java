package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.Map;

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
 * A workflow service which reports one generation of its model at a time. Two methods share a
 * user task and two share the workflow, and what tells each pair apart is the version of the
 * deployed BPMN process the event came from.
 * <p>
 * It has no method for every user task on purpose. A task no method serves in the version at
 * hand has to fall back to what the BPMS reported, and a catch-all would hide that.
 */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = TestAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = VersionedService.BPMN_PROCESS))
public class VersionedService {

  /**
   * The BPMN process these providers belong to. It is the one the BPMS double deploys, because
   * only a deployed process has versions to pick a method by.
   */
  public static final String BPMN_PROCESS = "TestProcess";

  /** What the provider of the first generation writes into the details of a user task. */
  public static final String APPROVE_OF_THE_FIRST = "approve as the first model asked for";

  /** What the provider of every later generation writes there. */
  public static final String APPROVE_OF_THE_LATER = "approve as the later models ask for";

  /** What the provider matched by a version tag writes there. */
  public static final String ESCALATE_OF_THE_TAGGED = "escalate as the tagged model asks for";

  /** What the provider of the workflow's first generation writes into its details. */
  public static final String WORKFLOW_OF_THE_FIRST = "the workflow as the first model asked for";

  /** What the provider of the workflow's second generation writes there. */
  public static final String WORKFLOW_OF_THE_SECOND = "the workflow as the second model asks for";

  /** The task definition two providers share, one per generation of the model. */
  public static final String APPROVE = "approve";

  /** The task definition the provider matched by a version tag serves. */
  public static final String ESCALATE = "escalate";

  /** The task definition only a version nobody deploys is served for. */
  public static final String AUDIT = "audit";

  @Inject
  ProcessService<TestAggregate> processService;

  /**
   * @return The process service, so that the test can start a workflow
   */
  public ProcessService<TestAggregate> processes() {

    return processService;

  }

  /**
   * The task as the first deployed model asked for it.
   *
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @UserTaskDetailsProvider(taskDefinition = APPROVE, version = "1")
  public UserTaskDetails approveOfTheFirstGeneration(
      final PrefilledUserTaskDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", APPROVE_OF_THE_FIRST));
    return prefilled;

  }

  /**
   * The same task as every model deployed after the first asks for it. The two ranges do not
   * overlap, which is what lets both methods name the same task.
   *
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @UserTaskDetailsProvider(taskDefinition = APPROVE, version = ">1")
  public UserTaskDetails approveOfTheLaterGenerations(
      final PrefilledUserTaskDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", APPROVE_OF_THE_LATER));
    return prefilled;

  }

  /**
   * A method named for a version TAG rather than for a counted version. Which deployment
   * carries that tag is what the BPMS is asked, and the answer decides whether this method runs.
   *
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @UserTaskDetailsProvider(taskDefinition = ESCALATE,
      version = VersionsProcessVersions.TAG_OF_THE_THIRD)
  public UserTaskDetails escalateOfTheTaggedGeneration(
      final PrefilledUserTaskDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", ESCALATE_OF_THE_TAGGED));
    return prefilled;

  }

  /**
   * A method for a version this application will never see. Nothing about a details provider
   * would ever say so by itself, because a task without a provider is reported with what the
   * BPMS carried, so VanillaBP names it while the application boots.
   *
   * @param prefilled What the BPMS reported
   * @return The details nobody asks this method for
   */
  @UserTaskDetailsProvider(taskDefinition = AUDIT, version = "9")
  public UserTaskDetails auditOfAVersionNobodyDeploys(
      final PrefilledUserTaskDetails prefilled) {

    return prefilled;

  }

  /**
   * The workflow as the first deployed model asked for it.
   *
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @WorkflowDetailsProvider(version = "1")
  public WorkflowDetails workflowOfTheFirstGeneration(
      final PrefilledWorkflowDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", WORKFLOW_OF_THE_FIRST));
    return prefilled;

  }

  /**
   * The workflow as the second deployed model asks for it. A workflow provider stands for the
   * whole BPMN process, so the version is the only thing telling these two apart.
   *
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @WorkflowDetailsProvider(version = "2")
  public WorkflowDetails workflowOfTheSecondGeneration(
      final PrefilledWorkflowDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", WORKFLOW_OF_THE_SECOND));
    return prefilled;

  }

}
