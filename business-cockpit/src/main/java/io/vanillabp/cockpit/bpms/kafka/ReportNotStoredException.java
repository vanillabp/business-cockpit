package io.vanillabp.cockpit.bpms.kafka;

/**
 * Thrown by a listener where the cockpit could not store the report a record carries.
 * <p>
 * The service has logged why already. The exception is there so that the record is not taken as
 * consumed: {@link RepeatUntilStored} hands the same record to the listener again.
 */
public class ReportNotStoredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param kindOfRecord What the record is about, like "user task" or "workflow"
     * @param id The id of the user task or the workflow
     */
    public ReportNotStoredException(
            final String kindOfRecord,
            final String id) {

        super("Could not store the report about " + kindOfRecord + " '" + id + "', it will come again");

    }

}
