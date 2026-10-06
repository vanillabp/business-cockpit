package io.vanillabp.cockpit.gui.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The open update streams, one per browser tab, and the way a change gets to the ones it concerns.
 *
 * <p>A change goes through three steps. It is collected as it happens, for nobody in particular.
 * Once per filtering tick the changes collected since the last tick are matched against every
 * stream: a stream gets a change if one of its views lets the change through, or if its browser
 * already shows the entity, which is how the browser learns that it has to drop it. Once per
 * collecting tick the matched changes are written to the browsers.
 *
 * <p>Which stream may see what is answered by one {@link UpdateStreamAudience} per kind of entity,
 * from the same visibility the lists use. An event of a kind no audience decides about, and an
 * event without an entity id, reaches every stream. The ping is such an event: it says nothing
 * about a case.
 */
public class UpdateStreams {

    private static final Logger logger = LoggerFactory.getLogger(UpdateStreams.class);

    /**
     * The stream stays open until the sign-in it was opened with expires, see
     * {@link #subscribe(UserDetails, Optional)}. Where the sign-in names no end, the stream stays
     * open for as long as the browser keeps the tab open. Both are far longer than the default
     * asynchronous request timeout, and timing out would close the stream under a client which is
     * perfectly healthy.
     */
    private static final long NO_SSE_TIMEOUT = Long.MAX_VALUE;

    private static final PingEvent pingEvent = new PingEvent();

    private final GuiSseProperties properties;

    private final TaskScheduler taskScheduler;

    private final Map<String, UpdateStreamAudience> audiences;

    private final int maxKnownIdsPerStream;

    private final Map<String, UpdateEmitter> updateEmitters = new ConcurrentHashMap<>();

    private final Object collectedChangesLock = new Object();

    private Map<String, List<GuiEvent>> collectedChanges = new LinkedHashMap<>();

    public UpdateStreams(
            final ApplicationProperties properties,
            final TaskScheduler taskScheduler,
            final List<UpdateStreamAudience> audiences) {

        this.properties = properties.getGuiSse();
        this.taskScheduler = taskScheduler;
        this.audiences = audiences
                .stream()
                .collect(Collectors.toMap(
                        UpdateStreamAudience::kindOfEntity, Function.identity()));
        this.maxKnownIdsPerStream = validMaxKnownIdsPerStream(this.properties);

    }

    /**
     * A limit below one would make every answer overflow, and each overflow asks the browser to
     * load its lists again. The cockpit starts anyway, with the default, and says which key to fix.
     */
    private static int validMaxKnownIdsPerStream(
            final GuiSseProperties properties) {

        final var configured = properties.getMaxKnownIdsPerStream();
        if (configured > 0) {
            return configured;
        }
        logger.warn(
                "Property 'business-cockpit.gui-sse.max-known-ids-per-stream' is {}, but it has to be "
                        + "at least 1. Using the default of {} instead. Set the property to a positive "
                        + "number, or remove it to use the default.",
                configured,
                GuiSseProperties.DEFAULT_MAX_KNOWN_IDS_PER_STREAM);
        return GuiSseProperties.DEFAULT_MAX_KNOWN_IDS_PER_STREAM;

    }

