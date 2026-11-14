package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A key of version 1 which version 2 does not read ends the start, and the message says what
 * became of it. This is the twin of the Spring Boot boot which writes the same key.
 */
@ExtendWith(SuppressOutputExtension.class)
public class KeyOfVersionOneRefusedTest {

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("key-of-version-one.yaml", "application.yaml")
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
        final var messages = new StringBuilder();
        for (var cause = failure; cause != null; cause = cause.getCause()) {
          messages.append(cause.getMessage()).append('\n');
        }
        final var message = messages.toString();
        assertTrue(
            message.contains("vanillabp.workflow-modules.test-module.cockpit.group-hierarchy-bean-name"),
            message);
        assertTrue(message.contains("key of version 1"), message);
        assertTrue(message.contains("WorkflowModuleGroupHierarchy"), message);
      });

  @Test
  @DisplayName("A key of version 1 keeps the application from starting and says what became of it")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes. This application never runs

  }

}
