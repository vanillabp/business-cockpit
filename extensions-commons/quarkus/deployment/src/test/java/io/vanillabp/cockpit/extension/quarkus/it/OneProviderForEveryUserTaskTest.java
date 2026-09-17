package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.test.support.CockpitServer;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

/**
 * One method reports what every user task of a workflow service has in common, and a method
 * naming a task reports what only that task has. Where two methods name the same task, one by
 * its task definition and one by its BPMN element id, the element id decides.
 * <p>
 * It asserts the same thing as the Spring Boot test of this name, because a neutral core being
 * right says nothing about a platform's glue ever calling it.
 */
@ExtendWith(SuppressOutputExtension.class)
public class OneProviderForEveryUserTaskTest {

  private static final String WORKFLOW_MODULE = "test-module";

  private static final String PRIMARY_PROCESS = "OrderProcess";

  private static final String SECONDARY_PROCESS = "ComplaintProcess";

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("business-cockpit.yaml", "application.yaml")
              .addAsResource("test-module/processes/dummy/TestProcess.bpmn")
              .addAsResource(
                  "workflow-module-descriptor/workflow-module", "META-INF/workflow-module")
              .addClass(TestAggregate.class)
              .addClass(TestAggregatePersistence.class)
              .addClass(EveryUserTaskService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class)
              // the test class runs in the application's class loader, so what it talks to the
              // cockpit server with has to be reachable from there as well
              .addClass(CockpitServer.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", CockpitServer.baseUrl());

  @Inject
  EveryUserTaskService workflowService;

  @Inject
  BusinessCockpitEventPublisher publisher;

  @Inject
  UserTransaction transaction;

  private TestAggregate aggregate;

  @BeforeEach
  public void startAWorkflowAndForgetWhatArrivedBefore() throws Exception {

    CockpitServer.forgetRequests();
    transaction.begin();
    try {
      final var started = new TestAggregate();
      started.setCustomer("Anna");
      aggregate = workflowService.processes().startWorkflow(started);
      transaction.commit();
    } catch (final RuntimeException e) {
      transaction.rollback();
      throw e;
    }

  }

  /**
   * Raises one user task event and answers with what the cockpit server got for it.
   *
   * @param bpmnProcessId Which of the two processes of the workflow service the task is in
   * @param taskDefinition The task definition, whose element id is 'Activity_' plus this text
   * @param eventId What tells this event's request from the ones of the other tests. It is the
   *          task's id as well, because the outbox keeps one entry per task and kind. Tasks of
   *          their own are what lets these events stand next to each other
   * @return The body of the request the cockpit server received
   * @throws Exception If the transaction around the event fails
   */
  private String reported(
      final String bpmnProcessId,
      final String taskDefinition,
      final String eventId) throws Exception {

    transaction.begin();
    publisher
        .publishUserTaskEvent(
            new UserTaskReference(
                TestBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, bpmnProcessId, TestBpmsBridge.PROCESS_VERSION, aggregate
                    .getId()
                    .toString(), TestBpmsBridge.WORKFLOW_ID, eventId, taskDefinition, "Activity_"
                        + taskDefinition),
            UserTaskEventKind.CREATED, eventId, OffsetDateTime.now(), EventTransaction.CURRENT);
    transaction.commit();

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
            .formatted(
                eventId,
                CockpitServer.received().stream().map(CockpitServer.Request::path).toList()));

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
  public void theMethodForEveryTaskServesWhatNobodyElseNames() throws Exception {

    final var body = reported(PRIMARY_PROCESS, "somethingNobodyNames", "every-task-1");

    assertTrue(body.contains(EveryUserTaskService.BY_EVERY_TASK), body);
    // it is a details provider like any other. It reads the workflow aggregate
    assertTrue(body.contains("\"customer\":\"Anna\""), body);

  }

  @Test
  @DisplayName("A task of the second BPMN process is served by it as well")
  public void theMethodForEveryTaskServesTheSecondProcessToo() throws Exception {

    final var body = reported(SECONDARY_PROCESS, "somethingNobodyNames", "every-task-2");

    assertTrue(body.contains(EveryUserTaskService.BY_EVERY_TASK), body);

  }

  @Test
  @DisplayName("A method naming the task definition wins over the one for every user task")
  public void namingTheTaskDefinitionWins() throws Exception {

    final var body = reported(PRIMARY_PROCESS, "approve", "every-task-3");

    assertTrue(body.contains(EveryUserTaskService.BY_THE_TASK_DEFINITION), body);

  }

  @Test
  @DisplayName("A method naming the BPMN element id wins over the one for every user task")
  public void namingTheElementIdWins() throws Exception {

    final var body = reported(PRIMARY_PROCESS, "decide", "every-task-4");

    assertTrue(body.contains(EveryUserTaskService.BY_THE_ELEMENT_ID), body);

  }

  @Test
  @DisplayName("Naming the element id wins over naming the task definition")
  public void theElementIdWinsOverTheTaskDefinition() throws Exception {

    final var body = reported(PRIMARY_PROCESS, "escalate", "every-task-6");

    assertTrue(body.contains(EveryUserTaskService.BY_THE_ELEMENT_ID), body);
    assertFalse(body.contains(EveryUserTaskService.BY_THE_TASK_DEFINITION), body);

  }

  @Test
  @DisplayName("It wins whichever of the two methods was written first")
  public void theElementIdWinsWhicheverMethodComesFirst() throws Exception {

    final var body = reported(PRIMARY_PROCESS, "cancel", "every-task-7");

    assertTrue(body.contains(EveryUserTaskService.BY_THE_ELEMENT_ID), body);
    assertFalse(body.contains(EveryUserTaskService.BY_THE_TASK_DEFINITION), body);

  }

  @Test
  @DisplayName("A method called like the task wins over the one for every user task")
  public void beingCalledLikeTheTaskWins() throws Exception {

    final var body = reported(PRIMARY_PROCESS, "inspect", "every-task-5");

    assertTrue(body.contains(EveryUserTaskService.BY_THE_METHOD_NAME), body);

  }

}
