package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application whose details provider names the reserved <code>version</code> attribute does
 * not start, and the message names the method to change. The Spring Boot half of the extension
 * asserts the same thing: what an extension refuses has to be refused on both platforms, and
 * only a booted application shows whether the platform's glue asks at all.
 */
@ExtendWith(SuppressOutputExtension.class)
public class VersionAttributeRefusedTest {

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
              .addClass(VersionedProviderService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class))
      .overrideRuntimeConfigKey(
          "vanillabp.extensions.business-cockpit.rest.base-url", "http://localhost:1")
      .assertException(failure -> {
        final var message = rootCauseMessage(failure);
        assertTrue(message.contains(VersionedProviderService.class.getName()), message);
        assertTrue(message.contains("approve"), message);
        assertTrue(message.contains("taskDefinition"), message);
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
  @DisplayName("A details provider naming a version keeps the application from starting")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes: this application never runs

  }

}
