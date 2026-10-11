package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application whose two providers of one user task serve a common version does not start.
 * The message names both methods and both ranges. The Spring Boot half of the extension checks
 * the same thing. What VanillaBP refuses has to be refused on both platforms, and only a booted
 * application shows whether the glue code of the platform registers the contract at all.
 */
@ExtendWith(SuppressOutputExtension.class)
public class OverlappingVersionsRefusedTest {

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
              .addClass(OverlappingVersionsProviderService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", "http://localhost:1")
      .assertException(failure -> {
        final var message = everyMessageOf(failure);
        assertTrue(message.contains("approveUpToTheThird"), message);
        assertTrue(message.contains("approveFromTheThird"), message);
        // the message names both ranges. The developer has to decide which of the two to make
        // narrower, and cannot decide that without seeing both
        assertTrue(message.contains("1-3"), message);
        assertTrue(message.contains(">2"), message);
      });

  /**
   * Every message of the chain. VanillaBP's own wiring ends the boot, and it puts the
   * annotation, the class and the method in front of what it found. So the parts of the answer
   * are spread over several exceptions.
   *
   * @param failure What the boot failed with
   * @return The messages, one per line
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
  @DisplayName("Two providers of one task whose versions overlap keep the application down")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes. This application never runs

  }

}
