package io.vanillabp.cockpit.bpms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * What the REST API of the BPMS side answers to a report, in the words the sender acts on.
 * <p>
 * The sender gives up a report which was answered with a status from 400 to 499, apart from 408
 * and 429, because sending it again would be refused again. It gives up a report which was answered
 * with 501 as well, because the server does not handle such a report at all. It repeats a report
 * which was answered with 503 or any other status from 500 up. See decision 11 in the repository's
 * DECISIONS.md. So the cockpit answers like this:
 * <ul>
 * <li>{@code 400 Bad Request} for a report the cockpit refuses for what it says.</li>
 * <li>{@code 503 Service Unavailable} for a report the cockpit could not store for now. MongoDB was
 * gone for a moment, or another report about the same record was stored at the same time. Either way
 * the same report goes through when it comes again, and nothing of it is lost.</li>
 * <li>{@code 422 Unprocessable Content} for a report MongoDB refuses every time, like one larger than
 * a document may be. See {@link OutcomeOfStoring} for which failures those are.</li>
 * </ul>
 * 503 and not 500, because 503 is the status for "the server cannot take this right now". A 500
 * says that something went wrong which nobody expected, and it is what the server answers to a
 * defect. The answer carries no {@code Retry-After}. The cockpit does not know when MongoDB is
 * back, and without the header the sender waits as long as its own backoff says.
 * <p>
 * 422 and not 400, although the sender gives up both. A 400 of this API says that the report breaks
 * a rule of the API, and that the sender has to change it. Its body names the field which breaks the
 * rule, and the sender's log points at the fields, the path and the version of the API. A report
 * MongoDB cannot store breaks no rule of the API. It is a report the cockpit understood and still
 * cannot keep, which is what 422 is for. The body is one line of plain text, like the body of a 400,
 * so the log of the sender shows it.
 * <p>
 * Each failure is logged here, once and as an error, because a report which is not stored is
 * something an operator has to know about. The service which tried to store it does not log it.
 */
public final class AnswerToAReport {

    private static final Logger logger = LoggerFactory.getLogger(AnswerToAReport.class);

    private AnswerToAReport() {
    }

    /**
     * @return The answer to a report which can never go through
     */
    public static ResponseEntity<Void> refused() {

        return ResponseEntity.badRequest().build();

    }

    /**
     * @param outcome What the service answered: whether the cockpit is up to date about the record
     *        now, and if not, whether the same report can go through when it comes again. A report
     *        which is older than what is stored changes nothing, and the cockpit is up to date
     *        anyway.
     * @return {@code 200 OK}, {@code 503 Service Unavailable} if the report has to come again, or
     *         {@code 422 Unprocessable Content} with the reason if it never goes through
     */
    // the generated API declares no body for any answer, so the type of the answer says Void. Spring
    // writes a body which is a CharSequence as text, whatever the declared type, and so the reason
    // of a 422 reaches the sender
    @SuppressWarnings("unchecked")
    public static ResponseEntity<Void> afterStoring(
            final OutcomeOfStoring outcome) {

        if (outcome.isUpToDate()) {
            return ResponseEntity.ok().build();
        }
        if (outcome.failsEveryTime()) {
            final var reason = outcome.reason();
            logger.error(
                    "Returning HTTP 422 Unprocessable Content: {} The sender gives the report up.",
                    reason,
                    outcome.failure());
            return (ResponseEntity<Void>) (ResponseEntity<?>) ResponseEntity
                    .unprocessableContent()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(reason);
        }
        logger.error(
                "Returning HTTP 503 Service Unavailable: {}. The sender sends the report again.",
                outcome.reason(),
                outcome.failure());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();

    }

}
