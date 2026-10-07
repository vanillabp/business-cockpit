package io.vanillabp.cockpit.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.outbox.BusinessCockpitOperations;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.integration.adapter.migration.config.PhaseTwoOutboxProperties;
import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;
import io.vanillabp.integration.spi.PhaseTwoRetryLater;
import io.vanillabp.integration.test.utils.CapturedOutput;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A changed workflow aggregate which the BPMS half could not name in the application's
 * transaction, and a changed user task whose report it could not build there: how long the entry
 * is tried again, what happens to it after that, and which key it is planned under.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ChangeResolvedWhenDispatchedTest {

  /** How many attempts ten minutes would take with two seconds between them. */
  private static final int ATTEMPTS_TWO_SECONDS_APART = 300;

  @Test
  @DisplayName("The first attempts come two seconds apart")
  public void theFirstAttemptsAreTwoSecondsApart() {

    assertEquals(Duration.ofSeconds(2), BusinessCockpitExtension.distanceToTheNextAttempt(Duration.ZERO));
    assertEquals(
        Duration.ofSeconds(2),
        BusinessCockpitExtension.distanceToTheNextAttempt(Duration.ofSeconds(19)));
    assertEquals(
        Duration.ofSeconds(60),
        BusinessCockpitExtension.distanceToTheNextAttempt(Duration.ofMinutes(10)));

  }

  @Test
  @DisplayName("A BPMS which stays behind for ten minutes is asked far less often than every two seconds")
  public void tenMinutesTakeFarFewerAttemptsThanTwoSecondsApart() {

    // counts the distances alone. The time the outbox takes to pick a due entry up only makes
    // the number smaller
    var waited = Duration.ZERO;
    var attempts = 0;
    while (waited.compareTo(BusinessCockpitExtension.CHANGE_RESOLUTION_WINDOW) < 0) {
      waited = waited.plus(BusinessCockpitExtension.distanceToTheNextAttempt(waited));
      attempts++;
    }
    assertEquals(46, attempts, "attempts in the window");
    assertTrue(attempts < ATTEMPTS_TWO_SECONDS_APART / 5);

  }

  @Test
  @DisplayName("A change inside the window is given back to the outbox")
  public void aChangeInsideTheWindowIsGivenBack() {

    final var happened = OffsetDateTime.now().minusMinutes(1);

    final var givenBack = assertThrows(
        PhaseTwoRetryLater.class,
        () -> BusinessCockpitExtension.tryTheChangeAgainLater(unnamedChange(), happened, true));

    assertTrue(
        givenBack.getRetryAfter().compareTo(Duration.ofSeconds(6)) >= 0,
        "a change which waited a minute comes again in %s".formatted(givenBack.getRetryAfter()));

  }

  @Test
  @DisplayName("A change which waited the whole window blocks its entry and says so at ERROR")
  public void aChangeAfterTheWindowBlocksItsEntry(
      final CapturedOutput output) {

    final var happened = OffsetDateTime
        .now()
        .minus(BusinessCockpitExtension.CHANGE_RESOLUTION_WINDOW)
        .minusSeconds(1);

    final var blocked = assertThrows(
        PhaseTwoPermanentFailure.class,
        () -> BusinessCockpitExtension.tryTheChangeAgainLater(unnamedChange(), happened, true));

    assertTrue(PhaseTwoPermanentFailure.isPermanent(blocked));
    final var logged = output.getAllOfThisTest();
    assertTrue(
        logged.contains("ERROR") && logged.contains("The outbox entry is now blocked"),
        "the log says that the entry is blocked, at ERROR: %s".formatted(logged));
    assertTrue(
        logged.contains("Check that the aggregate has a workflow in adapter 'camunda8'"),
        "the log says what to check: %s".formatted(logged));
    assertTrue(
        logged.contains("LAST_FAILURE") && logged.contains("lastFailure"),
        "the log names where the entry keeps its reason: %s".formatted(logged));
    assertTrue(
        logged.contains(PhaseTwoOutboxProperties.BLOCKED_ENTRIES_GUIDE),
        "the log links to how to open a blocked entry again: %s".formatted(logged));

  }

  @Test
  @DisplayName("A change of a workflow not named yet is keyed by its aggregate, so two cases never share a key")
  public void aChangeWithoutItsWorkflowIsKeyedByItsAggregate() {

    final var first = keyOf("aggregate-1", Map.of(BusinessCockpitOperations.ARG_EVENT_KIND, "UPDATED"));
    final var second = keyOf("aggregate-2", Map.of(BusinessCockpitOperations.ARG_EVENT_KIND, "UPDATED"));

    assertNotEquals(first, second);
    assertEquals(first, keyOf("aggregate-1", Map.of(BusinessCockpitOperations.ARG_EVENT_KIND, "UPDATED")));

  }

  @Test
  @DisplayName("An entry which names its workflow keeps the key it always had")
  public void anEntryNamingItsWorkflowKeepsItsKey() {

    assertEquals(
        "businesscockpit:PUBLISH_WORKFLOW_EVENT|camunda8|4711|UPDATED",
        keyOf(
            "aggregate-1",
            Map
                .of(
                    BusinessCockpitOperations.ARG_EVENT_KIND, "UPDATED",
                    BusinessCockpitOperations.ARG_WORKFLOW_ID, "4711",
                    BusinessCockpitOperations.ARG_RESOLVED_WHEN_DISPATCHED, "true")));

  }

  @Test
  @DisplayName("A changed user task which waited the whole window blocks its entry and names the task at ERROR")
  public void aUserTaskChangeAfterTheWindowBlocksItsEntry(
      final CapturedOutput output) {

    final var happened = OffsetDateTime
        .now()
        .minus(BusinessCockpitExtension.CHANGE_RESOLUTION_WINDOW)
        .minusSeconds(1);
    final var userTask = new UserTaskReference(
        "camunda8", "module", "Process", null, "aggregate-1", null, "4711", null, null);
    final var call = PhaseTwoCall
        .of(
            BusinessCockpitOperations.publishUserTaskEvent(), "module", "Process", "aggregate-1", "camunda8",
            Map
                .of(
                    BusinessCockpitOperations.ARG_EVENT_KIND, "UPDATED",
                    BusinessCockpitOperations.ARG_USER_TASK_ID, "4711",
                    BusinessCockpitOperations.ARG_RESOLVED_WHEN_DISPATCHED, "true"));

    assertThrows(
        PhaseTwoPermanentFailure.class,
        () -> BusinessCockpitExtension
            .tryTheChangeAgainLater(
                call, happened, true, BusinessCockpitExtension.WaitingFor.userTask(userTask)));

    final var logged = output.getAllOfThisTest();
    assertTrue(
        logged.contains("ERROR") && logged.contains("the change of user task '4711'"),
        "the log names the task at ERROR: %s".formatted(logged));
    assertTrue(
        logged.contains("Check that user task '4711' belongs to aggregate 'aggregate-1' in adapter 'camunda8'"),
        "the log says what to check: %s".formatted(logged));

  }

  @Test
  @DisplayName("A changed user task inside the window is given back to the outbox with the distance of a workflow")
  public void aUserTaskChangeInsideTheWindowIsGivenBack() {

    final var userTask = new UserTaskReference(
        "camunda8", "module", "Process", null, "aggregate-1", null, "4711", null, null);

    final var givenBack = assertThrows(
        PhaseTwoRetryLater.class,
        () -> BusinessCockpitExtension
            .tryTheChangeAgainLater(
                unnamedChange(), OffsetDateTime.now().minusMinutes(1), true,
                BusinessCockpitExtension.WaitingFor.userTask(userTask)));

    assertTrue(givenBack.getRetryAfter().compareTo(Duration.ofSeconds(6)) >= 0);

  }

  @Test
  @DisplayName("A user-task entry which names no task is keyed by its aggregate, and one which names its task keeps its key")
  public void aUserTaskEntryWithoutItsTaskIsKeyedByItsAggregate() {

    final var first = userTaskKeyOf("aggregate-1", Map.of(BusinessCockpitOperations.ARG_EVENT_KIND, "UPDATED"));
    final var second = userTaskKeyOf("aggregate-2", Map.of(BusinessCockpitOperations.ARG_EVENT_KIND, "UPDATED"));

    assertNotEquals(first, second);
    assertEquals(
        "businesscockpit:PUBLISH_USER_TASK_EVENT|camunda8|4711|UPDATED",
        userTaskKeyOf(
            "aggregate-1",
            Map
                .of(
                    BusinessCockpitOperations.ARG_EVENT_KIND, "UPDATED",
                    BusinessCockpitOperations.ARG_USER_TASK_ID, "4711",
                    BusinessCockpitOperations.ARG_RESOLVED_WHEN_DISPATCHED, "true")));

  }

  private static String userTaskKeyOf(
      final String workflowAggregateId,
      final Map<String, String> args) {

    return PhaseTwoCall
        .of(
            BusinessCockpitOperations.publishUserTaskEvent(), "module", "Process", workflowAggregateId, "camunda8",
            args)
        .idempotencyKey()
        .orElseThrow();

  }

  private static PhaseTwoCall unnamedChange() {

    return PhaseTwoCall
        .of(
            BusinessCockpitOperations.publishWorkflowEvent(), "module", "Process", "aggregate-1", "camunda8",
            Map
                .of(
                    BusinessCockpitOperations.ARG_EVENT_KIND, "UPDATED",
                    BusinessCockpitOperations.ARG_RESOLVED_WHEN_DISPATCHED, "true"));

  }

  private static String keyOf(
      final String workflowAggregateId,
      final Map<String, String> args) {

    return PhaseTwoCall
        .of(
            BusinessCockpitOperations.publishWorkflowEvent(), "module", "Process", workflowAggregateId, "camunda8",
            args)
        .idempotencyKey()
        .orElseThrow();

  }

}
