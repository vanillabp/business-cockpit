package io.vanillabp.cockpit.gui.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Decides for one kind of entity which open update streams may learn about a change. It answers
 * with the visibility the lists of that kind use, so a stream gets a wake-up call only for what
 * its person could find in one of their lists.
 *
 * <p>It is asked once per filtering tick, for all streams at once. One way to answer is a query per
 * stream. Another one is a single query for all streams, with the decision made in memory. Both
 * fit behind this interface. The memory of what a browser already shows is not decided here, see
 * {@link UpdateStreams}.
 */
public interface UpdateStreamAudience {

    /**
     * The kind of entity this audience decides about. It is the source of the {@link GuiEvent}s
     * it is asked about.
     */
    String kindOfEntity();

    /**
     * The view a stream starts with, before any list of its person has answered. The views the
     * lists of the person answer with are added to it.
     */
    Object widestViewOf(UserDetails user);

    /**
     * Of the ids changed since the last tick, the ones each stream may see through one of its
     * views. A stream which may see none of them may be left out of the answer.
     */
    Map<UpdateEmitter, Set<String>> whatEachStreamMaySee(
            Collection<UpdateEmitter> streams,
            Set<String> changedIds);

    /**
     * The wake-up call which makes a list of this kind load everything it shows again.
     */
    GuiEvent reloadEvent();

}
