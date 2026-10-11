package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
 * A workflow module which sets nothing for the Business Cockpit, but configures a workflow of its
 * BPMS, starts and reports nothing.
 * <p>
 * The workflows below a module belong to the platform. This platform hands the cockpit's mapping
 * an entry for every one of them, whatever it contains. If the cockpit read such an entry as one
 * of its own workflows, a module which opted out would look like a module with missing settings.
 * The boot would then end and ask for an address nobody wants to give.
 */
@ExtendWith(SuppressOutputExtension.class)
public class WorkflowsOfTheAdapterTest {

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .withApplicationRoot(
          jar -> jar
              .addAsResource("workflows-of-the-adapter.yaml", "application.yaml")
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
  @DisplayName("A workflow carrying only the adapter's keys leaves the module out of the cockpit")
  public void theModuleTakesNoPart() {

    assertFalse(
        BusinessCockpitConfiguration
            .readAndValidate(properties, settings, false, false)
            .reportsToTheCockpit("test-module"),
        "the module was read as one which reports, although it says nothing about the cockpit");

  }

}
