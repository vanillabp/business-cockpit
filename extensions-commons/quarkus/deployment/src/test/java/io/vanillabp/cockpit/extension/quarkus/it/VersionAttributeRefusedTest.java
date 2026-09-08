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
          "vanillabp.cockpit.rest.base-url", "http://localhost:1")
      .assertException(failure -> {
        final var message = everyMessageOf(failure);
        assertTrue(message.contains(VersionedProviderService.class.getName()), message);
        assertTrue(message.contains("approve"), message);
        assertTrue(message.contains("taskDefinition"), message);
      });

  /**
   * Every message of the chain: what an extension refuses is refused by VanillaBP's own scan,
   * which names the annotation, the class and the method in front of what the extension said -
   * so the two halves of the answer stand in two exceptions.
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
  @DisplayName("A details provider naming a version keeps the application from starting")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes: this application never runs

  }

}
