package io.vanillabp.cockpit.extension.quarkus.it;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A called process is held against the providers of its user tasks only, in its own version. The
 * provider of the workflow is not held against it, because it never gets a workflow report of its
 * own.
 */
@ExtendWith(SuppressOutputExtension.class)
public class DeployedVersionOfACalledProcessCoveredTest {

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
              .addClass(DeployedVersionSecondaryService.class)
              .addClass(TestBpmsBridge.class)
              .addClass(TestWorkflowAwareness.class)
              .addClass(TestWorkflowModuleDetails.class)
              .addClass(DeployedVersionProcessVersions.class)
              .addClass(DeployedVersionUserTasks.class)
              .addClass(DeployedVersionCalledProcessInTheSameFile.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", "http://localhost:1");

  @Test
  @DisplayName("A called process is held against its user task providers only, in its own version")
  public void theApplicationStarts() {

    // reaching this method is the assertion: the start ran through

  }

}
