package io.vanillabp.cockpit.bpms.kafka;

import io.vanillabp.cockpit.bpms.OutcomeOfStoring;

/**
 * Thrown by a listener where the cockpit could not store the report a record carries for now.
 * <p>
 * The exception is there so that the record is not taken as consumed: {@link RepeatUntilStored}
 * hands the same record to the listener again, and logs each attempt which failed. The cause is
 * what the save threw, so that log line has it.
 */
public class ReportNotStoredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param outcome What came of storing the report: a save which failed for now
     */
    public ReportNotStoredException(
            final OutcomeOfStoring outcome) {

        super(outcome.reason(), outcome.failure());

    }

}
