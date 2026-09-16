package io.vanillabp.cockpit.extension.springboot.everytask;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.springboot.test.RecordingBpmsBridge;
import io.vanillabp.cockpit.extension.test.support.CockpitServer;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * One method reports what every user task of a workflow service has in common, and a method
 * naming a task reports what only that task has. Where two methods name the same task, one by
 * its task definition and one by its BPMN element id, the element id decides.
 * <p>
 * Everything is asserted where a user sees it, in the request the cockpit server receives. The
 * details say which method wrote them. The second BPMN process of the workflow service is part
 * of it, because a method claiming every user task claims the tasks of that process as well.
 */
@SpringBootTest(classes = EveryTaskApplication.class,
    properties = {
        // an outbox store of its own. The contexts of the other test classes stay cached with
        // their pollers running, and on a shared store one of them takes the entry away
        "spring.datasource.url=jdbc:h2:mem:cockpit-every-task;DB_CLOSE_DELAY=-1"
    })
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class OneProviderForEveryUserTaskTest {

  private static final String WORKFLOW_MODULE = "test-module";

  private static final String PRIMARY_PROCESS = "OrderProcess";

  private static final String SECONDARY_PROCESS = "ComplaintProcess";

  @DynamicPropertySource
  static void cockpitServer(
      final DynamicPropertyRegistry registry) {

    registry
        .add("vanillabp.cockpit.rest.base-url", CockpitServer::baseUrl);

  }

  @Autowired
  private OrderService workflowService;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private BusinessCockpitEventPublisher publisher;

  private OrderAggregate aggregate;

  @BeforeEach
  public void startAWorkflowAndForgetWhatArrivedBefore() {

    CockpitServer.forgetRequests();
    aggregate = transactions.execute(status -> {
      final var started = new OrderAggregate();
      started.setCustomer("Anna");
      return workflowService.processes().startWorkflow(started);
    });

  }

  /**
   * Raises one user task event and answers with what the cockpit server got for it.
   *
   * @param bpmnProcessId Which of the two processes of the workflow service the task is in
   * @param taskDefinition The task definition, which the BPMS double turns into the element
   *          id 'Activity_' plus this text
   * @param eventId What tells this event's request from the ones of the other tests. It is the
   *          task's id as well, because the outbox keeps one entry per task and kind. Tasks of
   *          their own are what lets these events stand next to each other
   * @return The body of the request the cockpit server received
   */
  private String reported(
      final String bpmnProcessId,
      final String taskDefinition,
      final String eventId) {

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                RecordingBpmsBridge
                    .userTask(
                        WORKFLOW_MODULE, bpmnProcessId, aggregate.getId().toString(), eventId,
                        taskDefinition),
                UserTaskEventKind.CREATED, eventId, OffsetDateTime.now(),
                EventTransaction.CURRENT));

    final var deadline = System.currentTimeMillis() + 10000;
    while (System.currentTimeMillis() < deadline) {
      final var reported = CockpitServer
          .received()
          .stream()
          .filter(request -> request.body().contains("\"id\":\"%s\"".formatted(eventId)))
          .findFirst();
      if (reported.isPresent()) {
        return reported.get().body();
      }
      sleep();
    }
    throw new AssertionError(
        "The event '%s' was not reported. Received: %s"
            .formatted(eventId, CockpitServer.received().stream().map(
                CockpitServer.Request::path).toList()));

  }

  private static void sleep() {

    try {
      Thread.sleep(250);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for the cockpit server", e);
    }

  }

  @Test
  @DisplayName("A task no method names is served by the method serving every user task")
  public void theMethodForEveryTaskServesWhatNobodyElseNames() {

    final var body = reported(PRIMARY_PROCESS, "somethingNobodyNames", "every-task-1");

    assertTrue(body.contains(OrderService.BY_EVERY_TASK), body);
    // it is a details provider like any other. It reads the workflow aggregate
    assertTrue(body.contains("\"customer\":\"Anna\""), body);

  }

  @Test
  @DisplayName("A task of the second BPMN process is served by it as well")
  public void theMethodForEveryTaskServesTheSecondProcessToo() {

    final var body = reported(SECONDARY_PROCESS, "somethingNobodyNames", "every-task-2");

    assertTrue(body.contains(OrderService.BY_EVERY_TASK), body);

  }

  @Test
  @DisplayName("A method naming the task definition wins over the one for every user task")
  public void namingTheTaskDefinitionWins() {

    final var body = reported(PRIMARY_PROCESS, "approve", "every-task-3");

    assertTrue(body.contains(OrderService.BY_THE_TASK_DEFINITION), body);

  }

  @Test
  @DisplayName("A method naming the BPMN element id wins over the one for every user task")
  public void namingTheElementIdWins() {

    final var body = reported(PRIMARY_PROCESS, "decide", "every-task-4");

    assertTrue(body.contains(OrderService.BY_THE_ELEMENT_ID), body);

  }

  @Test
  @DisplayName("Naming the element id wins over naming the task definition")
  public void theElementIdWinsOverTheTaskDefinition() {

    final var body = reported(PRIMARY_PROCESS, "escalate", "every-task-6");

    assertTrue(body.contains(OrderService.BY_THE_ELEMENT_ID), body);
    assertFalse(body.contains(OrderService.BY_THE_TASK_DEFINITION), body);

  }

  @Test
  @DisplayName("It wins whichever of the two methods was written first")
  public void theElementIdWinsWhicheverMethodComesFirst() {

    final var body = reported(PRIMARY_PROCESS, "cancel", "every-task-7");

    assertTrue(body.contains(OrderService.BY_THE_ELEMENT_ID), body);
    assertFalse(body.contains(OrderService.BY_THE_TASK_DEFINITION), body);

  }

  @Test
  @DisplayName("A method called like the task wins over the one for every user task")
  public void beingCalledLikeTheTaskWins() {

    final var body = reported(PRIMARY_PROCESS, "inspect", "every-task-5");

    assertTrue(body.contains(OrderService.BY_THE_METHOD_NAME), body);

  }

}
