package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.transport.BusinessCockpitTransport;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application which configured no transport and brought none either does not start, and the
 * message names the three ways it has: the two keys and the bean.
 * <p>
 * This is what an application without a transport of its own still reads, which the story about
 * the own transport must not change.
 */
@ExtendWith(SuppressOutputExtension.class)
public class NoTransportRefusedTest {

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("own-transport.yaml", "application.yaml")
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
        assertTrue(message.contains("vanillabp.cockpit.rest.base-url"), message);
        assertTrue(message.contains("vanillabp.cockpit.kafka.bootstrap-servers"), message);
        assertTrue(message.contains(BusinessCockpitTransport.class.getName()), message);
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
  @DisplayName("Without a transport and without a bean the application does not start")
  public void theApplicationDoesNotStart() {

    // the assertion is the one the extension above makes: this application never runs

  }

}
