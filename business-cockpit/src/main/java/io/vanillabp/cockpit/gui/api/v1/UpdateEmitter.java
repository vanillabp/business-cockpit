package io.vanillabp.cockpit.gui.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * One subscribed browser tab: the open server-sent-event stream, the person it was opened for, and
 * the events waiting for it since the last delivery. Events are buffered rather than sent one by
 * one, so a burst of changes produces one update instead of hundreds.
 *
 * <p>The stream also remembers what its browser shows. These are the ids the lists of its person
 * were answered with, and the views they were answered from. The ids are how a stream learns that
 * a task was taken away from its person: the change is no longer visible to them, but the browser
 * still shows it, so it needs the wake-up call to drop it. The views are what the stream asks with
 * when it decides which changes concern its person.
 */
public class UpdateEmitter {

    private final SseEmitter emitter;

    /** Stable monitor: the event list itself is replaced on every delivery. */
    private final Object eventsLock = new Object();

    private final Object memoryLock = new Object();

    private int updateInterval;

    private int maxItemsPerUpdate;

    private UserDetails user;

    private long lastCommit;

    private List<GuiEvent> events;

    private final Map<String, Set<String>> idsTheBrowserShows = new HashMap<>();

    private final Map<String, Set<Object>> viewsOfTheBrowser = new HashMap<>();

    private final Set<String> kindsToReload = new LinkedHashSet<>();

    private UpdateEmitter(
            final SseEmitter emitter) {

        this.emitter = emitter;

    }

    public static UpdateEmitter withEmitter(
            final SseEmitter emitter) {

        final var result = new UpdateEmitter(emitter);
        result.events = new LinkedList<>();
        return result;

    }

    public UpdateEmitter maxItemsPerUpdate(
            final int maxItemsPerUpdate) {
        this.maxItemsPerUpdate = maxItemsPerUpdate;
        return this;
    }

    public SseEmitter getEmitter() {
        return emitter;
    }

    /**
     * The person the stream was opened for, with the rights they had at that moment. The stream
     * ends when their sign-in expires, so newer rights arrive with the next stream.
     */
    public UserDetails getUser() {
        return user;
    }

    public UpdateEmitter user(
            final UserDetails user) {
        this.user = user;
        return this;
    }

    public boolean belongsTo(
            final String userId) {

        return (user != null) && Objects.equals(user.getId(), userId);

    }

    public UpdateEmitter updateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
        return this;
    }

    /**
     * Remembers that a list of this stream's person was answered with these ids, out of this view.
     *
     * <p>The ids of all kinds count against {@code maxKnownIds}. Above it the stream keeps only the
     * ids of this answer and forgets the rest. Every kind which lost an id is reloaded, because
     * the browser may still show what the stream forgot. Keeping this answer is what stops a loop:
     * the answer to the reload is about the same ids, so it does not overflow again unless one
     * answer alone is above the limit, and even then it loses nothing.
     */
    public void rememberAnswer(
            final String kindOfEntity,
            final Object view,
            final Collection<String> ids,
            final int maxKnownIds) {

        synchronized (memoryLock) {
            viewsOfTheBrowser
                    .computeIfAbsent(kindOfEntity, kind -> new LinkedHashSet<>())
                    .add(view);
            idsTheBrowserShows
                    .computeIfAbsent(kindOfEntity, kind -> new HashSet<>())
                    .addAll(ids);
            if (numberOfIdsTheBrowserShows() <= maxKnownIds) {
                return;
            }
            final var answer = Set.copyOf(ids);
            idsTheBrowserShows.forEach((kind, known) -> {
                final var before = known.size();
                if (kind.equals(kindOfEntity)) {
                    known.retainAll(answer);
                } else {
                    known.clear();
                }
                if (known.size() < before) {
                    kindsToReload.add(kind);
                }
            });
        }

    }

    private int numberOfIdsTheBrowserShows() {

        return idsTheBrowserShows
                .values()
                .stream()
                .mapToInt(Set::size)
                .sum();

    }

    /** Of these ids, the ones the browser of this stream was told about by one of its lists. */
    public Set<String> whichOfTheseTheBrowserShows(
            final String kindOfEntity,
            final Collection<String> ids) {

        synchronized (memoryLock) {
            final var known = idsTheBrowserShows.get(kindOfEntity);
            if ((known == null) || known.isEmpty()) {
                return Set.of();
            }
            final var result = new HashSet<String>();
            ids.stream().filter(known::contains).forEach(result::add);
            return result;
        }

    }

    /**
     * The views the lists of this stream's person were answered from, and the view the stream
     * started with.
     */
    public Set<Object> viewsOf(
            final String kindOfEntity) {

        synchronized (memoryLock) {
            return Set.copyOf(
                    viewsOfTheBrowser.getOrDefault(kindOfEntity, Set.of()));
        }

    }

    public void addView(
            final String kindOfEntity,
            final Object view) {

        synchronized (memoryLock) {
            viewsOfTheBrowser
                    .computeIfAbsent(kindOfEntity, kind -> new LinkedHashSet<>())
                    .add(view);
        }

    }

    /** Asks the lists of this kind in the browser to load everything they show again. */
    public void reload(
            final String kindOfEntity) {

        synchronized (memoryLock) {
            kindsToReload.add(kindOfEntity);
        }

    }

    public Set<String> takeKindsToReload() {

        synchronized (memoryLock) {
            if (kindsToReload.isEmpty()) {
                return Set.of();
            }
            final var result = Set.copyOf(kindsToReload);
            kindsToReload.clear();
            return result;
        }

    }

    /**
     * Writes one named event to the browser.
     *
     * @return Whether the client is still there. A client which closed the stream cannot be told
     *         anything any more, and its emitter has to be dropped
     */
    public boolean send(
            final String name,
            final Object payload) {

        // SseEmitter is not safe for concurrent writes, and both the collecting tick and the ping
        // tick may want to write at the same moment
        synchronized (this) {
            try {
                emitter.send(SseEmitter
                        .event()
                        .id(UUID.randomUUID().toString())
                        .name(name)
                        .data(payload));
                return true;
            } catch (IOException | IllegalStateException e) {
                return false;
            }
        }

    }

    public void complete() {

        synchronized (this) {
            try {
                emitter.complete();
            } catch (Exception e) {
                // the stream is gone either way
            }
        }

    }

    public void collectEvent(
            final GuiEvent event) {

        if (event == null) {
            return;
        }

        synchronized (eventsLock) {
            events.add(event);
        }

    }

    public List<GuiEvent> consumeEvents() {

        synchronized (eventsLock) {
            if (events.isEmpty()) {
                return List.of();
            }

            final var now = System.currentTimeMillis();
            final var elapsed = now - lastCommit;
            if (elapsed > updateInterval) {
                lastCommit = now;
                final List<GuiEvent> result;
                if (events.size() > maxItemsPerUpdate) {
                    result = List.copyOf(events.subList(0, maxItemsPerUpdate));
                    events = new LinkedList<>(events.subList(maxItemsPerUpdate, events.size()));
                } else {
                    result = events;
                    events = new LinkedList<>();
                }
                return result;
            }

            return List.of();
        }

    }

}
