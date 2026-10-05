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
import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;
import io.vanillabp.integration.spi.PhaseTwoRetryLater;
import io.vanillabp.integration.test.utils.CapturedOutput;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A changed workflow aggregate which the BPMS half could not name in the application's
 * transaction: how long its entry is tried again, what happens to it after that, and which key it
 * is planned under.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ChangeResolvedWhenDispatchedTest {

  /** What the outbox blocks an entry after, unless the application configured otherwise. */
  private static final int DEFAULT_BLOCK_AFTER_ATTEMPTS = 50;

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
  @DisplayName("Ten minutes of a BPMS which has not written the workflow fit into the default number of attempts")
  public void tenMinutesFitIntoTheDefaultAttempts() {

    // the outbox counts every attempt, and the time it takes to pick a due entry up only makes
    // the window shorter in attempts. So this counts the distances alone, which is the worst case
    var waited = Duration.ZERO;
    var failedAttempts = 0;
    while (waited.compareTo(BusinessCockpitExtension.CHANGE_RESOLUTION_WINDOW) < 0) {
      waited = waited.plus(BusinessCockpitExtension.distanceToTheNextAttempt(waited));
      failedAttempts++;
    }
    assertTrue(
        failedAttempts < DEFAULT_BLOCK_AFTER_ATTEMPTS,
        "%d failed attempts before the window ends".formatted(failedAttempts));

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
        logged.contains("set the blocked entry back to open"),
        "the log says what to do: %s".formatted(logged));

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
