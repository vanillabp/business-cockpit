package io.vanillabp.cockpit.extension.springboot.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.springboot.stores.PayloadLosingOutboxAware;
import io.vanillabp.cockpit.extension.test.support.CockpitServer;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the cockpit is told about an entry which outlived the report it was to carry.
 * <p>
 * A report is written beside its entry and is removed once the entry was dispatched, so an
 * entry meets this after waiting longer than the outbox keeps a report, or after a crash
 * between the two writes. The store of this application throws every report away, which is the
 * same thing for whoever dispatches the entry.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class ReportWithoutItsPayloadTest {

  @Test
  @DisplayName("An entry which lost its report is sent with the identifiers of its event")
  public void anEntryWhichLostItsReportIsSentAnyway() {

    // the two settings arrive as command-line arguments, because what a builder is handed as
    // properties is a default and the application.yaml of this module would win over it
    try (var application = new SpringApplicationBuilder(
        TestApplication.class, PayloadLosingOutboxAware.class)
        .web(WebApplicationType.NONE)
        .run(
            "--spring.datasource.url=jdbc:h2:mem:cockpit-payload-lost;DB_CLOSE_DELAY=-1",
            "--vanillabp.cockpit.rest.base-url="
                + CockpitServer.baseUrl())) {

      CockpitServer.forgetRequests();
      final var transactions = application.getBean(TransactionTemplate.class);
      final var workflowService = application.getBean(TestWorkflowService.class);
      final var publisher = application.getBean(BusinessCockpitEventPublisher.class);
      final var aggregate = transactions.execute(status -> {
        final var started = new TestAggregate();
        started.setCustomer("Anna");
        return workflowService.processes().startWorkflow(started);
      });

      transactions
          .executeWithoutResult(status -> publisher
              .publishUserTaskEvent(
                  RecordingBpmsBridge
                      .userTask(
                          "test-module", "TestProcess", aggregate.getId().toString(),
                          RecordingBpmsBridge.USER_TASK_ID, "approve"),
                  UserTaskEventKind.CREATED, "bpms-event-lost-1", OffsetDateTime.now(),
                  EventTransaction.CURRENT));

      final var request = CockpitServer.awaitRequest("/usertask/created", "bpms-event-lost-1");
      assertTrue(
          request.body().contains("\"userTaskId\":\""
              + RecordingBpmsBridge.USER_TASK_ID
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

}
