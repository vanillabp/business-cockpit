package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A user task of a called process is checked against the version of the called process, not
 * against the version of the process at the top.
 */
@ExtendWith(SuppressOutputExtension.class)
public class DeployedVersionOfACalledProcessUncoveredTest {

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
              .addClass(DeployedVersionSecondaryUncoveredTaskService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class)
              .addClass(DeployedVersionProcessVersions.class)
              .addClass(DeployedVersionUserTasks.class)
              .addClass(DeployedVersionCalledProcessInTheSameFile.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", "http://localhost:1")
      .assertException(failure -> {
        final var message = everyMessageOf(failure);
        assertTrue(message.contains("Version '1' of BPMN process '%s'"
            .formatted(DeployedVersionProcessVersions.CALLED_PROCESS)), message);
        assertTrue(message.contains("element '%s' (also matched by '%s')"
            .formatted(DeployedVersionUserTasks.CHECK_ELEMENT, DeployedVersionUserTasks.CHECK)), message);
        assertTrue(message.contains("checkOfTheSecondVersion"), message);
        assertFalse(message.contains("the process itself"), message);
      });

  /**
   * Every message of the chain, one per line.
   *
   * @param failure What the boot failed with
   * @return The messages
   */
  private static String everyMessageOf(
      final Throwable failure) {

    final var messages = new StringBuilder();
    for (var cause = failure; cause != null; cause = cause.getCause()) {
      messages.append(cause.getMessage()).append('\n');
    }
    return messages.toString();

  }

  @Test
  @DisplayName("A user task of a called process is held against the version of that process")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes. This application never runs

  }

}
