package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.workflow.PrefilledWorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;

/**
 * A details provider reads the suspension of the process instance and cannot change it. Only the
 * BPMS suspends and activates an instance, and the cockpit takes the state only from the reports of
 * a suspension and an activation, which no details provider takes part in.
 * <p>
 * What a provider can write is what the SPI types it receives or returns offer: the setters of the
 * prefilled details and the getters of the details it returns, which the extension copies over.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TheSuspensionIsReadOnlyForADetailsProviderTest {

  private static List<String> suspensionMethods(
      final Class<?> type) {

    return Arrays
        .stream(type.getMethods())
        .map(Method::getName)
        .filter(name -> name.contains("Suspended"))
        .sorted()
        .toList();

  }

  @Test
  @DisplayName("The prefilled details of a user task offer the suspension to read only")
  public void aUserTaskProviderReadsTheSuspension() {

    assertEquals(
        List.of("getSuspendedAt", "getSuspendedBy"),
        suspensionMethods(PrefilledUserTaskDetails.class));
    assertTrue(
        suspensionMethods(UserTaskDetails.class).isEmpty(),
        "the details a provider returns cannot carry a suspension");

  }

  @Test
  @DisplayName("The prefilled details of a workflow offer the suspension to read only")
  public void aWorkflowProviderReadsTheSuspension() {

    assertEquals(
        List.of("getSuspendedAt", "getSuspendedBy"),
        suspensionMethods(PrefilledWorkflowDetails.class));
    assertTrue(
        suspensionMethods(WorkflowDetails.class).isEmpty(),
        "the details a provider returns cannot carry a suspension");

  }

}
