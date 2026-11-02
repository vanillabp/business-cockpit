package io.vanillabp.cockpit.bpms;

import java.time.OffsetDateTime;

/**
 * One of the two rules of {@link SuspensionOfARecord}: the suspension or the activation.
 *
 * @param <R> The kind of record the rule is laid onto
 */
@FunctionalInterface
public interface SuspensionRule<R extends SuspendableRecord> {

  /**
   * Lays the report onto the record.
   *
   * @param record The user task or the workflow the report is about
   * @param eventTimestamp When the process instance was suspended or activated
   * @param suspendedBy Who suspended or activated it, or {@code null} where nobody is named
   * @return What the report did to the record
   */
  SuspensionOfARecord.Outcome apply(
      R record,
      OffsetDateTime eventTimestamp,
      String suspendedBy);

}
