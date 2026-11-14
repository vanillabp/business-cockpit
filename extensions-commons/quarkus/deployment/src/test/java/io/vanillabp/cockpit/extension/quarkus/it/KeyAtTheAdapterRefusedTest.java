package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A key at the place per adapter which nobody reads there does not reach a running application.
 * One is a neutral key, which belongs to <code>vanillabp.cockpit</code>. The other is a key of the
 * BPMS half of the test adapter type, which that half reads at the adapter and not at the workflow
 * module. The check finds the second one through the service loader while Quarkus builds the
 * application, see {@link DummyAdapterCockpitKeys}.
 */
@ExtendWith(SuppressOutputExtension.class)
public class KeyAtTheAdapterRefusedTest {

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("key-at-the-adapter.yaml", "application.yaml")
              .addAsResource("test-module/processes/dummy/TestProcess.bpmn")
              .addAsResource(
                  "workflow-module-descriptor/workflow-module", "META-INF/workflow-module")
              .addClass(TestAggregate.class)
              .addClass(TestAggregatePersistence.class)
              .addClass(TestWorkflowService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class)
              // a BPMS half is a jar of the application, and its announcement comes with it
              .addClass(DummyAdapterCockpitKeys.class)
              .addAsResource(
                  "META-INF/services/io.vanillabp.cockpit.extension.spi.AdapterCockpitKeys",
                  "META-INF/services/io.vanillabp.cockpit.extension.spi.AdapterCockpitKeys"))
      .assertException(failure -> {
        final var message = messages(failure);
        assertTrue(message.contains("'vanillabp.cockpit.template-loader-path'"), message);
        assertTrue(
            message.contains("The Business Cockpit adapter for 'dummy' reads 'check-interval' at the adapter"),
            message);
        assertTrue(
            message.contains("vanillabp.workflow-modules.test-module.adapters.test.cockpit.check-interval"), message);
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
  @DisplayName("A key at the wrong place per adapter keeps the application from starting")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes. This application never runs

  }

}
