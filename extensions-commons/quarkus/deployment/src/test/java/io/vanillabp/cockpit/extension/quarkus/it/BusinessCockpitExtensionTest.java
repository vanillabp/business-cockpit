package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;

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
 * The Business Cockpit extension inside a booted Quarkus application, from the event a BPMS
 * reported to the request the cockpit server receives.
 * <p>
 * It runs the same way through as the Spring Boot test of this extension, and it exists
 * because a neutral core being right says nothing about a platform's glue ever calling it.
 */
@ExtendWith(SuppressOutputExtension.class)
public class BusinessCockpitExtensionTest {

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
              // the test class runs in the application's class loader, so what it talks to the
              // cockpit server with has to be reachable from there as well. A second copy of the
              // class is the consequence, and asking the running server over HTTP is what makes
              // the two copies agree on what arrived.
              .addClass(CockpitServer.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", CockpitServer.baseUrl());

  @Inject
  TestWorkflowService workflowService;

  @Inject
  TestAggregatePersistence aggregates;

  @Inject
  TestBpmsBridge bridge;

  @Inject
  BusinessCockpitEventPublisher publisher;

  @Inject
  UserTransaction transaction;

  @BeforeEach
  public void forgetWhatArrivedBefore() {

    CockpitServer.forgetRequests();
    bridge.forgetLookups();

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
  @DisplayName("A user task the BPMS reported reaches the cockpit, enriched by the application")
  public void aReportedUserTaskReachesTheCockpit() throws Exception {

    final var aggregate = aStartedWorkflow();

    transaction.begin();
    publisher
        .publishUserTaskEvent(
            TestBpmsBridge
                .userTask(
                    WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                    TestBpmsBridge.USER_TASK_ID),
            UserTaskEventKind.CREATED, "bpms-event-1", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    final var request = CockpitServer.awaitRequest("/usertask/created", "bpms-event-1");
    assertTrue(request.body().contains("\"id\":\"bpms-event-1\""), request.body());
    assertTrue(request.body().contains("\"customer\":\"Anna\""), request.body());
    assertTrue(request.body().contains("\"event\":\"CREATED\""), request.body());
    assertTrue(request.body().contains("\"candidateGroups\":[\"approvers\"]"), request.body());
    assertTrue(request.body().contains("Approve the order"), request.body());

  }

  @Test
  @DisplayName("The platform saves nothing after a user-task details provider, and this store keeps the change regardless")
  public void aUserTaskProviderIsNotSavedForByThePlatform() throws Exception {

    final var aggregate = aStartedWorkflow();
    aggregates.forgetSaves();

    transaction.begin();
    publisher
        .publishUserTaskEvent(
            TestBpmsBridge
                .userTask(
                    WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                    TestBpmsBridge.USER_TASK_ID),
            UserTaskEventKind.CREATED, "bpms-event-6", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    CockpitServer.awaitRequestOf("/usertask/created", "bpms-event-6");
    assertFalse(
        aggregates.sawSaveOf(aggregate.getId()),
        "the platform saved the aggregate a details provider was handed");
    // and the note the provider wrote is readable from the store all the same. This
    // application keeps its aggregates in a map and hands out the very object it holds, so a
    // change to it is the stored state by the time it is made. This is what the test found,
    // not a promise anybody makes. What an unsaved change costs is a question to the
    // persistence, and every persistence answers it differently.
    assertEquals(
        TestWorkflowService.APPROVE_NOTE, aggregates.byId(aggregate.getId()).getNote());

  }

  @Test
  @DisplayName("The platform saves nothing after a workflow details provider either")
  public void aWorkflowProviderIsNotSavedForByThePlatform() throws Exception {

    final var aggregate = aStartedWorkflow();
    aggregates.forgetSaves();

    transaction.begin();
    publisher
        .publishWorkflowEvent(
            new WorkflowReference(
                TestBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, BPMN_PROCESS, TestBpmsBridge.PROCESS_VERSION, aggregate
                    .getId()
                    .toString(), TestBpmsBridge.WORKFLOW_ID),
            WorkflowEventKind.CREATED, "bpms-event-7", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    CockpitServer.awaitRequestOf("/workflow/created", "bpms-event-7");
    assertFalse(
        aggregates.sawSaveOf(aggregate.getId()),
        "the platform saved the aggregate a workflow details provider was handed");
    assertEquals(
        TestWorkflowService.WORKFLOW_NOTE, aggregates.byId(aggregate.getId()).getWorkflowNote());

  }

  @Test
  @DisplayName("An event reported outside a transaction gets one of its own")
  public void anEventReportedWithoutATransactionGetsOne() throws Exception {

    final var aggregate = aStartedWorkflow();

    publisher
        .publishWorkflowEvent(
            new WorkflowReference(
                TestBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, BPMN_PROCESS, TestBpmsBridge.PROCESS_VERSION, aggregate
                    .getId()
                    .toString(), TestBpmsBridge.WORKFLOW_ID),
            WorkflowEventKind.CREATED, "bpms-event-2", OffsetDateTime.now(),
            EventTransaction.NEW);

    final var request = CockpitServer.awaitRequest("/workflow/created", "bpms-event-2");
    assertTrue(request.body().contains("\"customer\":\"Anna\""), request.body());
    assertTrue(request.body().contains("workflow of Anna"), request.body());

  }

  @Test
  @DisplayName("A report carries the state of its event and not the state of the moment it is sent")
  public void aReportCarriesTheStateOfItsEvent() throws Exception {

    final var aggregate = aStartedWorkflow();
    // the first attempt is refused, so the report leaves this application after the case below
    // was changed. Whichever moment the outbox gets to it, the report was written at the event
    CockpitServer.refuseRequestsAbout("bpms-event-22", 1);

    transaction.begin();
    publisher
        .publishUserTaskEvent(
            TestBpmsBridge
                .userTask(
                    WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                    TestBpmsBridge.USER_TASK_ID),
            UserTaskEventKind.CREATED, "bpms-event-22", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    transaction.begin();
    aggregates.byId(aggregate.getId()).setCustomer("Berta");
    transaction.commit();

    final var request = CockpitServer.awaitRequest("/usertask/created", "bpms-event-22");
    assertTrue(request.body().contains("\"customer\":\"Anna\""), request.body());
    assertFalse(request.body().contains("Berta"), request.body());
    assertEquals("Berta", aggregates.byId(aggregate.getId()).getCustomer());

  }

  @Test
  @DisplayName("A completed task is reported with the business data it was completed with")
  public void aCompletedTaskCarriesItsBusinessData() throws Exception {

    final var aggregate = aStartedWorkflow();

    transaction.begin();
    publisher
        .publishUserTaskEvent(
            TestBpmsBridge
                .userTask(
                    WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                    TestBpmsBridge.USER_TASK_ID),
            UserTaskEventKind.COMPLETED, "bpms-event-8", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    final var request = CockpitServer.awaitAnyRequest("/usertask/task-1/completed");
    assertTrue(request.body().contains("\"customer\":\"Anna\""), request.body());
    assertTrue(request.body().contains("\"event\":\"COMPLETED\""), request.body());

  }

  @Test
  @DisplayName("A finished workflow is reported with the business data it ended with")
  public void aFinishedWorkflowCarriesItsBusinessData() throws Exception {

    final var aggregate = aStartedWorkflow();

    transaction.begin();
    publisher
        .publishWorkflowEvent(
            new WorkflowReference(
                TestBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, BPMN_PROCESS, TestBpmsBridge.PROCESS_VERSION, aggregate
                    .getId()
                    .toString(), TestBpmsBridge.WORKFLOW_ID),
            WorkflowEventKind.COMPLETED, "bpms-event-9", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    final var request = CockpitServer.awaitAnyRequest("/workflow/workflow-1/completed");
    assertTrue(request.body().contains("\"customer\":\"Anna\""), request.body());
    assertTrue(request.body().contains("workflow of Anna"), request.body());

  }

  @Test
  @DisplayName("aggregateChanged reports the workflows of the aggregate")
  public void aggregateChangedReportsTheWorkflows() throws Exception {

    final var aggregate = aStartedWorkflow();

    transaction.begin();
    workflowService.businessCockpit().aggregateChanged(aggregate);
    transaction.commit();

    final var request = CockpitServer.awaitAnyRequest("/workflow/workflow-1/updated");
    assertTrue(request.body().contains("\"updated\":true"), request.body());

  }

  @Test
  @DisplayName("aggregateChanged with task ids reports those tasks")
  public void aggregateChangedReportsTheNamedTasks() throws Exception {

    final var aggregate = aStartedWorkflow();

    transaction.begin();
    workflowService.businessCockpit().aggregateChanged(aggregate, TestBpmsBridge.USER_TASK_ID);
    transaction.commit();

    final var request = CockpitServer.awaitAnyRequest("/usertask/task-1/updated");
    assertTrue(request.body().contains("\"customer\":\"Anna\""), request.body());

  }

  @Test
  @DisplayName("getUserTask answers what the cockpit would show, and reports nothing")
  public void getUserTaskAnswersWithoutReporting() throws Exception {

    final var aggregate = aStartedWorkflow();

    final var userTask = workflowService
        .businessCockpit()
        .getUserTask(aggregate, TestBpmsBridge.USER_TASK_ID);

    assertNotNull(userTask);
    assertTrue(userTask.isPresent());
    assertEquals("Anna", userTask.get().getDetails().get("customer"));
    assertEquals("Approve the order", userTask.get().getTitle().get("en"));
    CockpitServer.awaitQuiet();
    assertTrue(
        CockpitServer
            .received()
            .stream()
            .noneMatch(request -> request.path().contains("usertask")),
        "reading a user task reported something to the cockpit");

  }

  @Test
  @DisplayName("A read answers with what the caller changed and has not written yet")
  public void getUserTaskSeesWhatTheCallerHasNotWrittenYet() throws Exception {

    final var aggregate = aStartedWorkflow();
    aggregates.forgetSaves();

    transaction.begin();
    aggregates.byId(aggregate.getId()).setCustomer("Bea");
    final var userTask = workflowService
        .businessCockpit()
        .getUserTask(aggregate, TestBpmsBridge.USER_TASK_ID);
    transaction.commit();

    assertNotNull(userTask);
    assertTrue(userTask.isPresent());
    assertEquals("Bea", userTask.get().getDetails().get("customer"));
    assertEquals(List.of(Boolean.TRUE), bridge.tasksLookedUpInATransaction());
    assertFalse(
        aggregates.sawSaveOf(aggregate.getId()),
        "reading a user task saved the workflow aggregate");

  }

  @Test
  @DisplayName("A read from a caller without a transaction gets an answer")
  public void getUserTaskWithoutATransactionAnswers() throws Exception {

    final var aggregate = aStartedWorkflow();

    // this is what a REST endpoint does. It asks about a task and has opened nothing
    final var userTask = workflowService
        .businessCockpit()
        .getUserTask(aggregate, TestBpmsBridge.USER_TASK_ID);

    assertNotNull(userTask);
    assertTrue(userTask.isPresent());
    assertEquals("Anna", userTask.get().getDetails().get("customer"));
    // the read opened one of its own, so the engine and the aggregate were asked in one unit of
    // work here as well
    assertEquals(List.of(Boolean.TRUE), bridge.tasksLookedUpInATransaction());

  }

  @Test
  @DisplayName("A report from a caller without a transaction says what to do about it")
  public void aggregateChangedWithoutATransactionSaysWhatToDo() throws Exception {

    final var aggregate = aStartedWorkflow();

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> workflowService.businessCockpit().aggregateChanged(aggregate));

    assertTrue(failure.getMessage().contains("needs a transaction"), failure.getMessage());
    assertTrue(failure.getMessage().contains("@Transactional"), failure.getMessage());
    assertTrue(failure.getMessage().contains(BPMN_PROCESS), failure.getMessage());
    // the sentence about a BPMS half reporting from a worker thread belongs to an adapter and
    // would send an application looking in the wrong place
    assertFalse(failure.getMessage().contains("EventTransaction"), failure.getMessage());

  }

  @Test
  @DisplayName("A report which was to ride the caller's transaction says so where there is none")
  public void aReportWithoutATransactionIsRefused() throws Exception {

    final var aggregate = aStartedWorkflow();

    // the runner the workflow aggregate's writes go through demands the transaction rather
    // than opening one behind the caller's back, and the extension says what that means for a
    // BPMS half in front of the platform's own wording
    final var failure = assertThrows(
        IllegalStateException.class,
        () -> publisher
            .publishUserTaskEvent(
                TestBpmsBridge
                    .userTask(
                        WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                        TestBpmsBridge.USER_TASK_ID),
                UserTaskEventKind.CREATED, "bpms-event-3", OffsetDateTime.now(),
                EventTransaction.CURRENT));

    assertTrue(failure.getMessage().contains("EventTransaction.CURRENT"), failure.getMessage());
    assertTrue(failure.getMessage().contains("EventTransaction.NEW"), failure.getMessage());
    assertTrue(failure.getMessage().contains(TestBpmsBridge.ADAPTER_ID), failure.getMessage());

  }

  @Test
  @DisplayName("The workflow module registers itself while the application starts")
  public void theWorkflowModuleRegistersItself() {

    final var request = CockpitServer.awaitRegistration();

    assertEquals("/bpms/api/v1_1/workflow-module/test-module", request.path());
    assertTrue(request.body().contains("http://localhost:8081/test-module"), request.body());
    assertTrue(request.body().contains("\"clerks\""), request.body());

  }

}
