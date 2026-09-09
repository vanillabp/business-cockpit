package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A <code>&#64;UserTaskDetailsProvider</code> which is not public is named while the
 * application boots.
 * <p>
 * The scan reads the PUBLIC methods of a workflow service class, so such a method is invoked by
 * nobody - and nothing else would ever say so, because a user task without a provider is
 * reported with whatever the BPMS carried. The report is VanillaBP's, written over the handler
 * contracts an extension registered as well as over its own three annotations, and the Spring
 * Boot half of this extension asserts the same thing: only a booted application shows whether
 * the platform's glue registers the contract at all.
 */
@ExtendWith(SuppressOutputExtension.class)
public class DetailsProviderNobodySeesTest {

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
              .addClass(TestWorkflowModuleDetails.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", "http://localhost:1")
      .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
      .assertLogRecords(records -> {
        final var report = records
            .stream()
            .map(LogRecord::getMessage)
            .filter(message -> (message != null) && message.contains("which VanillaBP does not see"))
            .filter(message -> message.contains("@UserTaskDetailsProvider"))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError(
                    "no report about the details provider nobody sees: "
                        + records.stream().map(LogRecord::getMessage).toList()));

        assertTrue(report.contains(TestWorkflowService.class.getName()), report);
        assertTrue(report.contains("unseenByTheScan"), report);
        assertTrue(report.contains("is protected"), report);
        assertTrue(report.contains("Make the method public"), report);
      });

  @Test
  @DisplayName("A details provider which is not public is named while booting")
  public void theInvisibleDetailsProviderIsReported() {

    // the assertion is the one made on the boot's log records above

  }

}
