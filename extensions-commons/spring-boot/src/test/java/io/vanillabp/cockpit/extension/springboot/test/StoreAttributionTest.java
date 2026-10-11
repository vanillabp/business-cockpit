package io.vanillabp.cockpit.extension.springboot.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.cockpit.extension.springboot.stores.RecordingOutbox;
import io.vanillabp.cockpit.extension.springboot.stores.TestAggregateOutboxAware;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Which of an application's outbox stores the extension writes into.
 * <p>
 * Both applications below hold two stores. One is the store VanillaBP brings for the relational
 * database of the test, the other belongs to the application. VanillaBP knows which of them
 * serves the workflow aggregate, and the extension asks instead of guessing. Guessing led to an
 * extension which refused an application the platform serves, or which picked a store the
 * transaction of the aggregate never reaches.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class StoreAttributionTest {

  private static ConfigurableApplicationContext applicationWith(
      final String databaseName,
      final Class<?>... sources) {

    return new SpringApplicationBuilder(sources)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1".formatted(databaseName),
            "vanillabp.cockpit.rest.base-url=http://localhost:1")
        .run();

  }

  /**
   * Reports what a BPMS observed. This entry names no workflow aggregate class, and the extension
   * writes it into the store which all aggregates of the application share.
   * <p>
   * The aggregate is started first, because the report is built where the event is published:
   * the details provider of the application runs and gets the case the event is about.
   */
  private static void reportAWorkflowOfTheBpms(
      final ConfigurableApplicationContext application) {

    final var publisher = application.getBean(BusinessCockpitEventPublisher.class);
    final var transactions = application.getBean(TransactionTemplate.class);
    final var workflowService = application.getBean(TestWorkflowService.class);
    final var aggregate = transactions.execute(status -> {
      final var started = new TestAggregate();
      started.setCustomer("Anna");
      return workflowService.processes().startWorkflow(started);
    });
    transactions
        .executeWithoutResult(status -> publisher
            .publishWorkflowEvent(
                new WorkflowReference(
                    "test", "test-module", "TestProcess", "1", aggregate.getId().toString(), "workflow-1"),
                WorkflowEventKind.CREATED, "bpms-event-1", OffsetDateTime.now(),
                EventTransaction.CURRENT));

  }

  @Test
  @DisplayName("With a store of the application next to VanillaBP's, the aggregate's own is used")
  public void theStoreOfTheAggregatesPersistenceIsUsed() {

    try (var application = applicationWith(
        "cockpit-two-stores", TestApplication.class, RecordingOutbox.class)) {

      reportAWorkflowOfTheBpms(application);

      assertTrue(
          application.getBean(RecordingOutbox.class).getScheduled().isEmpty(),
          "the entry belongs into the store the aggregate's transaction reaches, which is VanillaBP's");

    }

  }

  @Test
  @DisplayName("A store the application named for its aggregate is the one written into")
  public void aNamedStoreIsUsedForItsAggregate() {

    try (var application = applicationWith(
        "cockpit-named-store", TestApplication.class, RecordingOutbox.class,
        TestAggregateOutboxAware.class)) {

      reportAWorkflowOfTheBpms(application);

      assertTrue(
          application
              .getBean(RecordingOutbox.class)
              .getScheduled()
              .stream()
              .anyMatch(call -> call.args().containsValue("bpms-event-1")),
          "the store named for the only aggregate of this application carries its entries");

    }

  }

}
