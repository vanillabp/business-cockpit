package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
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
          "vanillabp.extensions.business-cockpit.rest.base-url", CockpitServer.baseUrl());

  @Inject
  TestWorkflowService workflowService;

  @Inject
  TestAggregatePersistence aggregates;

  @Inject
  BusinessCockpitEventPublisher publisher;

  @Inject
  UserTransaction transaction;

  @BeforeEach
  public void forgetWhatArrivedBefore() {

    CockpitServer.forgetRequests();

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

    final var request = CockpitServer.awaitRequest("/usertask/created");
    assertTrue(request.body().contains("\"id\":\"bpms-event-1\""), request.body());
    assertTrue(request.body().contains("\"customer\":\"Anna\""), request.body());
    assertTrue(request.body().contains("\"event\":\"CREATED\""), request.body());
    assertTrue(request.body().contains("\"candidateGroups\":[\"approvers\"]"), request.body());
    assertTrue(request.body().contains("Approve the order"), request.body());
    assertEquals(
        TestWorkflowService.APPROVE_NOTE, aggregates.byId(aggregate.getId()).getNote());

  }

  @Test
  @DisplayName("An event reported outside a transaction gets one of its own")
  public void anEventReportedWithoutATransactionGetsOne() throws Exception {

    final var aggregate = aStartedWorkflow();

    publisher
        .publishWorkflowEvent(
            new WorkflowReference(
                TestBpmsBridge.ADAPTER_ID, WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId()
                    .toString(), TestBpmsBridge.WORKFLOW_ID),
            WorkflowEventKind.CREATED, "bpms-event-2", OffsetDateTime.now(),
            EventTransaction.NEW);

    final var request = CockpitServer.awaitRequest("/workflow/created");
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

    final var request = CockpitServer.awaitRequest("/workflow/workflow-1/updated");
    assertTrue(request.body().contains("\"updated\":true"), request.body());

  }

  @Test
  @DisplayName("aggregateChanged with task ids reports those tasks")
  public void aggregateChangedReportsTheNamedTasks() throws Exception {

    final var aggregate = aStartedWorkflow();

    transaction.begin();
    workflowService.businessCockpit().aggregateChanged(aggregate, TestBpmsBridge.USER_TASK_ID);
    transaction.commit();

    final var request = CockpitServer.awaitRequest("/usertask/task-1/updated");
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
  @DisplayName("A report which was to ride the caller's transaction says so where there is none")
  public void aReportWithoutATransactionIsRefused() throws Exception {

    final var aggregate = aStartedWorkflow();

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

    assertTrue(failure.getMessage().contains("no transaction is running"), failure.getMessage());

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
