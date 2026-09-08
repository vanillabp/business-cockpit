package io.vanillabp.cockpit.extension.springboot.twostores;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.cockpit.extension.springboot.stores.RecordingOutbox;
import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application whose workflow aggregates live in two outbox stores.
 * <p>
 * Each report has to reach the store of its own workflow, because an entry which is not
 * committed with what it reports reports something that may never have happened. The event a
 * BPMS observed names a BPMN process and no class, and the workflow service serving that
 * process is where the class comes from - see decision 13 of the repository's decision log.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class TwoStoresAttributionTest {

  /**
   * @param store One of the application's two stores
   * @return The reports of workflows it was handed - the registration of the workflow module
   *         lands in one of the two as well, and which one is not what this test is about
   */
  private static List<PhaseTwoCall> workflowReportsOf(
      final RecordingOutbox store) {

    return store
        .getScheduled()
        .stream()
        .filter(call -> call.operation().contains("PUBLISH_WORKFLOW_EVENT"))
        .toList();

  }

  private static List<String> eventIdsOf(
      final List<PhaseTwoCall> reports) {

    return reports.stream().map(call -> call.args().get("eventId")).toList();

  }

  @Test
  @DisplayName("Each report lands in the store of the aggregate its workflow belongs to")
  public void eachReportLandsInItsOwnStore() {

    try (var application = new SpringApplicationBuilder(TwoStoresApplication.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:h2:mem:cockpit-two-stores;DB_CLOSE_DELAY=-1",
            "vanillabp.extensions.business-cockpit.rest.base-url=http://localhost:1")
        .run()) {

      final var publisher = application.getBean(BusinessCockpitEventPublisher.class);
      application
          .getBean(TransactionTemplate.class)
          .executeWithoutResult(status -> {
            publisher
                .publishWorkflowEvent(
                    new WorkflowReference(
                        "test", "test-module", "TestProcess", "4711", "workflow-relational"),
                    WorkflowEventKind.CREATED, "of-the-relational-aggregate", OffsetDateTime.now(),
                    EventTransaction.CURRENT);
            publisher
                .publishWorkflowEvent(
                    new WorkflowReference(
                        "test", "test-module", "SecondProcess", "4712", "workflow-document"),
                    WorkflowEventKind.CREATED, "of-the-document-aggregate", OffsetDateTime.now(),
                    EventTransaction.CURRENT);
          });

      assertEquals(
          List.of("of-the-relational-aggregate"),
          eventIdsOf(workflowReportsOf(application.getBean("relationalStore", RecordingOutbox.class))),
          "the store of 'TestProcess' was handed something else than that workflow's report");
      assertEquals(
          List.of("of-the-document-aggregate"),
          eventIdsOf(workflowReportsOf(application.getBean("documentStore", RecordingOutbox.class))),
          "the store of 'SecondProcess' was handed something else than that workflow's report");

    }

  }

  @Test
  @DisplayName("The registration of a workflow module goes into one of its own stores")
  public void theRegistrationGoesIntoOneOfTheModulesStores() {

    try (var application = new SpringApplicationBuilder(TwoStoresApplication.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:h2:mem:cockpit-two-stores-registration;DB_CLOSE_DELAY=-1",
            "vanillabp.extensions.business-cockpit.rest.base-url=http://localhost:1")
        .run()) {

      final var registrations = List
          .of(
              application.getBean("relationalStore", RecordingOutbox.class),
              application.getBean("documentStore", RecordingOutbox.class))
          .stream()
          .flatMap(store -> store.getScheduled().stream())
          .filter(call -> call.operation().contains("REGISTER_WORKFLOW_MODULE"))
          .toList();

      assertEquals(
          1, registrations.size(),
          "a workflow module registers once, in one store, however many stores there are");

    }

  }

}
