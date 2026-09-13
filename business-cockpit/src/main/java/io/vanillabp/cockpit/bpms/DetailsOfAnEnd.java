package io.vanillabp.cockpit.bpms;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the cockpit stores as business data when a workflow module reports that a user task or a
 * case has ended.
 * <p>
 * An end carries the same fields as a change, and the point of that is the list of finished work:
 * it shows what a case was finished with instead of what the last change happened to say. But a
 * BPMS does not always have an answer left. Camunda 7 reads an ended task from its history, and the
 * history keeps no variables; the Process-Engine-API answers out of a cache which may have been
 * cleared by the time the report is sent. The report is sent anyway, because a completion which
 * never arrives leaves a task the cockpit shows as open forever, and it then carries no business
 * data.
 * <p>
 * Storing that emptiness would erase what the cockpit already knew about the case, at the moment
 * somebody starts looking at the finished list. So an end which reports nothing keeps what is
 * stored, and an end which reports something replaces it.
 */
public final class DetailsOfAnEnd {

    private DetailsOfAnEnd() {
    }

    /**
     * Copies the stored business data before a mapping runs.
     * <p>
     * A generated mapper updates a map in place: it empties the one the stored object holds and
     * fills it again. Reading the stored data after that reads what the event brought, so it has
     * to be read before.
     *
     * @param stored What the cockpit holds about the task or the case
     * @return A copy, or <code>null</code> where nothing is stored
     */
    public static Map<String, Object> before(
            final Map<String, Object> stored) {

        return stored == null ? null : new LinkedHashMap<>(stored);

    }

    /**
     * @param reported The business data the end carried
     * @param stored What the cockpit holds about the task or the case
     * @return What to store
     */
    public static Map<String, Object> whatToStore(
            final Map<String, Object> reported,
            final Map<String, Object> stored) {

        return (reported == null) || reported.isEmpty() ? stored : reported;

    }

    /**
     * @param reported The words the end carried for the fulltext search
     * @param stored What the cockpit holds about the task or the case
     * @return What to store
     */
    public static String whatToStore(
            final String reported,
            final String stored) {

        return (reported == null) || reported.isBlank() ? stored : reported;

    }

}
