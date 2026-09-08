package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The same two stores as {@link StoreAttributionTest} and no bean naming one of them for the
 * aggregate. The application persists its aggregate itself, so nothing can read a technology
 * off it, and VanillaBP refuses to guess - the extension ends the boot with that refusal
 * instead of with a verdict of its own, and the message is the platform's, naming the bean the
 * application has to add.
 */
@ExtendWith(SuppressOutputExtension.class)
public class UnattributableStoresRefusedTest {

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
              .addClass(RecordingOutbox.class))
      .overrideRuntimeConfigKey(
          "vanillabp.extensions.business-cockpit.rest.base-url", "http://localhost:1")
      .assertException(failure -> {
        final var message = rootCauseMessage(failure);
        assertTrue(message.contains(TestAggregate.class.getName()), message);
        assertTrue(
            message.contains("io.vanillabp.integration.spi.PhaseTwoOutboxAware"), message);
      });

  private static String rootCauseMessage(
      final Throwable failure) {

    var cause = failure;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    return String.valueOf(cause.getMessage());

  }

  @Test
  @DisplayName("Two stores nobody attributed keep the application from starting")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes: this application never runs

  }

}
