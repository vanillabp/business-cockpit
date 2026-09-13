package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application whose details provider changes the workflow aggregate hears about the second
 * writer while it boots.
 * <p>
 * A provider runs while a report is dispatched and writes the case back when that dispatch
 * commits, so a change the application made in between is written over where nothing notices a
 * second writer. VanillaBP says so, over the handler contracts an extension registered, and
 * this extension adds no warning of its own - see decision 20 in the repository's DECISIONS.md.
 * <p>
 * What is asserted here is that the warning reaches an application with the Business Cockpit,
 * naming this extension and the case it is about. The wording belongs to the platform and is
 * asserted there; a change which stopped the warning from reaching a cockpit application would
 * fail here, which is the point.
 */
@ExtendWith(SuppressOutputExtension.class)
public class SecondWriterOfTheCaseTest {

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
            .filter(message -> (message != null) && message.contains(ConfigurationKeys.EXTENSION_ID))
            .filter(message -> message.contains(TestAggregate.class.getName()))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError(
                    "nothing was said about the second writer this extension brings: "
                        + records.stream().map(LogRecord::getMessage).toList()));

        // the way out is what a developer needs from the line, and it is a version attribute on
        // the case
        assertTrue(report.contains("version attribute"), report);
      });

  @Test
  @DisplayName("A writing details provider on a case which cannot notice it is named while booting")
  public void theSecondWriterOfTheCaseIsReported() {

    // the assertion is the one made on the boot's log records above

  }

}
