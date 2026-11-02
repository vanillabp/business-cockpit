package io.vanillabp.cockpit.bpms;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * How a report about the suspension or the activation of a process instance changes a user task
 * or a workflow. The same rules hold for both, so they are written once.
 * <p>
 * Only these two reports change the suspension. A user task is created by a running process
 * instance and completed by a running process instance, so a creation, a change, an end or a
 * change the application reports about the aggregate says nothing about it. All of them leave
 * {@code suspendedAt} and {@code suspendedBy} as they are.
 * <p>
 * The reports overtake each other like every other report, see {@link OrderOfReports}. So the
 * cockpit orders them by the timestamp of their event:
 * <ul>
 * <li>A suspension sets {@code suspendedAt} to its timestamp and {@code suspendedBy} to the person
 * it names. A record which is suspended already keeps the moment and the person of its first
 * suspension, so a repeat from a second node of the application changes nothing.</li>
 * <li>An activation removes {@code suspendedAt} and sets {@code suspendedBy} to the person who
 * activated the instance.</li>
 * <li>Each of them is left out where the record holds a younger suspension or activation.</li>
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

    /** The record holds a younger report about the suspension, so this one is out of date. */
    TOO_OLD

  }

  private SuspensionOfARecord() {
  }

  /**
   * Lays a report onto the record that its process instance was suspended.
   *
   * @param record The user task or the workflow the report is about
   * @param eventTimestamp When the process instance was suspended, by the clock of the workflow
   *        module
   * @param suspendedBy Who suspended the process instance, or {@code null} where the workflow
   *        system does not say
   * @return What the report did to the record
   */
  public static Outcome suspend(
      final SuspendableRecord record,
      final OffsetDateTime eventTimestamp,
      final String suspendedBy) {

    if (OrderOfReports.isOlderThanWhatIsStored(eventTimestamp, record.getSuspensionChangedAt())) {
      return Outcome.TOO_OLD;
    }

    if (record.getSuspendedAt() != null) {
      return changeTo(record, record.getSuspendedAt(), record.getSuspendedBy(), eventTimestamp);
    }
    return changeTo(record, eventTimestamp, suspendedBy, eventTimestamp);

  }

  /**
   * Lays a report onto the record that its process instance was activated again.
   *
   * @param record The user task or the workflow the report is about
   * @param eventTimestamp When the process instance was activated, by the clock of the workflow
   *        module
   * @param activatedBy Who activated the process instance, or {@code null} where the workflow
   *        system does not say
   * @return What the report did to the record
   */
  public static Outcome activate(
      final SuspendableRecord record,
      final OffsetDateTime eventTimestamp,
      final String activatedBy) {

    if (OrderOfReports.isOlderThanWhatIsStored(eventTimestamp, record.getSuspensionChangedAt())) {
      return Outcome.TOO_OLD;
    }
    return changeTo(record, null, activatedBy, eventTimestamp);

  }

  private static Outcome changeTo(
      final SuspendableRecord record,
      final OffsetDateTime suspendedAt,
      final String suspendedBy,
      final OffsetDateTime eventTimestamp) {

    // a report without a timestamp cannot be ordered later, so it does not become the mark the
    // next report is compared with
    final var suspensionChangedAt = eventTimestamp != null
        ? eventTimestamp
        : record.getSuspensionChangedAt();
    final var sameMark = sameMoment(record.getSuspendedAt(), suspendedAt);
    final var samePerson = Objects.equals(record.getSuspendedBy(), suspendedBy);
    final var sameChange = sameMoment(record.getSuspensionChangedAt(), suspensionChangedAt);
    if (sameMark && samePerson && sameChange) {
      return Outcome.REPEATED;
    }
    record.setSuspendedAt(suspendedAt);
    record.setSuspendedBy(suspendedBy);
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
