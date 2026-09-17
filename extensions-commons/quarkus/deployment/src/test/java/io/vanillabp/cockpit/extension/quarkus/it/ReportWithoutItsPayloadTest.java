package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

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
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

/**
 * What the cockpit is told about an entry which outlived the report it was to carry.
 * <p>
 * A report is written beside its entry and is removed once the entry was dispatched, so an
 * entry meets this after waiting longer than the outbox keeps a report, or after a crash
 * between the two writes. The store of this application throws every report away, which is the
 * same thing for whoever dispatches the entry.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ReportWithoutItsPayloadTest {

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
              .addClass(PayloadLosingOutboxAware.class)
              .addClass(CockpitServer.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", CockpitServer.baseUrl());

  @Inject
  TestWorkflowService workflowService;

  @Inject
  BusinessCockpitEventPublisher publisher;

  @Inject
  UserTransaction transaction;

  @Test
  @DisplayName("An entry which lost its report is sent with the identifiers of its event")
  public void anEntryWhichLostItsReportIsSentAnyway() throws Exception {

    CockpitServer.forgetRequests();

    transaction.begin();
    final var aggregate = new TestAggregate();
    aggregate.setCustomer("Anna");
    final var started = workflowService.processes().startWorkflow(aggregate);
    transaction.commit();

    transaction.begin();
    publisher
        .publishUserTaskEvent(
            TestBpmsBridge
                .userTask(
                    "test-module", "TestProcess", started.getId().toString(),
                    TestBpmsBridge.USER_TASK_ID),
            UserTaskEventKind.CREATED, "bpms-event-lost-1", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    final var request = CockpitServer.awaitRequest("/usertask/created", "bpms-event-lost-1");
    assertTrue(
        request.body().contains("\"userTaskId\":\""
            + TestBpmsBridge.USER_TASK_ID
            + "\""),
        request.body());
    assertFalse(
        request.body().contains("Approve the order"),
        "a report which is gone was rendered from somewhere: "
            + request.body());
    assertFalse(
        request.body().contains("customer"),
        "a report which is gone still carried business data: "
            + request.body());

  }

}
