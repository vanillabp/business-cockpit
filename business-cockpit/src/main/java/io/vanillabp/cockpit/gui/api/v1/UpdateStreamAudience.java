package io.vanillabp.cockpit.gui.api.v1;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;

/**
 * Decides for one kind of entity which open update streams may learn about a change. It answers
 * with the visibility the lists of that kind use, so a stream gets a wake-up call only for what
 * its person could find in one of their lists.
 *
 * <p>It is asked once per filtering tick, for all streams at once. The cockpit's audiences read the
 * changed entities with one query and decide for each stream in memory, so the database load does
 * not grow with the number of open tabs. The memory of what a browser already shows is not decided
 * here, see {@link UpdateStreams}.
 */
public interface UpdateStreamAudience {

  /**
   * The kind of entity this audience decides about. It is the source of the {@link GuiEvent}s
   * it is asked about.
   *
   * @return The kind of entity
   */
  String kindOfEntity();

  /**
   * The view a stream starts with, before any list of its person has answered. The views the
   * lists of the person answer with are added to it.
   *
   * @param user The person the stream is opened for
   * @return The widest view this person has on entities of this kind
   */
  Object widestViewOf(
      UserDetails user);

  /**
   * Of the ids changed since the last tick, the ones each stream may see through one of its
   * views. A stream which may see none of them may be left out of the answer.
   *
   * @param streams The open streams
   * @param changedIds The ids of the entities which changed since the last tick
   * @return For each stream, the ids it may see
   */
  Map<UpdateEmitter, Set<String>> whatEachStreamMaySee(
      Collection<UpdateEmitter> streams,
      Set<String> changedIds);

  /**
   * The wake-up call which makes a list of this kind load everything it shows again.
   *
   * @return The event to send
   */
  GuiEvent reloadEvent();

}
