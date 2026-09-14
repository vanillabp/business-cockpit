package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.config.ConfigurationKeys;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application whose only handlers are details providers hears nothing about a second writer
 * while it boots.
 * <p>
 * VanillaBP warns about a handler it may save the workflow aggregate after, because such a
 * handler writes over what the application changed in between where nothing notices a second
 * writer. The contracts of this extension say that VanillaBP never saves after a details
 * provider, so there is no such handler here, and a warning at every start about something which
 * cannot happen is one people learn to skip.
 * <p>
 * That the warning still reaches an application which really has a writing handler is asserted
 * in the Spring Boot module, with an extension of its own next to the cockpit. The wording and
 * the condition belong to the platform and are asserted there.
 */
@ExtendWith(SuppressOutputExtension.class)
public class DetailsProvidersBringNoSecondWriterTest {

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
              .addClass(TestWorkflowModuleDetails.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", "http://localhost:1")
      .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
      .assertLogRecords(records -> {
        final List<String> aboutTheCase = records
            .stream()
            .map(LogRecord::getMessage)
            .filter(message -> (message != null) && message.contains(ConfigurationKeys.EXTENSION_ID))
            .filter(message -> message.contains(TestAggregate.class.getName()))
            .toList();

        assertTrue(
            aboutTheCase.isEmpty(),
            "the boot warned about the aggregate of a details provider: "
                + aboutTheCase);
      });

  @Test
  @DisplayName("A boot with details providers says nothing about a second writer")
  public void theDetailsProvidersBringNoSecondWriter() {

    // the assertion is the one made on the boot's log records above

  }

}
