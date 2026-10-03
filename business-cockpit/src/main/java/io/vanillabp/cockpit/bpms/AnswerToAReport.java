package io.vanillabp.cockpit.bpms;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * What the REST API of the BPMS side answers to a report, in the words the sender acts on.
 * <p>
 * The sender gives up a report which was answered with a status from 400 to 499, apart from 408
 * and 429, because sending it again would be refused again. It repeats a report which was answered
 * with 503 or any other status from 500 up. See decision 11 in the repository's DECISIONS.md. So a report the cockpit refuses for what it says is answered
 * with {@code 400 Bad Request}, and a report the cockpit could not store is answered with
 * {@code 503 Service Unavailable}. A save fails because MongoDB is gone for a moment, or because
 * another report about the same record was stored at the same time. Either way the same report
 * goes through when it comes again, and nothing of it is lost.
 * <p>
 * 503 and not 500, because 503 is the status for "the server cannot take this right now". A 500
 * says that something went wrong which nobody expected, and it is what the server answers to a
 * defect. The answer carries no {@code Retry-After}. The cockpit does not know when MongoDB is
 * back, and without the header the sender waits as long as its own backoff says.
 */
public final class AnswerToAReport {

    private AnswerToAReport() {
    }

    /**
     * @return The answer to a report which can never go through
     */
    public static ResponseEntity<Void> refused() {

        return ResponseEntity.badRequest().build();

    }

    /**
     * @param upToDate What the service answered: whether the cockpit is up to date about the
     *        record now. It is {@code false} only where storing the report failed. A report which
     *        is older than what is stored changes nothing, and the cockpit is up to date anyway.
     * @return {@code 200 OK}, or {@code 503 Service Unavailable} if the report has to come again
     */
    public static ResponseEntity<Void> afterStoring(
            final boolean upToDate) {

        return upToDate
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();

    }

}
