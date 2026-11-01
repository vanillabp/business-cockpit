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
 * report without a timestamp, a time read back from MongoDB in another offset, and a change at the
 * very moment of the suspension. The ordinary cases are tested through the ways in.
 */
@ExtendWith(SuppressOutputExtension.class)
class SuspensionOfARecordTest {

  private static final OffsetDateTime SUSPENDED_AT = OffsetDateTime.parse("2026-11-01T09:00:00Z");

  private static UserTask suspendedTask() {

    final var task = new UserTask();
    task.setSuspendedAt(SUSPENDED_AT);
    task.setSuspensionChangedAt(SUSPENDED_AT);
    return task;

  }

  @Test
  void theSameMomentInAnotherOffsetIsARepeat() {

    final var task = suspendedTask();

    final var outcome = SuspensionOfARecord.suspend(
        task, SUSPENDED_AT.withOffsetSameInstant(ZoneOffset.ofHours(2)));

    assertEquals(SuspensionOfARecord.Outcome.REPEATED, outcome, "nothing has to be saved");

  }

  @Test
  void aReportWithoutATimestampKeepsTheMarkItIsComparedWith() {

    final var task = suspendedTask();

    final var outcome = SuspensionOfARecord.activate(task, null);

    assertEquals(SuspensionOfARecord.Outcome.CHANGED, outcome);
    assertNull(task.getSuspendedAt());
    assertEquals(SUSPENDED_AT, task.getSuspensionChangedAt());

  }

  @Test
  void aChangeAtTheMomentOfTheSuspensionKeepsTheMark() {

    // it may be the suspension itself, which some engines also report as a change
    final var task = suspendedTask();

    SuspensionOfARecord.activatedByAYoungerChange(task, SUSPENDED_AT);

    assertEquals(SUSPENDED_AT, task.getSuspendedAt());

  }

  @Test
  void aChangeWithoutATimestampKeepsTheMark() {

    final var task = suspendedTask();

    SuspensionOfARecord.activatedByAYoungerChange(task, null);

    assertEquals(SUSPENDED_AT, task.getSuspendedAt());

  }

  @Test
  void aChangeOfARecordWhichNoSuspensionReachedYetChangesNothing() {

    final var task = new UserTask();

    SuspensionOfARecord.activatedByAYoungerChange(task, SUSPENDED_AT);

    assertNull(task.getSuspendedAt());
    assertNull(task.getSuspensionChangedAt());

  }

}
