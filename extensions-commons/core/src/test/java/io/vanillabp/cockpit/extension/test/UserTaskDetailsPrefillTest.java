package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.spi.UserTaskDetailsPrefill;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What a BPMS half may hand over.
 * <p>
 * The values are copied so that nothing changes them afterwards, and the copy has to take a
 * process variable which the engine holds as nothing: Camunda 7 delivers those routinely, and a
 * bridge which had to filter them first would filter them in three repositories.
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

}
