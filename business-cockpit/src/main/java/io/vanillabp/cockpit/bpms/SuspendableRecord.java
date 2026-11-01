package io.vanillabp.cockpit.bpms;

import java.time.OffsetDateTime;

/**
 * A user task or a workflow, as far as {@link SuspensionOfARecord} reads and writes it.
 * <p>
 * A BPMS suspends a process instance, and with it every user task of that instance. The cockpit
 * marks such a record with the moment of the suspension. It does not take the record out of any
 * list, because an entry which is suddenly gone looks like an error to the user who saw it before.
 */
public interface SuspendableRecord {

  /**
   * When the process instance of the record was suspended.
   *
   * @return The moment of the suspension, or {@code null} while the record is not suspended
   */
  OffsetDateTime getSuspendedAt();

  /**
   * Sets when the process instance of the record was suspended.
   *
   * @param suspendedAt The moment of the suspension, or {@code null} for a record which is not
   *        suspended
   */
  void setSuspendedAt(
      OffsetDateTime suspendedAt);

  /**
   * When the event happened which set or removed {@link #getSuspendedAt()} the last time, by the
   * clock of the reporting workflow system. A report about the suspension which is older than this
   * changes nothing.
   *
   * @return The moment, or {@code null} where no report ever changed the suspension
   */
  OffsetDateTime getSuspensionChangedAt();

  /**
   * Sets when the event happened which set or removed {@link #getSuspendedAt()} the last time.
   *
   * @param suspensionChangedAt The moment, by the clock of the reporting workflow system
   */
  void setSuspensionChangedAt(
      OffsetDateTime suspensionChangedAt);

  /**
   * When the record ended.
   *
   * @return The time of the end, or {@code null} while the record is open
   */
  OffsetDateTime getEndedAt();

  /**
   * When the event behind the latest change the cockpit stored about the record happened.
   *
   * @return The moment, or {@code null} where the cockpit does not know
   */
  OffsetDateTime getLatestEventAt();

}
