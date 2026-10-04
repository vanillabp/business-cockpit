package io.vanillabp.cockpit.bpms;

import com.mongodb.MongoServerException;
import io.vanillabp.cockpit.config.startup.MapKeyDotReplacement;
import org.bson.BsonMaximumSizeExceededException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.data.mapping.MappingException;

/**
 * What came of storing a report: the cockpit is up to date about the record, or the save failed.
 * A failed save is one of two kinds, and the way the report came in answers each kind
 * differently.
 * <ul>
 * <li>It fails for now. MongoDB cannot be reached, or another report about the same record was
 * stored at the same time. The same report goes through when it comes again, so REST answers
 * {@code 503} and Kafka hands the record over again.</li>
 * <li>It fails every time. The document itself is refused, so the same report is refused again
 * however often it comes. REST answers {@code 422} with the reason, and Kafka passes the
 * record over, so that the records behind it are not held up for good.</li>
 * </ul>
 * Only three failures are of the second kind. Each of them depends on nothing but the document,
 * so the same report fails the same way however often it comes. Everything else counts as a failure
 * for now, because repeating a report which could have gone through costs a few more attempts, and
 * giving up such a report loses it.
 * <ul>
 * <li>{@link BsonMaximumSizeExceededException}. The driver throws it before it sends anything,
 * because the document is larger than the 16 MB MongoDB takes. Spring does not translate it, so it
 * arrives as it is.</li>
 * <li>A write which breaks the validation rules of the collection, which MongoDB answers with the
 * error code {@value #DOCUMENT_VALIDATION_FAILURE}. The cockpit sets no such rules itself. An
 * operator can add them, and then every report which breaks them is refused the same way. The driver throws a
 * {@code MongoWriteException} with that code, and Spring wraps it into a
 * {@code DataIntegrityViolationException}, so the causes are searched for it.</li>
 * <li>A {@link MappingException} of Spring Data, thrown while it turns the record into a document
 * and before anything is sent. The case known is a key of the business data with a dot in it, like
 * {@code order.id}: unless {@link MapKeyDotReplacement} is configured, Spring Data refuses such a
 * key, and the reason names the property and what it costs.</li>
 * </ul>
 */
public final class OutcomeOfStoring {

    /** The error code MongoDB answers a write with which breaks the validation rules of the collection. */
    static final int DOCUMENT_VALIDATION_FAILURE = 121;

    private static final OutcomeOfStoring UP_TO_DATE = new OutcomeOfStoring(null, null, false);

    private final String record;

    private final Exception failure;

    private final boolean failsEveryTime;

    private OutcomeOfStoring(
            final String record,
            final Exception failure,
            final boolean failsEveryTime) {

        this.record = record;
        this.failure = failure;
        this.failsEveryTime = failsEveryTime;

    }

    /**
     * @return The outcome of a report which was stored, or which changed nothing because the
     *         cockpit knows something younger already
     */
    public static OutcomeOfStoring upToDate() {

        return UP_TO_DATE;

    }

    /**
     * @param record The record the report is about, like "user task 'task-1'"
     * @param failure What the save threw
     * @return The outcome of a save which failed, sorted into one of the two kinds
     */
    public static OutcomeOfStoring saveFailed(
            final String record,
            final Exception failure) {

        return new OutcomeOfStoring(record, failure, failsEveryTime(failure));

    }

    private static boolean failsEveryTime(
            final Throwable failure) {

        for (var cause = failure; cause != null; cause = cause.getCause()) {
            if ((cause instanceof BsonMaximumSizeExceededException)
                    || (cause instanceof MappingException)) {
                return true;
            }
            if ((cause instanceof MongoServerException server)
                    && (server.getCode() == DOCUMENT_VALIDATION_FAILURE)) {
                return true;
            }
        }
        return false;

    }

    /** @return Whether the cockpit is up to date about the record now */
    public boolean isUpToDate() {

        return failure == null;

    }

    /** @return Whether the save failed in a way which no repetition of the report can change */
    public boolean failsEveryTime() {

        return failsEveryTime;

    }

    /** @return What the save threw, or {@code null} if nothing failed */
    public Exception failure() {

        return failure;

    }

    /**
     * @return One line which says what could not be stored and why, short enough for the body of
     *         an answer and for a log line. It names the record and the most specific cause, and
     *         never a value of the report.
     */
    public String reason() {

        if (failure == null) {
            return "Nothing failed";
        }
        final var cause = NestedExceptionUtils.getMostSpecificCause(failure);
        if (cause instanceof BsonMaximumSizeExceededException) {
            return "The %s cannot be stored: it is larger than the 16 MB MongoDB takes for one document."
                    .formatted(record);
        }
        if (cause instanceof MappingException) {
            final var reason = "The %s cannot be stored: it does not fit the form MongoDB stores it in. %s"
                    .formatted(record, cause.getMessage());
            // a key with a dot can be stored once the cockpit is told what to write instead, and
            // the sender's log is where somebody looks first, so the answer says how and what it costs
            return MapKeyDotReplacement
                    .hintFor(cause.getMessage())
                    .map(hint -> reason + " " + hint)
                    .orElse(reason);
        }
        if (failsEveryTime) {
            return "The %s cannot be stored: MongoDB refuses it, because it breaks the validation rules of the collection."
                    .formatted(record);
        }
        return "The %s could not be stored for now: %s".formatted(record, cause.getClass().getSimpleName());

    }

}
