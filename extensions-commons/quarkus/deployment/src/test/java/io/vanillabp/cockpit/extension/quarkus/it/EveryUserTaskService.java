package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.Map;

import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.process.ProcessService;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * A workflow service reporting the same thing for every user task it has, plus a method for
 * each task which needs more than that. It declares a second BPMN process as well, because
 * "every user task of this workflow service" means the tasks of that one too.
 * <p>
 * Two of its user tasks are named twice, once by their task definition and once by their
 * element id, which is what the order between the two keys is read from.
 */
@ApplicationScoped
@WorkflowService(workflowAggregateClass = TestAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "OrderProcess"),
    secondaryBpmnProcesses = @BpmnProcess(bpmnProcessId = "ComplaintProcess"))
public class EveryUserTaskService {

  /** What the method serving every user task writes into the details. */
  public static final String BY_EVERY_TASK = "the method for every user task";

  /** What the method naming a task definition writes into the details. */
  public static final String BY_THE_TASK_DEFINITION = "the method for the task definition";

  /** What the method naming a BPMN element id writes into the details. */
  public static final String BY_THE_ELEMENT_ID = "the method for the element id";

  /** What the method called like its task writes into the details. */
  public static final String BY_THE_METHOD_NAME = "the method called like the task";

  @Inject
  ProcessService<TestAggregate> processService;

  /**
   * @return The process service, so that the test can start a workflow
   */
  public ProcessService<TestAggregate> processes() {

    return processService;

  }

  /**
   * The one method this story is about: it reports the same columns for every user task of
   * both BPMN processes, and it steps aside wherever a method below names the task.
   *
   * @param aggregate The workflow aggregate, loaded by VanillaBP
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @UserTaskDetailsProvider(taskDefinition = UserTaskDetailsProvider.ALL)
  public UserTaskDetails everyUserTask(
      final TestAggregate aggregate,
      final PrefilledUserTaskDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", BY_EVERY_TASK, "customer", aggregate.getCustomer()));
    return prefilled;

  }

  /**
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @UserTaskDetailsProvider(taskDefinition = "approve")
  public UserTaskDetails approve(
      final PrefilledUserTaskDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", BY_THE_TASK_DEFINITION));
    return prefilled;

  }

  /**
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @UserTaskDetailsProvider(id = "Activity_decide")
  public UserTaskDetails decideAboutTheOrder(
      final PrefilledUserTaskDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", BY_THE_ELEMENT_ID));
    return prefilled;

  }

  /**
   * Names a task the method below names as well, by the other key. The element id is the
   * stronger of the two, so the method below is the one which runs.
   *
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @UserTaskDetailsProvider(taskDefinition = "escalate")
  public UserTaskDetails escalate(
      final PrefilledUserTaskDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", BY_THE_TASK_DEFINITION));
    return prefilled;

  }

  /**
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @UserTaskDetailsProvider(id = "Activity_escalate")
  public UserTaskDetails escalateByItsElementId(
      final PrefilledUserTaskDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", BY_THE_ELEMENT_ID));
    return prefilled;

  }

  /**
   * The same pair once more, written the other way round. The scan reads the methods of this
   * class in an order nobody promises, so a test which only ever saw one order would not say
   * much.
   *
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @UserTaskDetailsProvider(id = "Activity_cancel")
  public UserTaskDetails cancelByItsElementId(
      final PrefilledUserTaskDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", BY_THE_ELEMENT_ID));
    return prefilled;

  }

  /**
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @UserTaskDetailsProvider(taskDefinition = "cancel")
  public UserTaskDetails cancel(
      final PrefilledUserTaskDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", BY_THE_TASK_DEFINITION));
    return prefilled;

  }

  /**
   * The convention this story leaves untouched: an annotation naming nothing stands for the
   * task called like the method.
   *
   * @param prefilled What the BPMS reported
   * @return The enriched details
   */
  @UserTaskDetailsProvider
  public UserTaskDetails inspect(
      final PrefilledUserTaskDetails prefilled) {

    prefilled.setDetails(Map.of("servedBy", BY_THE_METHOD_NAME));
    return prefilled;

  }

}
