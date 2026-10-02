package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.cockpit.extension.test.support.CockpitServer;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

/**
 * Every report names an initiator here as well, and this application answers for it itself.
 * <p>
 * It runs the same way through as the Spring Boot test of the same name, and it exists because a
 * neutral core being right says nothing about a platform's glue ever calling it.
 */
@ExtendWith(SuppressOutputExtension.class)
public class InitiatorIsAlwaysSetTest {

  private static final String WORKFLOW_MODULE = "test-module";

  private static final String BPMN_PROCESS = "TestProcess";

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
              .addClass(TestWorkflowService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class)
              .addClass(CockpitServer.class))
      .overrideRuntimeConfigKey("vanillabp.cockpit.rest.base-url", CockpitServer.baseUrl());

  @Inject
  TestWorkflowService workflowService;

  @Inject
  TestBpmsBridge bridge;

  @Inject
  BusinessCockpitEventPublisher publisher;

  @Inject
  UserTransaction transaction;

  @BeforeEach
  public void anEngineNamingNobody() {

    CockpitServer.forgetRequests();
    bridge.prefillsAnInitiator(false);

  }

  @AfterEach
  public void letTheEngineNameSomebodyAgain() {

    bridge.prefillsAnInitiator(true);

  }

  private TestAggregate aStartedWorkflow() throws Exception {

    transaction.begin();
    try {
      final var aggregate = new TestAggregate();
      aggregate.setCustomer("Anna");
      final var started = workflowService.processes().startWorkflow(aggregate);
      transaction.commit();
      return started;
    } catch (final RuntimeException e) {
      transaction.rollback();
      throw e;
    }

  }

  /**
   * @param report What raises the event, inside a transaction of its own
   * @return What the extension refused it with
   */
  private String refusalOf(
      final Runnable report) throws Exception {

    transaction.begin();
    try {
      return assertThrows(IllegalStateException.class, report::run).getMessage();
    } finally {
      transaction.rollback();
    }

  }

  @Test
  @DisplayName("A user task nobody named an initiator for fails the work it belongs to")
  public void aUserTaskWithoutAnInitiatorFails() throws Exception {

    final var aggregate = aStartedWorkflow();

    final var failure = refusalOf(
        () -> publisher
            .publishUserTaskEvent(
                TestBpmsBridge
                    .userTask(
                        WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                        TestBpmsBridge.USER_TASK_ID),
                UserTaskEventKind.CREATED, "bpms-event-initiator-1", OffsetDateTime.now(),
                EventTransaction.CURRENT));

    assertTrue(failure.contains(WORKFLOW_MODULE), failure);
    assertTrue(failure.contains(BPMN_PROCESS), failure);
    assertTrue(failure.contains("approve"), failure);
    assertTrue(failure.contains(UserTaskEventKind.CREATED.name()), failure);
    assertTrue(failure.contains("@UserTaskDetailsProvider"), failure);
    assertTrue(failure.contains("Initiator.SYSTEM"), failure);
    assertTrue(
        failure.contains("vanillabp.workflow-modules.test-module.cockpit.initiator-source"),
        failure);

  }

  @Test
  @DisplayName("A workflow nobody named an initiator for fails the same way")
  public void aWorkflowWithoutAnInitiatorFails() throws Exception {

    final var aggregate = aStartedWorkflow();

    final var failure = refusalOf(
        () -> publisher
            .publishWorkflowEvent(
                new WorkflowReference(
                    TestBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, BPMN_PROCESS, TestBpmsBridge.PROCESS_VERSION, aggregate
                        .getId().toString(), TestBpmsBridge.WORKFLOW_ID),
                WorkflowEventKind.CREATED, "bpms-event-initiator-2", OffsetDateTime.now(),
                EventTransaction.CURRENT));

    assertTrue(failure.contains(WORKFLOW_MODULE), failure);
    assertTrue(failure.contains(BPMN_PROCESS), failure);
    assertTrue(failure.contains("@WorkflowDetailsProvider"), failure);

  }

  @Test
  @DisplayName("What the engine prefilled reaches the cockpit untouched")
  public void whatTheEnginePrefilledReachesTheCockpit() throws Exception {

    bridge.prefillsAnInitiator(true);
    final var aggregate = aStartedWorkflow();

    transaction.begin();
    publisher
        .publishUserTaskEvent(
            TestBpmsBridge
                .userTask(
                    WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                    TestBpmsBridge.USER_TASK_ID),
            UserTaskEventKind.CREATED, "bpms-event-initiator-3", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    final var request = CockpitServer
        .awaitRequest("/usertask/created", "bpms-event-initiator-3");
    assertTrue(
        request.body().contains("\"initiator\":\""
            + TestBpmsBridge.ENGINE_INITIATOR
            + "\""),
        request.body());

  }

}