    /**
     * Opens a stream for one browser tab.
     *
     * @param user The person signed in, with the rights resolved for this sign-in
     * @param signInExpiresAt When the sign-in expires. The stream ends then, and the browser opens a
     *        new one, which carries the rights of the new sign-in. That is how a change of rights,
     *        a substitute for example, reaches the stream without the person signing in again
     */
    public SseEmitter subscribe(
            final UserDetails user,
            final Optional<Instant> signInExpiresAt) {

        final var id = UUID.randomUUID().toString();

        final var sseEmitter = new SseEmitter(NO_SSE_TIMEOUT);
        final var updateEmitter = UpdateEmitter
                .withEmitter(sseEmitter)
                .user(user)
                .maxItemsPerUpdate(properties.getMaxItemsPerUpdate())
                .updateInterval(properties.getUpdateInterval());
        audiences.forEach((kind, audience) -> {
            updateEmitter.addView(kind, audience.widestViewOf(user));
            // a new stream knows nothing of what its browser shows. The page may have loaded a
            // list before the stream was opened, and after a reconnect the browser shows the
            // lists of the stream before. The answer to the reload tells the stream about both.
            updateEmitter.reload(kind);
        });

        logger.debug("Register update emitter '{}' of user '{}'", id, user.getId());
        updateEmitters.put(id, updateEmitter);

        // whichever way the stream ends, the emitter must not be written to any more
        sseEmitter.onCompletion(() -> updateEmitters.remove(id));
        sseEmitter.onTimeout(() -> updateEmitters.remove(id));
        sseEmitter.onError(error -> updateEmitters.remove(id));

        // this ping makes the browser treat the text/event-stream request as closed. The lock
        // fetchApi.ts created is then released, so the user interface does not hang after an
        // error.
        taskScheduler.schedule(
                () -> {
                    if (!pingUpdateEmitter(id, updateEmitter)) {
                        logger.warn("Could not SSE send confirmation, client might stuck");
                    }
                }, Instant.now().plusMillis(300));

        signInExpiresAt.ifPresent(expiresAt -> taskScheduler.schedule(
                () -> {
                    logger.debug("Sign-in of update emitter '{}' expired", id);
                    removeUpdateEmitter(id);
                },
                expiresAt));

        return sseEmitter;

    }

    /**
     * Tells the streams of this person that one of their lists was answered with these ids, from
     * this view. Every stream of the person learns it, because a request of a list does not say
     * which tab it comes from.
     */
    public void listAnswered(
            final String kindOfEntity,
            final UserDetails user,
            final Object view,
            final Collection<String> ids) {

        if (user == null) {
            return;
        }
        updateEmitters
                .values()
                .stream()
                .filter(stream -> stream.belongsTo(user.getId()))
                .forEach(stream -> stream.rememberAnswer(
                        kindOfEntity, view, ids, maxKnownIdsPerStream));

    }

    /** Collects a change for the next filtering tick, without deciding who it concerns yet. */
    @EventListener(classes = GuiEvent.class)
    public void collectChange(
            final GuiEvent guiEvent) {

        synchronized (collectedChangesLock) {
            collectedChanges
                    .computeIfAbsent(guiEvent.getKindOfEntity(), kind -> new LinkedList<>())
                    .add(guiEvent);
        }

    }

    /**
     * Matches the changes collected since the last tick against the open streams. Nothing is asked
     * of the database when nothing was collected.
     *
     * <p>The placeholder spells the key the way the configuration spells it, see decision 30 in
     * the repository's DECISIONS.md.
     */
    @Scheduled(fixedDelayString = "${business-cockpit.gui-sse.filtering-interval:250}")
    public void filterCollectedChanges() {

        final Map<String, List<GuiEvent>> changes;
        synchronized (collectedChangesLock) {
            changes = collectedChanges;
            collectedChanges = new LinkedHashMap<>();
        }

        final var streams = List.copyOf(updateEmitters.values());
        streams.forEach(this::queueReloads);
        if (streams.isEmpty()) {
            return;
        }

        changes.forEach((kind, events) -> handOverToWhomItConcerns(kind, events, streams));

    }

    private void queueReloads(
            final UpdateEmitter stream) {

        stream
                .takeKindsToReload()
                .stream()
                .map(audiences::get)
                .filter(Objects::nonNull)
                .map(UpdateStreamAudience::reloadEvent)
                .forEach(stream::collectEvent);

    }

