package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A key nobody reads inside a workflow module's own defaults file keeps the application from
 * starting, and Quarkus is what refuses it.
 * <p>
 * The extension's own check runs while the application is built and sees the configuration of the
 * build; a module's defaults file is a configuration source of the running application, so it
 * reaches the startup instead. What is asserted here is that such a key does not slip through:
 * the message names it, without guessing which key was meant.
 */
@ExtendWith(SuppressOutputExtension.class)
public class MisspelledKeyInAModuleFileTest {

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("business-cockpit.yaml", "application.yaml")
              .addAsResource("misspelled-module-file.yaml", "test-module.yaml")
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
        final var messages = new StringBuilder();
        for (var cause = failure; cause != null; cause = cause.getCause()) {
          messages.append(cause.getMessage()).append('\n');
        }
        assertTrue(
            messages.toString().contains("vanillabp.workflow-modules.test-module.cockpit.ui-uri-pth"),
            messages.toString());
      });

  @Test
  @DisplayName("A key nobody reads in a module's own file ends the startup")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes: this application never runs

  }

}
