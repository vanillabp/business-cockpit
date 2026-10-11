package io.vanillabp.cockpit.bpms.kafka;

import io.vanillabp.cockpit.bpms.OutcomeOfStoring;

/**
 * Thrown by a listener where MongoDB refuses the report a record carries, and would refuse it every
 * time it comes.
 * <p>
 * {@link RepeatUntilStored} skips such a record at once, and the error it logs names the record.
 * Repeating it would hold up every record behind it on the same partition for good. The cause is
 * what the save threw, and it is a failure of storing like the ones which are repeated. So the
 * error handler lists this class as one it never repeats, and it checks this class before the
 * causes.
 */
public final class ReportCannotBeStoredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param outcome What came of storing the report: a save which fails every time
     */
    public ReportCannotBeStoredException(
            final OutcomeOfStoring outcome) {

        super(outcome.reason(), outcome.failure());

    }

}
