package io.vanillabp.cockpit.extension.quarkus.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.extension.test.support.CockpitServer;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleGroupHierarchyChangedEvent;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * A workflow module whose group hierarchy comes from a bean, and changes while the application
 * runs. The configuration of this test sets no <code>group-hierarchy</code>, because the bean and
 * the key together end the start.
 */
@ExtendWith(SuppressOutputExtension.class)
public class GroupHierarchyFromABeanTest {

  private static final long WAIT_MILLIS = 20_000;

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
              .addClass(ChangingGroupHierarchy.class)
              .addClass(CockpitServer.class))
      .overrideRuntimeConfigKey("vanillabp.cockpit.rest.base-url", CockpitServer.baseUrl());

  @Inject
  ChangingGroupHierarchy hierarchy;

  @Inject
  Event<WorkflowModuleGroupHierarchyChangedEvent> changes;

  /**
   * Waits for a registration of the module which carries the given group. Every test class of
   * this module registers the same module at the same server, so a registration is told apart
   * by the groups it carries.
   */
  private static CockpitServer.Request awaitRegistrationWith(
      final String group) throws InterruptedException {

    final var deadline = System.currentTimeMillis() + WAIT_MILLIS;
    while (System.currentTimeMillis() < deadline) {
      final var registration = CockpitServer
          .registrations()
          .stream()
          .filter(request -> request.body().contains("\"group\":\"%s\"".formatted(group)))
          .findFirst();
      if (registration.isPresent()) {
        return registration.get();
      }
      Thread.sleep(100);
    }
    throw new AssertionError("No registration carried the group "
        + group);

  }

  @Test
  @DisplayName("The module registers with the hierarchy of the bean, and again after a change")
  public void theModuleRegistersAgainAfterAChange() throws InterruptedException {

    awaitRegistrationWith(ChangingGroupHierarchy.GROUP_AT_START);

    hierarchy.changeTo(Map.of("CHANGED_LEAD", List.of("CHANGED_MEMBER")));
    changes.fire(new WorkflowModuleGroupHierarchyChangedEvent("test-module"));

    final var afterTheChange = awaitRegistrationWith("CHANGED_LEAD");
    assertTrue(afterTheChange.body().contains("CHANGED_MEMBER"), afterTheChange.body());
    assertFalse(
        afterTheChange.body().contains(ChangingGroupHierarchy.GROUP_AT_START),
        afterTheChange.body());

  }

  @Test
  @DisplayName("A change of a module which reports nothing fails the publisher")
  public void aChangeOfAnUnknownModuleFails() {

    final var failure = assertThrows(
        IllegalArgumentException.class,
        () -> changes.fire(new WorkflowModuleGroupHierarchyChangedEvent("no-such-module")));

    assertTrue(failure.getMessage().contains("'no-such-module'"), failure.getMessage());

  }

}
