package io.vanillabp.cockpit.gui.api.v1;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * One subscribed browser: the open server-sent-event stream plus the events collected for it since
 * the last delivery. Events are buffered rather than sent one by one, so a burst of changes
 * produces one update instead of hundreds.
 */
public class UpdateEmitter {

    private final SseEmitter emitter;

    /** Stable monitor: the event list itself is replaced on every delivery. */
    private final Object eventsLock = new Object();

    private int updateInterval;

    private int maxItemsPerUpdate;

    private List<String> groups;

    private long lastCommit;

    private List<GuiEvent> events;

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

    public List<String> getGroups() {
        return groups;
    }

    public UpdateEmitter groups(
            final List<String> groups) {
        this.groups = groups;
        return this;
    }

    public UpdateEmitter updateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
        return this;
    }

    /**
     * Writes one named event to the browser.
     *
     * @return whether the client is still there; a client which closed the stream cannot be told
     *         anything any more and its emitter has to be dropped
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
