package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

/**
 * The store an entry goes into, in an application which holds two of them. One is what VanillaBP
 * brings for the datasource of the test. The other belongs to the application and is named for
 * the workflow aggregate.
 * <p>
 * The extension used to work this out itself, which is what {@link QuarkusStoreAttributionTest}
 * of the platform shows cannot be done from outside the Quarkus integration. It asks the
 * platform's resolver now, and this test is the application in which the two answers would
 * differ.
 */
@ExtendWith(SuppressOutputExtension.class)
public class StoreAttributionTest {

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
              .addClass(RecordingOutbox.class)
              .addClass(TestAggregateOutboxAware.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", "http://localhost:1");

  @Inject
  RecordingOutbox namedStore;

  @Inject
  TestAggregatePersistence aggregates;

  @Inject
  BusinessCockpitEventPublisher publisher;

  @Inject
  UserTransaction transaction;

  @Test
  @DisplayName("A store the application named for its aggregate is the one written into")
  public void theNamedStoreCarriesTheEntries() throws Exception {

    // the case is saved first, because the report is put together where it is made and the
    // details provider of the application is handed the case the event is about
    final var aggregate = new TestAggregate();
    aggregate.setCustomer("Anna");
    aggregates.save(aggregate);

    transaction.begin();
    publisher
        .publishWorkflowEvent(
            new WorkflowReference(
                TestBpmsBridge.ADAPTER_ID, "test-module", "TestProcess", TestBpmsBridge.PROCESS_VERSION, aggregate
                    .getId()
                    .toString(), TestBpmsBridge.WORKFLOW_ID),
            WorkflowEventKind.CREATED, "bpms-event-1", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    assertTrue(
        namedStore
            .getScheduled()
            .stream()
            .anyMatch(call -> call.args().containsValue("bpms-event-1")),
        "the store named for the only aggregate of this application carries its entries");

  }

}
