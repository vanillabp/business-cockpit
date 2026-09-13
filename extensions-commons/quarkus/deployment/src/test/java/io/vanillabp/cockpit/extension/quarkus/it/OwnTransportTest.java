package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

/**
 * An application which brings its own way to the cockpit: one bean of type
 * <code>BusinessCockpitTransport</code>, no REST and no Kafka, and the reports go the way the
 * application wrote.
 * <p>
 * It is the twin of the Spring Boot test of the same name. The seam has to be the same on both
 * platforms, because an application which reports its own way is what version 1 of the Business
 * Cockpit allowed and what a customer of it cannot upgrade without.
 */
@ExtendWith(SuppressOutputExtension.class)
public class OwnTransportTest {

  private static final String WORKFLOW_MODULE = "test-module";

  private static final String BPMN_PROCESS = "TestProcess";

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              // the file configures neither transport, and nothing overrides that here
              .addAsResource("own-transport.yaml", "application.yaml")
              .addAsResource("test-module/processes/dummy/TestProcess.bpmn")
              .addAsResource(
                  "workflow-module-descriptor/workflow-module", "META-INF/workflow-module")
              .addClass(TestAggregate.class)
              .addClass(TestAggregatePersistence.class)
              .addClass(TestWorkflowService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class)
              .addClass(OwnTransport.class));

  @Inject
  OwnTransport transport;

  @Inject
  BusinessCockpitConfiguration configuration;

  @Inject
  BusinessCockpitEventPublisher publisher;

  @Inject
  TestWorkflowService workflowService;

  @Inject
  UserTransaction transaction;

  @Test
  @DisplayName("Neither shipped transport is configured, and the extension knows why")
  public void neitherShippedTransportIsConfigured() {

    assertNull(configuration.getRest());
    assertNull(configuration.getKafka());
    assertTrue(configuration.isTransportProvidedByTheApplication());

  }

  @Test
  @DisplayName("The workflow module registers itself through the transport of the application")
  public void theWorkflowModuleRegistersItselfThroughTheOwnTransport() {

    final var registration = transport.awaitWorkflowModule(WORKFLOW_MODULE);

    assertEquals(
        TestWorkflowModuleDetails.ACCESSIBLE_TO_GROUPS, registration.accessibleToGroups());

  }

  @Test
  @DisplayName("A user task the BPMS reported reaches the transport of the application")
  public void aReportedUserTaskReachesTheOwnTransport() throws Exception {

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
                    WORKFLOW_MODULE, BPMN_PROCESS, started.getId().toString(),
                    TestBpmsBridge.USER_TASK_ID),
            UserTaskEventKind.CREATED, "bpms-event-own-transport", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    final var reported = transport.awaitUserTask("approve");

    assertEquals("bpms-event-own-transport", reported.getEventId());
    assertEquals(WORKFLOW_MODULE, reported.getWorkflowModuleId());
    assertEquals("Anna", reported.getDetails().get("customer"));

  }

}
