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
 * The store an entry goes into, in an application holding two of them: the one VanillaBP brings
 * for the datasource of the test, and one of the application which is named for the workflow
 * aggregate.
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
          "vanillabp.extensions.business-cockpit.rest.base-url", "http://localhost:1");

  @Inject
  RecordingOutbox namedStore;

  @Inject
  BusinessCockpitEventPublisher publisher;

  @Inject
  UserTransaction transaction;

  @Test
  @DisplayName("A store the application named for its aggregate is the one written into")
  public void theNamedStoreCarriesTheEntries() throws Exception {

    transaction.begin();
    publisher
        .publishWorkflowEvent(
            new WorkflowReference(
                TestBpmsBridge.ADAPTER_ID, "test-module", "TestProcess", "4711", TestBpmsBridge.WORKFLOW_ID),
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
