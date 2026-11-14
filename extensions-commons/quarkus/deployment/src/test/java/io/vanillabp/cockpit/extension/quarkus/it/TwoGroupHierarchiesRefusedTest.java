package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A group hierarchy from a bean and from the key ends the start, and the message names the class
 * of the bean. The bean is <code>&#64;ApplicationScoped</code>, so Quarkus hands it out as a
 * client proxy. The message names the class the developer wrote, not the proxy.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TwoGroupHierarchiesRefusedTest {

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("matrix.yaml", "application.yaml")
              .addAsResource("matrix-test-module.yaml", "test-module.yaml")
              .addAsResource("test-module/processes/dummy/TestProcess.bpmn")
              .addAsResource(
                  "workflow-module-descriptor/workflow-module", "META-INF/workflow-module")
              .addClass(TestAggregate.class)
              .addClass(TestAggregatePersistence.class)
              .addClass(TestWorkflowService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class)
              .addClass(ChangingGroupHierarchy.class))
      .assertException(failure -> {
        final var messages = new StringBuilder();
        for (var cause = failure; cause != null; cause = cause.getCause()) {
          messages.append(cause.getMessage()).append('\n');
        }
        final var message = messages.toString();
        assertTrue(
            message.contains("vanillabp.workflow-modules.test-module.cockpit.group-hierarchy"),
            message);
        assertTrue(message.contains(ChangingGroupHierarchy.class.getName()), message);
        assertFalse(message.contains("_ClientProxy"), message);
      });

  @Test
  @DisplayName("A hierarchy from a bean and from the key ends the start, naming the bean's class")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes. This application never runs

  }

}
