package io.vanillabp.cockpit.extension.springboot.ownway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.springboot.test.RecordingBpmsBridge;
import io.vanillabp.cockpit.extension.springboot.test.TestAggregate;
import io.vanillabp.cockpit.extension.springboot.test.TestApplication;
import io.vanillabp.cockpit.extension.springboot.test.TestWorkflowService;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application which brings its own way to the cockpit: one bean of type
 * <code>BusinessCockpitTransport</code>, no REST and no Kafka, and the reports go the way the
 * application wrote.
 * <p>
 * What such a transport inherits is why the seam is at this point: it is called while an outbox
 * entry is dispatched, so the report it is handed has been through the BPMS half, the details
 * provider of the application and the rendering of the titles. The Quarkus twin of this test
 * asserts the same thing.
 */
@SpringBootTest(classes = {
    TestApplication.class, OwnTransportConfiguration.class
},
    properties = {
        "spring.datasource.url=jdbc:h2:mem:cockpit-own-transport;DB_CLOSE_DELAY=-1",
        // an empty value is no value: the test module's file sets the key for every other test
        // class, and an application reporting its own way configures neither transport
        "vanillabp.cockpit.rest.base-url="
    })
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class OwnTransportTest {

  private static final String WORKFLOW_MODULE = "test-module";

  private static final String BPMN_PROCESS = "TestProcess";

  @Autowired
  private OwnTransport transport;

  @Autowired
  private BusinessCockpitConfiguration configuration;

  @Autowired
  private BusinessCockpitEventPublisher publisher;

  @Autowired
  private TestWorkflowService workflowService;

  @Autowired
  private TransactionTemplate transactions;

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
        TestApplication.ACCESSIBLE_TO_GROUPS, registration.accessibleToGroups());

  }

  @Test
  @DisplayName("A user task the BPMS reported reaches the transport of the application")
  public void aReportedUserTaskReachesTheOwnTransport() {

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
                        WORKFLOW_MODULE, BPMN_PROCESS, aggregate.getId().toString(),
                        RecordingBpmsBridge.USER_TASK_ID, "approve"),
                UserTaskEventKind.CREATED, "bpms-event-own-transport", OffsetDateTime.now(),
                EventTransaction.CURRENT));

    final var reported = transport.awaitUserTask("approve");

    assertEquals("bpms-event-own-transport", reported.getEventId());
    assertEquals(WORKFLOW_MODULE, reported.getWorkflowModuleId());
    assertEquals("Anna", reported.getDetails().get("customer"));

  }

}
