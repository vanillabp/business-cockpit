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
 * A workflow module which says nothing about the Business Cockpit and configures a workflow of
 * its BPMS starts, and reports nothing.
 * <p>
 * The workflows below a module are the platform's, and this platform hands the cockpit's mapping
 * an entry for every one of them, whatever stands inside. Reading such an entry as a workflow of
 * the cockpit would turn a module which opted out into a module missing its settings, and the
 * boot would end asking for an address nobody wants to give.
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
            .readAndValidate(properties, settings, false)
            .reportsToTheCockpit("test-module"),
        "the module was read as one which reports, although it says nothing about the cockpit");

  }

}