    private void handOverToWhomItConcerns(
            final String kindOfEntity,
            final List<GuiEvent> events,
            final List<UpdateEmitter> streams) {

        final var audience = audiences.get(kindOfEntity);
        if (audience == null) {
            streams.forEach(stream -> events.forEach(stream::collectEvent));
            return;
        }

        final var changedIds = events
                .stream()
                .map(GuiEvent::getEntityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        final Map<UpdateEmitter, Set<String>> visible = changedIds.isEmpty()
                ? Map.of()
                : audience.whatEachStreamMaySee(streams, changedIds);

        streams.forEach(stream -> {
            final var concerned = new HashSet<String>(visible.getOrDefault(stream, Set.of()));
            // a change the person may not see any more, of an entity the browser still shows
            concerned.addAll(stream.whichOfTheseTheBrowserShows(kindOfEntity, changedIds));
            events
                    .stream()
                    .filter(event -> (event.getEntityId() == null)
                            || concerned.contains(event.getEntityId()))
                    .forEach(stream::collectEvent);
        });

    }

    /**
     * Looks at every open stream and writes out what has been matched to it. A stream whose last
     * flush is less than its update interval ago is skipped, see
     * {@link UpdateEmitter#consumeEvents()}.
     *
     * <p>The placeholder spells the property key the way the configuration spells it. Spring's
     * relaxed binding turns {@code business-cockpit.gui-sse.collecting-interval} into
     * {@link GuiSseProperties#getCollectingInterval()}, but it does not help a placeholder in an
     * annotation. That one is looked up by its exact name. A camel-case name found nothing, so the
     * default of 250 milliseconds was the only value which ever counted.
     */
    @Scheduled(fixedDelayString = "${business-cockpit.gui-sse.collecting-interval:250}")
    public void deliverMatchedChanges() {

        final var toBeRemoved = new LinkedList<String>();
        updateEmitters
                .forEach((key, updateEmitter) -> updateEmitter
                        .consumeEvents()
                        .stream()
                        .collect(Collectors.groupingBy(
                                GuiEvent::getKindOfEntity, LinkedHashMap::new, Collectors.toList()))
                        .forEach((kind, events) -> {
                            try {
                                if (!updateEmitter.send(kind, events)) {
                                    toBeRemoved.add(key);
                                }
                            } catch (Exception e) {
                                logger.warn("Could not send update event", e);
                            }
                        }));
        toBeRemoved.forEach(this::removeUpdateEmitter);

    }

    /**
     * An update stream never ends by itself, so the clients have to be told when the application
     * shuts down. This reacts to the context closing and not to the bean being destroyed. The web
     * server refuses to shut down while requests are still in flight, and an open event stream is
     * such a request.
     */
    @EventListener(classes = ContextClosedEvent.class)
    public void closeUpdateStreams() {

        updateEmitters
                .keySet()
                .stream()
                .toList()
                .forEach(this::removeUpdateEmitter);

    }

    /**
     * An idle server-sent-event channel is closed, so the client is pinged to keep it open.
     */
    @Scheduled(fixedDelayString = "PT27S")
    public void cleanupUpdateEmitters() {

        final var toBeDeleted = new LinkedList<String>();
        updateEmitters
                .forEach((key, updateEmitter) -> {
                        if (!pingUpdateEmitter(key, updateEmitter)) {
                            toBeDeleted.add(key);
                        }
                    });
        toBeDeleted.forEach(this::removeUpdateEmitter);

    }

    /** The open streams, for a test or a measurement which looks at them from outside. */
    public Collection<UpdateEmitter> openStreams() {

        return List.copyOf(updateEmitters.values());

    }

    private boolean pingUpdateEmitter(
            final String id,
            final UpdateEmitter updateEmitter) {

        try {
            return updateEmitter.send("ping", pingEvent);
        } catch (Exception e) {
            logger.warn(
                    "Could not ping SSE emitter '{}'!",
                    id,
                    e);
            return true; // the client may still be there
        }

    }

    private void removeUpdateEmitter(
            final String id) {

        final var removed = updateEmitters.remove(id);
        if (removed != null) {
            removed.complete();
        }

    }

}
