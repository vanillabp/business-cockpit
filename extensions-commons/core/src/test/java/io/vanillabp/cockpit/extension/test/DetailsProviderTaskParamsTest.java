package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.handler.DetailsProviderTaskParams;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.usertask.PrefilledUserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.service.TaskParam;

/**
 * Which process variables the providers of one user task read, as a BPMS half asks for them.
 * <p>
 * The methods below are handed over the way VanillaBP hands them over while it scans a workflow
 * service: one occurrence of the annotation together with the method carrying it.
 */
@ExtendWith(SuppressOutputExtension.class)
public class DetailsProviderTaskParamsTest {

  private final DetailsProviderTaskParams taskParams = new DetailsProviderTaskParams();

  @Test
  @DisplayName("A provider naming the task definition reads its @TaskParam names for that task")
  public void aProviderNamingTheTaskDefinition() {

    note("byTaskDefinition");

    assertEquals(List.of("amount", "customer"), taskParams.namesReadBy(List.of("Activity_approve", "approve")));
    assertEquals(List.of(), taskParams.namesReadBy(List.of("Activity_sign", "sign")));

  }

  @Test
  @DisplayName("A provider naming the element id reads its names for that element only")
  public void aProviderNamingTheElementId() {

    note("byElementId");

    assertEquals(List.of("signer"), taskParams.namesReadBy(List.of("Activity_sign", "sign")));
    assertEquals(List.of(), taskParams.namesReadBy(List.of("Activity_other", "sign")));

  }

  @Test
  @DisplayName("A provider naming nothing is matched by its method name")
  public void aProviderNamingNothing() {

    note("review");

    assertEquals(List.of("reviewer"), taskParams.namesReadBy(List.of("Activity_review", "review")));

  }

  @Test
  @DisplayName("What a provider of every task reads counts for every task")
  public void aProviderOfEveryTask() {

    note("byTaskDefinition");
    note("everyTask");

    assertEquals(
        List.of("amount", "customer", "region"),
        taskParams.namesReadBy(List.of("Activity_approve", "approve")));
    assertEquals(List.of("region"), taskParams.namesReadBy(List.of("Activity_sign", "sign")));

  }

  @Test
  @DisplayName("A provider without @TaskParam reads nothing, and noting it twice changes nothing")
  public void nothingIsReadTwice() {

    note("withoutTaskParams");
    note("byTaskDefinition");
    note("byTaskDefinition");

    assertEquals(List.of("amount", "customer"), taskParams.namesReadBy(List.of("approve")));

  }

  /**
   * Hands one method of {@link Providers} over, the way VanillaBP's scan does.
   */
  private void note(
      final String methodName) {

    final Method method = Arrays
        .stream(Providers.class.getMethods())
        .filter(candidate -> candidate.getName().equals(methodName))
        .findFirst()
        .orElseThrow();
    taskParams.note(method.getAnnotation(UserTaskDetailsProvider.class), method);

  }

  /**
   * The details providers of a made-up workflow service. They are never called.
   */
  public static class Providers {

    @UserTaskDetailsProvider(taskDefinition = "approve")
    public UserTaskDetails byTaskDefinition(
        final PrefilledUserTaskDetails prefilled,
        @TaskParam("customer") final String customer,
        @TaskParam("amount") final Long amount) {

      return prefilled;

    }

    @UserTaskDetailsProvider(id = "Activity_sign")
    public UserTaskDetails byElementId(
        final PrefilledUserTaskDetails prefilled,
        @TaskParam("signer") final String signer) {

      return prefilled;

    }

    @UserTaskDetailsProvider
    public UserTaskDetails review(
        final PrefilledUserTaskDetails prefilled,
        @TaskParam("reviewer") final String reviewer) {

      return prefilled;

    }

    @UserTaskDetailsProvider(taskDefinition = UserTaskDetailsProvider.ALL)
    public UserTaskDetails everyTask(
        final PrefilledUserTaskDetails prefilled,
        @TaskParam("region") final String region) {

      return prefilled;

    }

    @UserTaskDetailsProvider(taskDefinition = "approve", version = "2")
    public UserTaskDetails withoutTaskParams(
        final PrefilledUserTaskDetails prefilled) {

      return prefilled;

    }

  }

}
