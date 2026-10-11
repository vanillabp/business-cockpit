package io.vanillabp.cockpit.extension.springboot.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.test.support.CockpitServer;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What a single workflow, and a single user task of it, said about the Business Cockpit.
 * <p>
 * The two levels are inside the module's own section, because the core reserves that place for
 * an extension and hands it over as written. This test writes them the way a developer does,
 * boots an application and reads in which language the title arrived. Otherwise nobody would
 * notice how a platform flattens a nested key into that section.
 */
@SpringBootTest(classes = TestApplication.class,
    properties = {
        "vanillabp.workflow-modules.test-module.workflows.TestProcess.cockpit.bpmn-description-language=fr", "vanillabp.workflow-modules.test-module.workflows.TestProcess.cockpit.i18n-languages=fr",
        // an outbox store of its own. A context with another configuration stays cached and
        // running between the test classes. It would dispatch an entry of this class with its own
        // languages instead of the ones written above
        "spring.datasource.url=jdbc:h2:mem:cockpit-per-workflow;DB_CLOSE_DELAY=-1"
    })
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class PerWorkflowConfigurationTest {

  @DynamicPropertySource
  static void cockpitServer(
      final DynamicPropertyRegistry registry) {

    registry
        .add("vanillabp.cockpit.rest.base-url", CockpitServer::baseUrl);

  }

  @Autowired
  private TestWorkflowService workflowService;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private BusinessCockpitEventPublisher publisher;

  @BeforeEach
  public void forgetWhatArrivedBefore() {

    CockpitServer.forgetRequests();

  }

  @Test
  @DisplayName("A workflow's own language is the one its titles are reported in")
  public void theWorkflowsOwnLanguageIsUsed() {

    final var aggregate = transactions.execute(status -> {
      final var started = new TestAggregate();
      started.setCustomer("Amélie");
      return workflowService.processes().startWorkflow(started);
    });

    transactions
        .executeWithoutResult(status -> publisher
            .publishUserTaskEvent(
                RecordingBpmsBridge
                    .userTask(
                        "test-module", "TestProcess", aggregate.getId().toString(),
                        RecordingBpmsBridge.USER_TASK_ID, "approve"),
                UserTaskEventKind.CREATED, "bpms-event-per-workflow", OffsetDateTime.now(),
                EventTransaction.CURRENT));

    final var request = CockpitServer.awaitRequest("/usertask/created", "bpms-event-per-workflow");
    assertTrue(
        request.body().contains("\"title\":{\"fr\":"),
        "the title arrived in a language the workflow did not configure: "
            + request.body());

  }

}
