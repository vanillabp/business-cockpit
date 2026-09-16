package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.handler.BusinessCockpitHandlers;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The keys one user task is looked up by, and the order they are offered in.
 * <p>
 * Which method a task ends up at is asserted where a user sees it, by the tests about one
 * provider for every user task. This one is about the edges those tests cannot show, a task
 * which carries only one of the two names and a name which is there but empty.
 */
@ExtendWith(SuppressOutputExtension.class)
public class LookupKeysTest {

  @Test
  @DisplayName("The element id is offered before the task definition")
  public void theElementIdComesFirst() {

    assertEquals(
        List.of("Activity_approve", "approve"),
        BusinessCockpitHandlers.lookupKeysOf("approve", "Activity_approve"));

  }

  @Test
  @DisplayName("A task without an element id is looked up by its task definition alone")
  public void aTaskWithoutAnElementId() {

    assertEquals(List.of("approve"), BusinessCockpitHandlers.lookupKeysOf("approve", null));
    assertEquals(List.of("approve"), BusinessCockpitHandlers.lookupKeysOf("approve", "  "));

  }

  @Test
  @DisplayName("A task without a task definition is looked up by its element id alone")
  public void aTaskWithoutATaskDefinition() {

    assertEquals(
        List.of("Activity_approve"),
        BusinessCockpitHandlers.lookupKeysOf(null, "Activity_approve"));
    assertEquals(
        List.of("Activity_approve"),
        BusinessCockpitHandlers.lookupKeysOf("  ", "Activity_approve"));

  }

  @Test
  @DisplayName("A task named neither way offers no key, so the method for every task runs")
  public void aTaskNamedNeitherWay() {

    assertEquals(List.of(), BusinessCockpitHandlers.lookupKeysOf(null, null));

  }

}
