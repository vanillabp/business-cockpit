package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A workflow module which reports to the cockpit and says nowhere who answers its initiator does
 * not start, on this platform as well.
 * <p>
 * The message is the point of the key. Somebody meets this question once, at the first start, and
 * has to be able to answer it from the log alone.
 */
@ExtendWith(SuppressOutputExtension.class)
public class InitiatorSourceMissingTest {

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("without-an-initiator-source.yaml", "application.yaml")
              .addAsResource("test-module/processes/dummy/TestProcess.bpmn")
              .addAsResource(
                  "workflow-module-descriptor/workflow-module", "META-INF/workflow-module")
              .addClass(TestAggregate.class)
              .addClass(TestAggregatePersistence.class)
              .addClass(TestWorkflowService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class))
      .overrideRuntimeConfigKey("vanillabp.cockpit.rest.base-url", "http://localhost:1")
      .assertException(failure -> {
        final var message = messages(failure);
        assertTrue(
            message.contains("vanillabp.workflow-modules.test-module.cockpit.initiator-source"),
            message);
        assertTrue(message.contains("vanillabp.cockpit.initiator-source"), message);
        assertTrue(message.contains("by-application"), message);
        assertTrue(message.contains("system"), message);
        // why a boot ends over a missing value: it cannot be added to a case afterwards
        assertTrue(message.contains("for good"), message);
      });

  private static String messages(
      final Throwable failure) {

    final var messages = new StringBuilder();
    for (var cause = failure; cause != null; cause = cause.getCause()) {
      messages.append(cause.getMessage()).append('\n');
    }
    return messages.toString();

  }

  @Test
  @DisplayName("Without an initiator-source the application does not start")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes. This application never runs

  }

}
