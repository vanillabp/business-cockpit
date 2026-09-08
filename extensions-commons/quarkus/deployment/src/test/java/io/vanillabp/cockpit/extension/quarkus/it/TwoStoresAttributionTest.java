package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;

/**
 * An application whose two workflow aggregates live in two outbox stores, each named by the
 * application itself.
 * <p>
 * Each report has to reach the store of its own workflow, because an entry which is not
 * committed with what it reports reports something that may never have happened. The event a
 * BPMS observed names a BPMN process and no class, and the workflow service serving that process
 * is where the class comes from - see decision 13 of the repository's decision log.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TwoStoresAttributionTest {

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
              .addClass(TestAggregateOutboxAware.class)
              .addClass(SecondAggregate.class)
              .addClass(SecondAggregatePersistence.class)
              .addClass(SecondWorkflowService.class)
              .addClass(SecondRecordingOutbox.class)
              .addClass(SecondAggregateOutboxAware.class))
      .overrideRuntimeConfigKey(
          "vanillabp.extensions.business-cockpit.rest.base-url", "http://localhost:1");

  @Inject
  RecordingOutbox storeOfTheFirstAggregate;

  @Inject
  SecondRecordingOutbox storeOfTheSecondAggregate;

  @Inject
  BusinessCockpitEventPublisher publisher;

  @Inject
  UserTransaction transaction;

  private static List<String> workflowReportsOf(
      final List<PhaseTwoCall> scheduled) {

    return scheduled
        .stream()
        .filter(call -> call.operation().contains("PUBLISH_WORKFLOW_EVENT"))
        .map(call -> call.args().get("eventId"))
        .toList();

  }

  @Test
  @DisplayName("Each report lands in the store of the aggregate its workflow belongs to")
  public void eachReportLandsInItsOwnStore() throws Exception {

    transaction.begin();
    publisher
        .publishWorkflowEvent(
            new WorkflowReference(
                TestBpmsBridge.ADAPTER_ID, "test-module", "TestProcess", "4711", TestBpmsBridge.WORKFLOW_ID),
            WorkflowEventKind.CREATED, "of-the-first-aggregate", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    publisher
        .publishWorkflowEvent(
            new WorkflowReference(
                TestBpmsBridge.ADAPTER_ID, "test-module", "SecondProcess", "4712", "workflow-2"),
            WorkflowEventKind.CREATED, "of-the-second-aggregate", OffsetDateTime.now(),
            EventTransaction.CURRENT);
    transaction.commit();

    assertEquals(
        List.of("of-the-first-aggregate"),
        workflowReportsOf(storeOfTheFirstAggregate.getScheduled()),
        "the store of 'TestProcess' was handed something else than that workflow's report");
    assertEquals(
        List.of("of-the-second-aggregate"),
        workflowReportsOf(storeOfTheSecondAggregate.getScheduled()),
        "the store of 'SecondProcess' was handed something else than that workflow's report");

  }

}
