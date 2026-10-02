package io.vanillabp.cockpit.extension.springboot.test;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
 * Every report names an initiator, and this application answers for it itself.
 * <p>
 * The engine of this test prefills one, the way Camunda 7 does for a case. Switching that off is
 * how the other two workflow systems report, and it is what shows the rule: a report which names
 * nobody fails the work the event belongs to, instead of arriving without the one field a reader
 * sorts and recognizes cases by.
 */
@SpringBootTest(classes = TestApplication.class,
    properties = {
        // an outbox store of its own, for the reason given in BusinessCockpitExtensionTest
        "spring.datasource.url=jdbc:h2:mem:cockpit-initiator-by-application;DB_CLOSE_DELAY=-1"
    })
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class InitiatorIsAlwaysSetTest {

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
    bridge.knowsTheTask(true);

  }

  private TestAggregate aStartedWorkflow() {

    return transactions.execute(status -> {
      final var aggregate = new TestAggregate();
      aggregate.setCustomer("Anna");
      return workflowService.processes().startWorkflow(aggregate);
    });

  }

  private WorkflowReference workflowOf(
      final TestAggregate aggregate) {

    return new WorkflowReference(
        RecordingBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, BPMN_PROCESS, RecordingBpmsBridge.PROCESS_VERSION, aggregate
            .getId().toString(), RecordingBpmsBridge.WORKFLOW_ID);

  }

  @Test
  @DisplayName("A user task nobody named an initiator for fails the work it belongs to")
  public void aUserTaskWithoutAnInitiatorFails() {

    final var aggregate = aStartedWorkflow();

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> transactions
            .executeWithoutResult(status -> publisher
                .publishUserTaskEvent(
                    RecordingBpmsBridge
                        .userTask(
                            WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                            RecordingBpmsBridge.USER_TASK_ID, "no-provider-at-all"),
                    UserTaskEventKind.CREATED, "bpms-event-initiator-1", OffsetDateTime.now(),
                    EventTransaction.CURRENT)))
        .getMessage();

    assertTrue(failure.contains(WORKFLOW_MODULE), failure);
    assertTrue(failure.contains(BPMN_PROCESS), failure);
    assertTrue(failure.contains("no-provider-at-all"), failure);
    assertTrue(failure.contains(UserTaskEventKind.CREATED.name()), failure);
    // both ways on, and the key which says why this is refused at all
    assertTrue(failure.contains("@UserTaskDetailsProvider"), failure);
    assertTrue(failure.contains("Initiator.SYSTEM"), failure);
    assertTrue(
        failure.contains("vanillabp.workflow-modules.test-module.cockpit.initiator-source"),
        failure);

  }

  @Test
  @DisplayName("A workflow nobody named an initiator for fails the same way")
  public void aWorkflowWithoutAnInitiatorFails() {

    final var aggregate = aStartedWorkflow();

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> transactions
            .executeWithoutResult(status -> publisher
                .publishWorkflowEvent(
                    workflowOf(aggregate), WorkflowEventKind.CREATED,
                    "bpms-event-initiator-2", OffsetDateTime.now(), EventTransaction.CURRENT)))
        .getMessage();

    assertTrue(failure.contains(WORKFLOW_MODULE), failure);
    assertTrue(failure.contains(BPMN_PROCESS), failure);
    assertTrue(failure.contains(WorkflowEventKind.CREATED.name()), failure);
    assertTrue(failure.contains("@WorkflowDetailsProvider"), failure);

  }

  @Test
  @DisplayName("Reading a task back refuses it as well, although nothing is reported")
  public void readingATaskRefusesItAsWell() {

    final var aggregate = aStartedWorkflow();

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> transactions
            .executeWithoutResult(status -> workflowService
                .businessCockpit()
                .getUserTask(aggregate, RecordingBpmsBridge.USER_TASK_ID)))
        .getMessage();

    // the task the engine answers with for a read is 'approve', whose provider enriches the
    // details and says nothing about the initiator
    assertTrue(failure.contains("approve"), failure);
    assertTrue(failure.contains("Initiator.SYSTEM"), failure);

  }

  @Test
  @DisplayName("A provider which names the constant is answer enough")
  public void theConstantIsAnAnswer() {

    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                RecordingBpmsBridge
                    .userTask(
                        WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                        RecordingBpmsBridge.USER_TASK_ID, "archive"),
                UserTaskEventKind.CREATED, "bpms-event-initiator-3", OffsetDateTime.now(),
                EventTransaction.CURRENT));

    final var request = CockpitServer
        .awaitRequest("/usertask/created", "bpms-event-initiator-3");
    assertTrue(
        request.body().contains("\"initiator\":\""
            + Initiator.SYSTEM
            + "\""),
        request.body());

  }

  @Test
  @DisplayName("What the engine prefilled stays, and the provider is asked all the same")
  public void whatTheEnginePrefilledStays() {

    bridge.prefillsAnInitiator(true);
    final var aggregate = aStartedWorkflow();

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                RecordingBpmsBridge
                    .userTask(
                        WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                        RecordingBpmsBridge.USER_TASK_ID, "approve"),
                UserTaskEventKind.CREATED, "bpms-event-initiator-4", OffsetDateTime.now(),
                EventTransaction.CURRENT));

    final var request = CockpitServer
        .awaitRequest("/usertask/created", "bpms-event-initiator-4");
    assertTrue(
        request.body().contains("\"initiator\":\""
            + RecordingBpmsBridge.ENGINE_INITIATOR
            + "\""),
        request.body());

  }

  @Test
  @DisplayName("An end the BPMS says nothing about is reported, and it names the system")
  public void anEndWithoutDetailsNamesTheSystem() {

    final var aggregate = aStartedWorkflow();
    bridge.knowsTheTask(false);

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                RecordingBpmsBridge
                    .userTask(
                        WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                        RecordingBpmsBridge.USER_TASK_ID, "approve"),
                UserTaskEventKind.COMPLETED, "bpms-event-initiator-5", OffsetDateTime.now(),
                EventTransaction.CURRENT));

    final var request = CockpitServer
        .awaitRequest("/usertask/task-1/completed", "bpms-event-initiator-5");
    assertTrue(
        request.body().contains("\"initiator\":\""
            + Initiator.SYSTEM
            + "\""),
        request.body());

  }

}
