package io.vanillabp.cockpit.bpms;

import java.time.OffsetDateTime;

/**
 * How a report about the suspension or the activation of a process instance changes a user task
 * or a workflow. The same rules hold for both, so they are written once.
 * <p>
 * The reports overtake each other like every other report, see {@link OrderOfReports}. So the
 * cockpit orders them by the timestamp of their event:
 * <ul>
 * <li>A suspension sets {@code suspendedAt} to its timestamp. A record which is suspended already
 * keeps the moment of its first suspension, so a repeat from a second node of the application
 * changes nothing.</li>
 * <li>An activation removes {@code suspendedAt}.</li>
 * <li>Each of them is left out where the record holds a younger suspension or activation. A
 * suspension is also left out where the record holds a younger change: only a running process
 * instance reports a change, so the instance was not suspended any more.</li>
 * <li>A change which is younger than the latest suspension removes {@code suspendedAt}, for the same
 * reason. This heals an activation the workflow module never reported, for example because it was
 * not running at that moment.</li>
 * <li>An end removes {@code suspendedAt} in any case, and an ended record takes no suspension. An
 * ended record is never shown as suspended.</li>
 * </ul>
 * Neither report changes {@code latestEventAt}, so they never make a change look old.
 */
public final class SuspensionOfARecord {

  /** What a report about the suspension did to the record. */
  public enum Outcome {

    /** The record changed and has to be saved. */
    CHANGED,

    /** The record says this already, for example because another node reported it before. */
    REPEATED,

    /** The record holds a younger report, so this one says nothing which is still true. */
    TOO_OLD,

    /** The record has ended, and an ended record is never suspended. */
    ENDED

  }

  private SuspensionOfARecord() {
  }

  /**
   * Lays a report onto the record that its process instance was suspended.
   *
   * @param record The user task or the workflow the report is about
   * @param eventTimestamp When the process instance was suspended, by the clock of the workflow
   *        module
   * @return What the report did to the record
   */
  public static Outcome suspend(
      final SuspendableRecord record,
      final OffsetDateTime eventTimestamp) {

    if (record.getEndedAt() != null) {
      return Outcome.ENDED;
    }
    final var olderThanTheLastSuspension = OrderOfReports
        .isOlderThanWhatIsStored(eventTimestamp, record.getSuspensionChangedAt());
    final var olderThanTheLastChange = OrderOfReports
        .isOlderThanWhatIsStored(eventTimestamp, record.getLatestEventAt());
    if (olderThanTheLastSuspension || olderThanTheLastChange) {
      return Outcome.TOO_OLD;
    }

    final var suspendedAt = record.getSuspendedAt() != null
        ? record.getSuspendedAt()
        : eventTimestamp;
    return changeTo(record, suspendedAt, eventTimestamp);

  }

  /**
   * Lays a report onto the record that its process instance was activated again.
   *
   * @param record The user task or the workflow the report is about
   * @param eventTimestamp When the process instance was activated, by the clock of the workflow
   *        module
   * @return What the report did to the record
   */
  public static Outcome activate(
      final SuspendableRecord record,
      final OffsetDateTime eventTimestamp) {

    if (record.getEndedAt() != null) {
      return Outcome.ENDED;
    }
    if (OrderOfReports.isOlderThanWhatIsStored(eventTimestamp, record.getSuspensionChangedAt())) {
      return Outcome.TOO_OLD;
    }
    return changeTo(record, null, eventTimestamp);

  }

  /**
   * Removes the suspension from a record where a change of it is younger than the suspension.
   * Only a running process instance reports a change, so the instance was activated in between.
   *
   * @param record The user task or the workflow the change is about
   * @param eventTimestamp When the change happened, by the clock of the workflow module
   */
  public static void activatedByAYoungerChange(
      final SuspendableRecord record,
      final OffsetDateTime eventTimestamp) {

    if ((record.getSuspendedAt() == null) || (eventTimestamp == null)) {
      return;
    }
    // a change at the same moment as the suspension may belong to the suspension itself, so
    // only a change which is really later counts
    final var suspensionChangedAt = record.getSuspensionChangedAt();
    if ((suspensionChangedAt != null) && !eventTimestamp.isAfter(suspensionChangedAt)) {
      return;
    }
    record.setSuspendedAt(null);
    record.setSuspensionChangedAt(eventTimestamp);

  }

  /**
   * Removes the suspension from a record which ends. An ended record is never shown as
   * suspended, whatever the order of the reports was.
   *
   * @param record The user task or the workflow which ends
   */
  public static void endedRecordIsNotSuspended(
      final SuspendableRecord record) {

    record.setSuspendedAt(null);

  }

  private static Outcome changeTo(
      final SuspendableRecord record,
      final OffsetDateTime suspendedAt,
      final OffsetDateTime eventTimestamp) {

    // a report without a timestamp cannot be ordered later, so it does not become the mark the
    // next report is compared with
    final var suspensionChangedAt = eventTimestamp != null
        ? eventTimestamp
        : record.getSuspensionChangedAt();
    final var sameMark = sameMoment(record.getSuspendedAt(), suspendedAt);
    final var sameChange = sameMoment(record.getSuspensionChangedAt(), suspensionChangedAt);
    if (sameMark && sameChange) {
      return Outcome.REPEATED;
    }
    record.setSuspendedAt(suspendedAt);
    record.setSuspensionChangedAt(suspensionChangedAt);
    return Outcome.CHANGED;

  }

  /**
   * MongoDB keeps the instant and not the offset of a time, so a time read back can carry another
   * offset than the same time in a report. Both are still the same moment.
   */
  private static boolean sameMoment(
      final OffsetDateTime one,
      final OffsetDateTime other) {

    if ((one == null) || (other == null)) {
      return one == other;
    }
    return one.isEqual(other);

  }

}
