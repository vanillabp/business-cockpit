package io.vanillabp.cockpit.extension.quarkus.it;

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
import io.vanillabp.cockpit.extension.test.support.CockpitServer;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.Initiator;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

/**
 * The other of the two answers on this platform: a workflow module which knows no action a user
 * causes reports the system instead of failing.
 */
@ExtendWith(SuppressOutputExtension.class)
public class InitiatorFromTheConfigurationTest {

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
      .overrideRuntimeConfigKey("vanillabp.cockpit.rest.base-url", CockpitServer.baseUrl())
      .overrideRuntimeConfigKey("vanillabp.cockpit.initiator-source", "system");

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

  @Test
  @DisplayName("A user task nobody named an initiator for is reported as the system's")
  public void aUserTaskWithoutAnInitiatorNamesTheSystem() throws Exception {

    final var aggregate = aStartedWorkflow();

    transaction.begin();
    publisher
        .publishUserTaskEvent(
            TestBpmsBridge
                .userTask(
                    WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                    TestBpmsBridge.USER_TASK_ID),
            UserTaskEventKind.CREATED, "bpms-event-system-1", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    final var request = CockpitServer.awaitRequest("/usertask/created", "bpms-event-system-1");
    assertTrue(
        request.body().contains("\"initiator\":\""
            + Initiator.SYSTEM
            + "\""),
        request.body());

  }

}
