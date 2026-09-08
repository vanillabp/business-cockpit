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
 * Both applications below hold two stores: the one VanillaBP brings for the relational
 * database of the test, and one of the application. Which of them serves the workflow
 * aggregate is VanillaBP's answer, and the extension asks rather than guessing - an extension
 * refusing an application the platform serves, or picking a store the aggregate's transaction
 * never reaches, is what the guess produced.
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
            "vanillabp.extensions.business-cockpit.rest.base-url=http://localhost:1")
        .run();

  }

  /**
   * Reports what a BPMS observed, which is the entry naming no workflow aggregate class - the
   * one the extension writes into the store all aggregates of the application share.
   */
  private static void reportAWorkflowOfTheBpms(
      final ConfigurableApplicationContext application) {

    final var publisher = application.getBean(BusinessCockpitEventPublisher.class);
    application
        .getBean(TransactionTemplate.class)
        .executeWithoutResult(status -> publisher
            .publishWorkflowEvent(
                new WorkflowReference(
                    "test", "test-module", "TestProcess", "4711", "workflow-1"),
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
