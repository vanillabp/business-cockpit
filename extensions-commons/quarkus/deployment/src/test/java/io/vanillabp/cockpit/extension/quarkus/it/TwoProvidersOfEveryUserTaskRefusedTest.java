package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application whose two details providers claim every user task does not start, and the
 * refusal names both methods. Which of them would run is decided by the order the methods are
 * found in, and that order is decided by nothing.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TwoProvidersOfEveryUserTaskRefusedTest {

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
              .addClass(TwoMethodsForEveryUserTaskService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", "http://localhost:1")
      .assertException(failure -> {
        final var message = rootCauseMessage(failure);
        assertTrue(
            message.contains(TwoMethodsForEveryUserTaskService.class.getSimpleName()), message);
        assertTrue(message.contains("everyTaskByItsTaskDefinition"), message);
        assertTrue(message.contains("everyTaskByItsElementId"), message);
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
  @DisplayName("Two details providers claiming every user task keep the application from starting")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes. This application never runs

  }

}
