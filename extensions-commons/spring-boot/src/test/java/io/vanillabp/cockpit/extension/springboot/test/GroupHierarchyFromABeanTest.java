package io.vanillabp.cockpit.extension.springboot.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.vanillabp.cockpit.extension.springboot.hierarchy.ChangingGroupHierarchy;
import io.vanillabp.cockpit.extension.test.support.CockpitServer;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleGroupHierarchyChangedEvent;

/**
 * A workflow module whose group hierarchy comes from a bean, and changes while the application
 * runs.
 * <p>
 * The configuration of this test is the one of the test application without
 * <code>group-hierarchy</code>, because the bean and the key together end the start.
 */
@SpringBootTest(classes = {
    TestApplication.class, ChangingGroupHierarchy.class
},
    properties = {
        "spring.config.location=classpath:/group-hierarchy-from-a-bean/",
        // an outbox store of its own, for the reason given in BusinessCockpitExtensionTest
        "spring.datasource.url=jdbc:h2:mem:cockpit-group-hierarchy-from-a-bean;DB_CLOSE_DELAY=-1"
    })
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class GroupHierarchyFromABeanTest {

  private static final long WAIT_MILLIS = 20_000;

  @DynamicPropertySource
  static void cockpitServer(
      final DynamicPropertyRegistry registry) {

    registry.add("vanillabp.cockpit.rest.base-url", CockpitServer::baseUrl);

  }

  @Autowired
  private ChangingGroupHierarchy hierarchy;

  @Autowired
  private ApplicationEventPublisher events;

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

    final var atStart = awaitRegistrationWith(ChangingGroupHierarchy.GROUP_AT_START);
    assertFalse(atStart.body().contains("\"group\":\"TEAM_LEAD\""), atStart.body());

    hierarchy.changeTo(Map.of("CHANGED_LEAD", List.of("CHANGED_MEMBER")));
    events.publishEvent(new WorkflowModuleGroupHierarchyChangedEvent("test-module"));

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
        () -> events.publishEvent(new WorkflowModuleGroupHierarchyChangedEvent("no-such-module")));

    assertTrue(failure.getMessage().contains("'no-such-module'"), failure.getMessage());
    assertTrue(failure.getMessage().contains("test-module"), failure.getMessage());

  }

}
