package io.vanillabp.cockpit.bpms.kafka;

import com.mongodb.MongoException;
import io.vanillabp.cockpit.bpms.OutcomeOfStoring;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * What the listener containers of the cockpit do with a record whose listener threw.
 * <p>
 * A record the cockpit could not store comes again, with a growing pause between two attempts and
 * no last attempt. It holds up the records behind it on the same partition until it is stored.
 * That is on purpose. A workflow module sends every report about one task or one case with the id
 * as the key, so they all sit on one partition, in the order they were sent. Passing over one of
 * them, or parking it on a topic of its own, would let the reports behind it overtake it. A
 * creation which overtakes nothing is what keeps the start of a case right. See decision 36 in the
 * repository's DECISIONS.md.
 * <p>
 * Repeating a report does not make it count twice. Nothing of it was stored, so it is weighed again
 * against what is stored, like any report: a creation of a record the cockpit holds stores nothing,
 * a change older than what is stored is dropped, an end of a record which has ended changes nothing.
 * <p>
 * Only a failure of storing is repeated, and only one which can go away. Everything else is about the
 * record itself: bytes which are no protobuf message, an event type this cockpit does not know, a
 * field the mapping cannot take, or a report MongoDB refuses every time, like one larger than a
 * document may be (see {@link OutcomeOfStoring}). Such a record fails the
 * same way each time, and repeating it would stop the partition for good. So it is passed over, and
 * an error names it, so that somebody can look for it on the topic.
 * <p>
 * Each attempt which failed is logged once, as an error which names the record, the attempt and the
 * cause. The service which tried to store the report does not log it, and the listener container
 * logs no more than that the record is in retry.
 */
public final class RepeatUntilStored {

    private static final Logger logger = LoggerFactory.getLogger(RepeatUntilStored.class);

    /** The pause after the first failure. */
    static final long FIRST_PAUSE_MILLIS = 1_000;

    /**
     * The longest pause. It stays well below the five minutes Kafka allows between two polls by
     * default, because the container waits in the thread which polls.
     */
    static final long LONGEST_PAUSE_MILLIS = 60_000;

    /**
     * Which exceptions are repeated, and which are not although their cause is. The error handler
     * looks at the exception and then at its causes, one after the other, and the first class named
     * here decides. So a report MongoDB refuses every time is not repeated for the failure of storing
     * it carries as its cause. Every exception which reaches none of these classes is passed over.
     */
    private static final Map<Class<? extends Throwable>, Boolean> REPEATED = Map.of(
            ReportNotStoredException.class, true,
            DataAccessException.class, true,
            MongoException.class, true,
            ReportCannotBeStoredException.class, false);

    private RepeatUntilStored() {
    }

    /**
     * The same answer the error handler gives, so that an attempt is logged as one which comes again
     * only where it does.
     */
    static boolean isRepeated(
            final Throwable exception) {

        for (var cause = exception; cause != null; cause = cause.getCause()) {
            final var current = cause;
            final var decided = REPEATED
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getKey().isInstance(current))
                    .map(Map.Entry::getValue)
                    .findFirst();
            if (decided.isPresent()) {
                return decided.get();
            }
        }
        return false;

    }

    /**
     * @return A new error handler for the listener containers of the cockpit
     */
    public static DefaultErrorHandler errorHandler() {

        final var backOff = new ExponentialBackOff(FIRST_PAUSE_MILLIS, 2.0);
        backOff.setMaxInterval(LONGEST_PAUSE_MILLIS);
        backOff.setMaxElapsedTime(Long.MAX_VALUE);
        backOff.setMaxAttempts(Long.MAX_VALUE);

        final var errorHandler = new DefaultErrorHandler(RepeatUntilStored::passOver, backOff);
        // nothing is repeated unless it is named in the map
        errorHandler.setClassifications(REPEATED, false);
        errorHandler.setRetryListeners(new RetryListener() {

            @Override
            public void failedDelivery(
                    final ConsumerRecord<?, ?> record,
                    final Exception exception,
                    final int deliveryAttempt) {

                // the error handler tells the listener of a record it passes over as well, as the
                // first and last attempt. That record gets the error of passOver instead
                if (!isRepeated(exception)) {
                    return;
                }
                logger.error(
                        "Handing a Kafka record over again, attempt {} failed: topic '{}', partition {}, offset {}, key '{}'. "
                                + "The cause: {}",
                        deliveryAttempt,
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        record.key(),
                        NestedExceptionUtils.getMostSpecificCause(exception).toString(),
                        exception);

            }

        });
        return errorHandler;

    }

    /**
     * A listener calls this with what the service answered.
     *
     * @param outcome What came of storing the report of a record
     * @throws ReportNotStoredException if the same report can go through when it comes again
     * @throws ReportCannotBeStoredException if MongoDB refuses the report every time
     */
    static void storedOrThrow(
            final OutcomeOfStoring outcome) {

        if (outcome.isUpToDate()) {
            return;
        }
        if (outcome.failsEveryTime()) {
            throw new ReportCannotBeStoredException(outcome);
        }
        throw new ReportNotStoredException(outcome);

    }

    private static void passOver(
            final ConsumerRecord<?, ?> record,
            final Exception exception) {

        final var cannotBeStored = cannotBeStoredIn(exception);
        if (cannotBeStored != null) {
            logger.error(
                    "Passing over a Kafka record the cockpit cannot store: topic '{}', partition {}, offset {}, key '{}'. "
                            + "MongoDB refuses it every time. {}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    cannotBeStored.getMessage(),
                    cannotBeStored.getCause());
            return;
        }
        logger.error(
                "Passing over a Kafka record the cockpit cannot read: topic '{}', partition {}, offset {}, key '{}'. "
                        + "It would fail the same way each time. The cause: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                NestedExceptionUtils.getMostSpecificCause(exception).toString(),
                exception);

    }

    private static ReportCannotBeStoredException cannotBeStoredIn(
            final Throwable exception) {

        for (var cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ReportCannotBeStoredException cannotBeStored) {
                return cannotBeStored;
            }
        }
        return null;

    }

}
