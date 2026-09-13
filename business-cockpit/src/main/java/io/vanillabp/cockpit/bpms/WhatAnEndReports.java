package io.vanillabp.cockpit.bpms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * What the cockpit stores when a workflow module reports that a user task or a case has ended.
 * <p>
 * An end carries the same fields as a change, and the point of that is the list of finished work:
 * it shows what a case was finished with instead of what the last change happened to say. But a
 * BPMS does not always have an answer left. Camunda 7 reads an ended task from its history, and the
 * history keeps no variables; the Process-Engine-API answers out of a cache which may have been
 * cleared by the time the report is sent. The report is sent anyway, because a completion which
 * never arrives leaves a task the cockpit shows as open forever, and it then carries the
 * identifiers, the timestamp and the address of the user interface and nothing else.
 * <p>
 * Storing that emptiness would erase what the cockpit already knew about the case, at the moment
 * somebody starts looking at the finished list. So an end which reports nothing for a field keeps
 * what is stored, and an end which reports something replaces it. That is the rule for every field
 * of an end, and a mapper says it with {@link NullValuePropertyMappingStrategy#IGNORE} wherever a
 * field which was not reported arrives as <code>null</code>.
 * <p>
 * The methods here are for the fields where it arrives empty instead. A report carries its maps and
 * its lists as empty collections rather than leaving them out, and over Kafka it could not do
 * otherwise, because protobuf gives a map and a repeated field no presence information at all. An
 * empty collection is therefore read as nothing reported. The price is a report which cannot say "no
 * title" or "nobody may see this" once something is stored; saying it the other way round, by leaving
 * a collection out, would be the far more expensive mistake.
 */
public final class WhatAnEndReports {

    private WhatAnEndReports() {
    }

    /**
     * Copies what is stored before a mapping runs.
     * <p>
     * A generated mapper updates a map in place: it empties the one the stored object holds and
     * fills it again. Reading the stored map after that reads what the event brought, so it has to
     * be read before.
     *
     * @param stored What the cockpit holds about the task or the case
     * @param <V> What the map holds
     * @return A copy, or <code>null</code> where nothing is stored
     */
    public static <V> Map<String, V> before(
            final Map<String, V> stored) {

        return stored == null ? null : new LinkedHashMap<>(stored);

    }

    /**
     * @param stored What the cockpit holds about the task or the case
     * @param <V> What the list holds
     * @return A copy, or <code>null</code> where nothing is stored
     * @see #before(Map)
     */
    public static <V> List<V> before(
            final List<V> stored) {

        return stored == null ? null : new ArrayList<>(stored);

    }

    /**
     * @param reported What the end carried
     * @param stored What the cockpit holds about the task or the case
     * @param <V> What the map holds
     * @return What to store
     */
    public static <V> Map<String, V> whatToStore(
            final Map<String, V> reported,
            final Map<String, V> stored) {

        return (reported == null) || reported.isEmpty() ? stored : reported;

    }

    /**
     * @param reported What the end carried
     * @param stored What the cockpit holds about the task or the case
     * @param <V> What the list holds
     * @return What to store
     */
    public static <V> List<V> whatToStore(
            final List<V> reported,
            final List<V> stored) {

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
