package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.config.CockpitSettings;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;

/**
 * A workflow module starts although it sets neither the language of its BPMN names nor the
 * languages it reports in, because its one workflow sets both.
 * <p>
 * Version 1 allowed this too, but it checked at the first event of a workflow. This version
 * checks while the application starts, once VanillaBP has said which processes the module holds.
 * The boot of this application is the assertion: the check accepts a module whose workflows set
 * what the module left out.
 */
@ExtendWith(SuppressOutputExtension.class)
public class WorkflowSaysWhatItsModuleLeftOutTest {

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("left-to-the-workflows.yaml", "application.yaml")
              .addAsResource("test-module/processes/dummy/TestProcess.bpmn")
              .addAsResource(
                  "workflow-module-descriptor/workflow-module", "META-INF/workflow-module")
              .addClass(TestAggregate.class)
              .addClass(TestAggregatePersistence.class)
              .addClass(TestWorkflowService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class));

  @Inject
  CockpitSettings settings;

  @Inject
  MigrationAdapterProperties properties;

  @Test
  @DisplayName("What the workflow says is what its module reports with")
  public void theWorkflowSaysItForItsModule() {

    final var module = BusinessCockpitConfiguration
        .readAndValidate(properties, settings, false, false)
        .workflowModule("test-module");

    assertEquals(List.of("fr"), module.i18nLanguages("TestProcess"));
    assertEquals("fr", module.bpmnDescriptionLanguage("TestProcess"));

  }

}
