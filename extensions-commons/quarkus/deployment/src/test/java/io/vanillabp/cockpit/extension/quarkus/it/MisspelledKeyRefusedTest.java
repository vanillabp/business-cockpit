package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A key which looks like a setting of the Business Cockpit and is read by nobody does not reach a
 * running application on this platform.
 * <p>
 * Quarkus refuses a key below <code>vanillabp</code> which no mapping declares, which is the
 * asymmetry between the two platforms the VanillaBP platform integration decided on. What the
 * extension adds to it is the answer to the question the developer has: the message names the key
 * which was meant.
 */
@ExtendWith(SuppressOutputExtension.class)
public class MisspelledKeyRefusedTest {

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("misspelled-key.yaml", "application.yaml")
              .addAsResource("test-module/processes/dummy/TestProcess.bpmn")
              .addAsResource(
                  "workflow-module-descriptor/workflow-module", "META-INF/workflow-module")
              .addClass(TestAggregate.class)
              .addClass(TestAggregatePersistence.class)
              .addClass(TestWorkflowService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class))
      .assertException(failure -> {
        final var message = messages(failure);
        assertTrue(
            message.contains("vanillabp.workflow-modules.test-module.cockpit.ui-uri-pth"), message);
        assertTrue(
            message.contains("vanillabp.workflow-modules.test-module.cockpit.ui-uri-path"),
            message);
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
  @DisplayName("A misspelled key keeps the application from starting and names the one meant")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes: this application never runs

  }

}
