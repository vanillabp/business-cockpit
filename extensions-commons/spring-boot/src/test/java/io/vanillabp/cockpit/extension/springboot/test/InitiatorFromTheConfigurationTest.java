package io.vanillabp.cockpit.extension.springboot.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.AfterEach;
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
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.cockpit.extension.test.support.CockpitServer;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.Initiator;

/**
 * The other of the two answers: a workflow module which knows no action a user causes.
 * <p>
 * Nothing fails here. What a provider leaves empty is reported as the system, and what the
 * engine or a provider did name is left alone, so a module which switches to this setting does
 * not lose the values it already had.
 */
@SpringBootTest(classes = TestApplication.class,
    properties = {
        // an outbox store of its own, for the reason given in BusinessCockpitExtensionTest
        "spring.datasource.url=jdbc:h2:mem:cockpit-initiator-system;DB_CLOSE_DELAY=-1",
        // an inlined test property outranks the module's application.yaml
        "vanillabp.cockpit.initiator-source=system"
    })
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class InitiatorFromTheConfigurationTest {

  private static final String WORKFLOW_MODULE = "test-module";

  private static final String BPMN_PROCESS = "TestProcess";

  @DynamicPropertySource
  static void cockpitServer(
      final DynamicPropertyRegistry registry) {

    registry.add("vanillabp.cockpit.rest.base-url", CockpitServer::baseUrl);

  }

  @Autowired
  private TestWorkflowService workflowService;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private BusinessCockpitEventPublisher publisher;

  @Autowired
  private RecordingBpmsBridge bridge;

  @BeforeEach
  public void anEngineNamingNobody() {

    CockpitServer.forgetRequests();
    bridge.knowsTheTask(true);
    bridge.prefillsAnInitiator(false);

  }

  @AfterEach
  public void letTheEngineNameSomebodyAgain() {

    bridge.prefillsAnInitiator(true);

  }

  private TestAggregate aStartedWorkflow() {

    return transactions.execute(status -> {
      final var aggregate = new TestAggregate();
      aggregate.setCustomer("Anna");
      return workflowService.processes().startWorkflow(aggregate);
    });

  }

  @Test
  @DisplayName("A user task nobody named an initiator for is reported as the system's")
  public void aUserTaskWithoutAnInitiatorNamesTheSystem() {

    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                RecordingBpmsBridge
                    .userTask(
                        WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                        RecordingBpmsBridge.USER_TASK_ID, "approve"),
                UserTaskEventKind.CREATED, "bpms-event-system-1", OffsetDateTime.now(),
                EventTransaction.CURRENT));

    final var request = CockpitServer.awaitRequest("/usertask/created", "bpms-event-system-1");
    assertTrue(
        request.body().contains("\"initiator\":\""
            + Initiator.SYSTEM
            + "\""),
        request.body());

  }

  @Test
  @DisplayName("A workflow nobody named an initiator for is reported as the system's")
  public void aWorkflowWithoutAnInitiatorNamesTheSystem() {

    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> publisher
            .publishWorkflowEvent(
                new WorkflowReference(
                    RecordingBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, BPMN_PROCESS, RecordingBpmsBridge.PROCESS_VERSION, aggregate
                        .getId().toString(), RecordingBpmsBridge.WORKFLOW_ID),
                WorkflowEventKind.CREATED, "bpms-event-system-2", OffsetDateTime.now(),
                EventTransaction.CURRENT));

    final var request = CockpitServer.awaitRequest("/workflow/created", "bpms-event-system-2");
    assertTrue(
        request.body().contains("\"initiator\":\""
            + Initiator.SYSTEM
            + "\""),
        request.body());

  }

  @Test
  @DisplayName("What the engine prefilled survives this setting too")
  public void whatTheEnginePrefilledSurvives() {

    bridge.prefillsAnInitiator(true);
    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                RecordingBpmsBridge
                    .userTask(
                        WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                        RecordingBpmsBridge.USER_TASK_ID, "approve"),
                UserTaskEventKind.CREATED, "bpms-event-system-3", OffsetDateTime.now(),
                EventTransaction.CURRENT));

    final var request = CockpitServer.awaitRequest("/usertask/created", "bpms-event-system-3");
    assertTrue(
        request.body().contains("\"initiator\":\""
            + RecordingBpmsBridge.ENGINE_INITIATOR
            + "\""),
        request.body());

  }

}
