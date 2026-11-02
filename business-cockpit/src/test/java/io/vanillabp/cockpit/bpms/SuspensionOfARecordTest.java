package io.vanillabp.cockpit.bpms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The edges of the rules about a suspension which no report of a running application reaches: a
 * report without a timestamp, a time read back from MongoDB in another offset, and an activation
 * which names nobody. The ordinary cases are tested through the ways in.
 */
@ExtendWith(SuppressOutputExtension.class)
class SuspensionOfARecordTest {

  private static final OffsetDateTime SUSPENDED_AT = OffsetDateTime.parse("2026-11-01T09:00:00Z");

  private static UserTask suspendedTask() {

    final var task = new UserTask();
    task.setSuspendedAt(SUSPENDED_AT);
    task.setSuspendedBy("olivia");
    task.setSuspensionChangedAt(SUSPENDED_AT);
    return task;

  }

  @Test
  void theSameMomentInAnotherOffsetIsARepeat() {

    final var task = suspendedTask();

    final var outcome = SuspensionOfARecord.suspend(
        task, SUSPENDED_AT.withOffsetSameInstant(ZoneOffset.ofHours(2)), "olivia");

    assertEquals(SuspensionOfARecord.Outcome.REPEATED, outcome, "nothing has to be saved");

  }

  @Test
  void aReportWithoutATimestampKeepsTheMarkItIsComparedWith() {

    final var task = suspendedTask();

    final var outcome = SuspensionOfARecord.activate(task, null, "sam");

    assertEquals(SuspensionOfARecord.Outcome.CHANGED, outcome);
    assertNull(task.getSuspendedAt());
    assertEquals(SUSPENDED_AT, task.getSuspensionChangedAt());

  }

  @Test
  void anActivationWhichNamesNobodyRemovesThePersonOfTheSuspension() {

    // the person who suspended did not activate, and the cockpit must not say so
    final var task = suspendedTask();

    final var outcome = SuspensionOfARecord.activate(task, SUSPENDED_AT.plusHours(1), null);

    assertEquals(SuspensionOfARecord.Outcome.CHANGED, outcome);
    assertNull(task.getSuspendedAt());
    assertNull(task.getSuspendedBy());

  }

  @Test
  void aRepeatedActivationIsARepeat() {

    final var task = suspendedTask();
    SuspensionOfARecord.activate(task, SUSPENDED_AT.plusHours(1), "sam");

    final var outcome = SuspensionOfARecord.activate(task, SUSPENDED_AT.plusHours(1), "sam");

    assertEquals(SuspensionOfARecord.Outcome.REPEATED, outcome);

  }

}
