package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

/**
 * What a single workflow of a workflow module said about the Business Cockpit.
 * <p>
 * The level stands inside the module's own section, because that is the location the core
 * reserves for an extension and hands over as written. The Quarkus twin of the Spring Boot test
 * proves the same thing on this platform: the property is written the way a developer writes it,
 * and the title arrives in the language that workflow asked for.
 */
@ExtendWith(SuppressOutputExtension.class)
public class PerWorkflowConfigurationTest {

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
              .addClass(CockpitServer.class))
      .overrideRuntimeConfigKey(
          "vanillabp.extensions.business-cockpit.rest.base-url", CockpitServer.baseUrl())
      .overrideRuntimeConfigKey(
          "vanillabp.workflow-modules.test-module.extensions.business-cockpit.workflows.TestProcess.bpmn-description-language",
          "fr")
      .overrideRuntimeConfigKey(
          "vanillabp.workflow-modules.test-module.extensions.business-cockpit.workflows.TestProcess.i18n-languages",
          "fr");

  @Inject
  TestWorkflowService workflowService;

  @Inject
  BusinessCockpitEventPublisher publisher;

  @Inject
  UserTransaction transaction;

  @BeforeEach
  public void forgetWhatArrivedBefore() {

    CockpitServer.forgetRequests();

  }

  @Test
  @DisplayName("A workflow's own language is the one its titles are reported in")
  public void theWorkflowsOwnLanguageIsUsed() throws Exception {

    transaction.begin();
    final var aggregate = new TestAggregate();
    aggregate.setCustomer("Amélie");
    final var started = workflowService.processes().startWorkflow(aggregate);
    transaction.commit();

    transaction.begin();
    publisher
        .publishUserTaskEvent(
            TestBpmsBridge
                .userTask(
                    "test-module", "TestProcess", started.getId().toString(),
                    TestBpmsBridge.USER_TASK_ID),
            UserTaskEventKind.CREATED, "bpms-event-per-workflow", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    final var request = CockpitServer.awaitRequest("/usertask/created");
    assertTrue(
        request.body().contains("\"title\":{\"fr\":"),
        "the title arrived in a language the workflow did not configure: "
            + request.body());

  }

}
