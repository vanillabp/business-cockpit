package io.vanillabp.cockpit.extension.springboot.everytask;

import java.util.Map;

import org.springframework.stereotype.Service;

import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.process.ProcessService;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;

/**
 * A workflow service reporting the same thing for every user task it has, plus a method for
 * each task which needs more than that. It declares a second BPMN process as well, because
 * "every user task of this workflow service" means the tasks of that one too.
 */
@Service
@WorkflowService(workflowAggregateClass = OrderAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "OrderProcess"),
    secondaryBpmnProcesses = @BpmnProcess(bpmnProcessId = "ComplaintProcess"))
public class OrderService {

  /** What the method serving every user task writes into the details. */
  public static final String BY_EVERY_TASK = "the method for every user task";

  /** What the method naming a task definition writes into the details. */
  public static final String BY_THE_TASK_DEFINITION = "the method for the task definition";

  /** What the method naming a BPMN element id writes into the details. */
  public static final String BY_THE_ELEMENT_ID = "the method for the element id";

  /** What the method called like its task writes into the details. */
  public static final String BY_THE_METHOD_NAME = "the method called like the task";

  private final ProcessService<OrderAggregate> processService;

  public OrderService(
      final ProcessService<OrderAggregate> processService) {

    this.processService = processService;

  }

  /**
   * @return The process service, so that the test can start a workflow
   */
  public ProcessService<OrderAggregate> processes() {

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
      final OrderAggregate aggregate,
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
