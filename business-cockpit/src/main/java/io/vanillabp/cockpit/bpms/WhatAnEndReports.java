package io.vanillabp.cockpit.bpms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * What the cockpit stores when a workflow module reports that a user task or a case has ended. An
 * end overwrites what it reports and leaves the rest of the record as it is, see decision 19 in
 * the repository's DECISIONS.md. That entry says what an end still carries when nobody can
 * describe the case any more, and what keeping it costs.
 * <p>
 * A field which was not reported usually arrives as <code>null</code>. A mapper answers that with
 * {@link NullValuePropertyMappingStrategy#IGNORE}, so no method here is needed for it. The methods
 * here are for the maps and the lists, which arrive empty instead. A report fills its collections
 * whether it has anything to put in them or not, and over Kafka it cannot do otherwise: protobuf
 * cannot tell a map or a repeated field which was left out from one which is empty. So an empty
 * collection means nothing was reported.
 */
public final class WhatAnEndReports {

    private WhatAnEndReports() {
    }

    /**
     * Copies what is stored before a mapping runs.
     * <p>
     * A generated mapper updates a map in place. It empties the map the stored object holds and
     * fills it again. Read that map after the mapping and you read what the event brought, so read
     * it before.
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
