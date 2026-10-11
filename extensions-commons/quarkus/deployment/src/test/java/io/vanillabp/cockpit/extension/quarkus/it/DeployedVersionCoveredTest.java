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
 * If the providers cover the version this start deployed, the start runs. A provider of a version
 * the BPMS does not hold only causes a warning. The Spring Boot test
 * <code>TheDeployedVersionNeedsACoveringProviderTest</code> checks the same, see decision 64 in
 * the repository's DECISIONS.md.
 */
@ExtendWith(SuppressOutputExtension.class)
public class DeployedVersionCoveredTest {

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
              .addClass(DeployedVersionCoveringService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class)
              .addClass(DeployedVersionProcessVersions.class)
              .addClass(DeployedVersionUserTasks.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", "http://localhost:1")
      .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
      .assertLogRecords(records -> {
        final var report = records
            .stream()
            .map(DeployedVersionCoveredTest::rendered)
            .filter(message -> message.contains("auditOfAVersionNobodyDeploys"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("nothing was said about the provider of a version nobody holds"));
        assertTrue(report.contains("the method never runs"), report);
        assertTrue(report.contains(ConfigurationKeys.EXTENSION_ID), report);
      });

  /**
   * One log record as a reader sees it, with the values of the record in its text.
   *
   * @param record The record
   * @return Its message
   */
  private static String rendered(
      final LogRecord record) {

    var message = record.getMessage() == null
        ? ""
        : record.getMessage();
    final var parameters = record.getParameters();
    if (parameters == null) {
      return message;
    }
    for (final var parameter : parameters) {
      message = message.replaceFirst("\\{\\}", String.valueOf(parameter));
    }
    return message;

  }

  @Test
  @DisplayName("Providers which cover the deployed version let the start run")
  public void theApplicationStarts() {

    // reaching this method is the assertion: the start ran through

  }

}
