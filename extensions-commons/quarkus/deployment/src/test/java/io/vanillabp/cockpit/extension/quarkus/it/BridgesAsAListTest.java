package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;

/**
 * A BPMS half which produces its bridges as one list, next to a bridge registered as a bean of
 * its own. Both shapes reach the extension, and a dispatch finds the half of the adapter it
 * names. The Spring Boot module checks the same, because what one platform accepts the other has
 * to accept as well.
 */
@ExtendWith(SuppressOutputExtension.class)
public class BridgesAsAListTest {

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
              .addClass(TestWorkflowModuleDetails.class)
              .addClass(BridgesOfASecondBpms.class)
              .addClass(BridgesOfASecondBpms.SecondBpmsBridge.class))
      .overrideRuntimeConfigKey(
          "vanillabp.cockpit.rest.base-url", "http://localhost:1");

  @Inject
  BusinessCockpitExtension extension;

  @Test
  @DisplayName("A BPMS half registered as one list of bridges serves its adapter")
  public void bridgesRegisteredAsAListAreCollected() {

    assertEquals(
        BridgesOfASecondBpms.ADAPTER_ID,
        extension.bridgeOf(BridgesOfASecondBpms.ADAPTER_ID).adapterId(),
        "the bridge of the list serves its adapter");
    assertEquals(
        TestBpmsBridge.ADAPTER_ID,
        extension.bridgeOf(TestBpmsBridge.ADAPTER_ID).adapterId(),
        "and the bridge registered as a bean of its own still does");

  }

}
