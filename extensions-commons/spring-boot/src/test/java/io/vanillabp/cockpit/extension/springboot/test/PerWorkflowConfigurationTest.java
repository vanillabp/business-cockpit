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
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What a single workflow, and a single user task of it, said about the Business Cockpit.
 * <p>
 * The two levels stand inside the module's own section, because that is the location the core
 * reserves for an extension and hands over as written. This test states them the way a developer
 * writes them, boots an application, and reads which language the title arrived in: how a
 * platform flattens a nested key into that section is exactly what goes unnoticed otherwise.
 */
@SpringBootTest(classes = TestApplication.class,
    properties = {
        "vanillabp.workflow-modules.test-module.workflows.TestProcess.cockpit.bpmn-description-language=fr", "vanillabp.workflow-modules.test-module.workflows.TestProcess.cockpit.i18n-languages=fr",
        // an outbox store of its own: a context of another configuration stays cached and
        // running between the test classes, and it would dispatch an entry of this one with
        // the languages it was configured with rather than the ones written above
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

    final var request = CockpitServer.awaitRequest("/usertask/created");
    assertTrue(
        request.body().contains("\"title\":{\"fr\":"),
        "the title arrived in a language the workflow did not configure: "
            + request.body());

  }

}
