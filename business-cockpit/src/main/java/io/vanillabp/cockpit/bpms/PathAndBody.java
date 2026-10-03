package io.vanillabp.cockpit.bpms;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The check that a report names the same user task or case in its path and in its body.
 * <p>
 * A report about a known record carries the id twice: once in the path, for example
 * {@code workflow/{workflowId}/updated}, and once in the body. The service looks the record up by
 * the id in the path. The mapper which builds a record the cockpit does not hold yet takes the id
 * from the body. If the two differ, the record is stored under the id of the body, and the next
 * report with the same path does not find it. So the cockpit refuses such a report with
 * {@code 400 Bad Request}, as a REST interface is expected to.
 */
public final class PathAndBody {

    private static final Logger logger = LoggerFactory.getLogger(PathAndBody.class);

    private PathAndBody() {
    }

    /**
     * @param kindOfRecord What the id is about, for the log: "user task" or "workflow"
     * @param idInPath The id the path of the report names
     * @param idInBody The id the body of the report names
     * @return Whether both name the same record. If not, the log says why the report is refused.
     */
    public static boolean nameTheSameRecord(
            final String kindOfRecord,
            final String idInPath,
            final String idInBody) {

        if (Objects.equals(idInPath, idInBody)) {
            return true;
        }

        logger.warn(
                "Refusing a report about {} '{}': the path names '{}' and the body names '{}'. "
                        + "Send the report with the same id in both places.",
                kindOfRecord,
                idInPath,
                idInPath,
                idInBody);
        return false;

    }

}
