package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A user task whose providers all miss the version this start deployed ends the start.
 */
@ExtendWith(SuppressOutputExtension.class)
public class DeployedVersionUncoveredTaskTest {

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
              .addClass(DeployedVersionUncoveredTaskService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class)
              .addClass(DeployedVersionProcessVersions.class)
              .addClass(DeployedVersionUserTasks.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", "http://localhost:1")
      .assertException(failure -> {
        final var message = everyMessageOf(failure);
        assertTrue(message.contains("Version '2' of BPMN process 'TestProcess'"), message);
        assertTrue(message.contains("element '%s' (also matched by '%s')"
            .formatted(DeployedVersionUserTasks.APPROVE_ELEMENT, DeployedVersionUserTasks.APPROVE)), message);
        assertTrue(message.contains("approveOfTheFirstVersion"), message);
        assertTrue(message.contains("which does not cover version '2'"), message);
        assertTrue(
            message.contains("For @UserTaskDetailsProvider of extension '%s': Every report about such a user task "
                .formatted(ConfigurationKeys.EXTENSION_ID)),
            message);
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
  @DisplayName("A user task whose providers miss the deployed version keeps the application down")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes. This application never runs

  }

}
