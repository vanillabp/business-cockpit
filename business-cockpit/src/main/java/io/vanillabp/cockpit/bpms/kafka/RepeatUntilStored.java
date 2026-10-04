package io.vanillabp.cockpit.bpms.kafka;

import com.mongodb.MongoException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.listener.DefaultErrorHandler;
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
 * Only a failure of storing is repeated. Everything else is about the record itself: bytes which are
 * no protobuf message, an event type this cockpit does not know, a field the mapping cannot take.
 * Such a record fails the same way each time, and repeating it would stop the partition for good.
 * So it is passed over, and an error names it, so that somebody can look for it on the topic.
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

    private RepeatUntilStored() {
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
        // nothing is repeated unless it is named here. The classification looks at the causes as
        // well, so an exception wrapped by the listener container is found
        errorHandler.defaultFalse();
        errorHandler.addRetryableExceptions(
                ReportNotStoredException.class,
                DataAccessException.class,
                MongoException.class);
        return errorHandler;

    }

    private static void passOver(
            final ConsumerRecord<?, ?> record,
            final Exception exception) {

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

}
