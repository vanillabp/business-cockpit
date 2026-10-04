package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.spi.UserTaskDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.WorkflowDetailsPrefill;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What a BPMS half may hand over.
 * <p>
 * The values are copied so that nothing changes them afterwards, and the copy has to take a
 * process variable which the engine holds as nothing. Camunda 7 delivers those all the time, and
 * a bridge which had to filter them first would filter them in three repositories.
 */
@ExtendWith(SuppressOutputExtension.class)
public class UserTaskDetailsPrefillTest {

  @Test
  @DisplayName("A process variable set to nothing is taken as it is")
  public void aVariableSetToNothingIsTaken() {

    final var variables = new HashMap<String, Object>();
    variables.put("amount", 250);
    variables.put("approvedBy", null);

    final var prefill = UserTaskDetailsPrefill.builder().variables(variables).build();

    assertEquals(2, prefill.variables().size());
    assertEquals(250, prefill.variables().get("amount"));
    assertTrue(prefill.variables().containsKey("approvedBy"));
    assertNull(prefill.variables().get("approvedBy"));

  }

  @Test
  @DisplayName("What a BPMS half left out is empty rather than absent")
  public void whatIsLeftOutIsEmpty() {

    final var prefill = UserTaskDetailsPrefill.builder().assignee("anna").build();

    assertEquals(List.of(), prefill.candidateUsers());
    assertEquals(List.of(), prefill.candidateGroups());
    assertTrue(prefill.variables().isEmpty());
    assertTrue(prefill.multiInstances().isEmpty());

  }

  @Test
  @DisplayName("A BPMS half which builds the values the way it did before says nothing about the start")
  public void theValuesOfBeforeLeaveTheStartEmpty() {

    final var prefill = new UserTaskDetailsPrefill(
        "1", "workflow-1", null, "4711", "Approve", "Order handling", null, "anna", null, null, null, null, null, null);
    final var workflow = new WorkflowDetailsPrefill("1", "4711", "Order handling", null);

    assertNull(prefill.createdAt());
    assertNull(workflow.createdAt());
    assertEquals(
        OffsetDateTime.parse("2026-10-01T08:00:00Z"),
        UserTaskDetailsPrefill.builder().createdAt(OffsetDateTime.parse("2026-10-01T08:00:00Z")).build().createdAt());

  }

}
