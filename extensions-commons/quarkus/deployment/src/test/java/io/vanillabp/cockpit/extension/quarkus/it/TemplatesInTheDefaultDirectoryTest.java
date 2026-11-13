package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.BusinessCockpitAssembly;
import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.config.CockpitSettings;
import io.vanillabp.cockpit.extension.templating.Templating;
import io.vanillabp.cockpit.extension.test.support.CockpitServer;
import io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;

/**
 * A workflow module keeps its templates in <code>&lt;module id&gt;/business-cockpit-templates</code>
 * and configures nothing. On this platform the class loader of the application is Quarkus' own, so
 * the test proves that it finds the directory and loads a template from it.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TemplatesInTheDefaultDirectoryTest {

  private static final String WORKFLOW_MODULE = "test-module";

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("business-cockpit.yaml", "application.yaml")
              .addAsResource("test-module/processes/dummy/TestProcess.bpmn")
              .addAsResource(
                  "module-templates/workflow-title.ftl",
                  "test-module/business-cockpit-templates/workflow-title.ftl")
              .addAsResource(
                  "workflow-module-descriptor/workflow-module", "META-INF/workflow-module")
              .addClass(TestAggregate.class)
              .addClass(TestAggregatePersistence.class)
              .addClass(TestWorkflowService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class)
              .addClass(CockpitServer.class))
      .overrideRuntimeConfigKey("vanillabp.cockpit.rest.base-url", CockpitServer.baseUrl());

  @Inject
  CockpitSettings settings;

  @Inject
  MigrationAdapterProperties properties;

  @Test
  @DisplayName("The templates of a module are found in its default directory")
  public void theTemplatesAreFoundInTheDefaultDirectory() {

    final var configuration = BusinessCockpitConfiguration
        .readAndValidate(properties, settings, Templating.engineAvailable(), false);

    assertNotNull(configuration.workflowModule(WORKFLOW_MODULE).templateDirectory());
    assertEquals(
        Optional.of("Order 4711"),
        BusinessCockpitAssembly
            .templatingOf(configuration)
            .render(
                List.of(WORKFLOW_MODULE), Templating.WORKFLOW_TITLE, Locale.ENGLISH,
                Map.of("number", "4711"), Map.of()));

  }

}
