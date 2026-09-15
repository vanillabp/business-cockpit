package io.vanillabp.cockpit.gui.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping(path = "/gui/api/v1")
public class LoginApiController implements LoginApi {

    /**
     * The stream stays open for as long as the browser keeps the tab open, which is far longer
     * than the default asynchronous request timeout. Timing it out would close the stream under a
     * client which is perfectly healthy.
     */
    private static final long NO_SSE_TIMEOUT = Long.MAX_VALUE;

    @Autowired
    private Logger logger;

    @Autowired
    private ApplicationProperties properties;

    @Autowired
    private UserContext userContext;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private PersonAndGroupApiMapper personAndGroupMapper;

    private final Map<String, UpdateEmitter> updateEmitters = new ConcurrentHashMap<>();

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/updates",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter updatesSubscription() throws Exception {

        final var id = UUID.randomUUID().toString();
        final var user = userContext.getUserLoggedInDetails();

        final var sseEmitter = new SseEmitter(NO_SSE_TIMEOUT);
        final var updateEmitter = UpdateEmitter
                .withEmitter(sseEmitter)
                .groups(user.getAuthorities())
                .maxItemsPerUpdate(properties.getGuiSse().getMaxItemsPerUpdate())
                .updateInterval(properties.getGuiSse().getUpdateInterval());

        logger.debug("Register update emitter '{}': {}", id, user.getAuthorities());
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

        return sseEmitter;

    }

    @Scheduled(fixedDelayString = "${businessCockpit.guiSse.collectingInterval:250}") // every 0.5 seconds
    public void updateClients() {

        final var toBeRemoved = new LinkedList<String>();
        updateEmitters
                .forEach((key, updateEmitter) -> updateEmitter
                        .consumeEvents()
                        .stream()
                        .collect(Collectors.groupingBy(GuiEvent::getSource))
                        .forEach((source, event) -> {
                            try {
                                if (!updateEmitter.send(source.toString(), event)) {
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

    @EventListener(classes = GuiEvent.class)
    public void updateClients(
            final GuiEvent guiEvent) {

        updateEmitters
                .values()
                .stream()
                // TODO: get affected users/groups (old doc, new doc) and also take mappings (user substitutes) into account
                // .filter(emitter -> guiEvent.matchesTargetGroups(emitter.getGroups()))
                .forEach(emitter -> emitter.collectEvent(guiEvent));

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

    private static final PingEvent pingEvent = new PingEvent();

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

    @Override
    public ResponseEntity<AppInformation> appInformation() {

        return ResponseEntity.ok(
                new AppInformation()
                        .titleLong(properties.getTitleLong())
                        .titleShort(properties.getTitleShort())
                        .version(properties.getApplicationVersion())
                        .buildTimestamp(properties.getBuildTimestamp())
			.additionalProperties(properties.getAdditionalProperties()));
    }

    @Override
    public ResponseEntity<User> currentUser(
            final String xRefreshToken) {

        final var user = userContext.getUserLoggedInDetails();
        // the request which signs in by basic authentication is authenticated, but the response
        // to exactly that request is what carries the JWT cookie. The cockpit reads its user
        // details from that cookie, so the sign-in request has no user to report yet and the
        // client asks again with the cookie. The answer has no body, the way it had while the
        // application was reactive and the empty publisher ended up as an empty 200.
        if (user == null) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.ok(Optional
                .ofNullable(personAndGroupMapper.toApiPerson(user.getId()))
                .map(person -> new User()
                        .id(person.getId())
                        .email(person.getEmail())
                        .avatar(person.getAvatar())
                        .display(person.getDisplay())
                        .displayShort(person.getDisplayShort())
                        .details(person.getDetails()))
                .orElse(new User()
                        .id(user.getId())
                        .display(user.getDisplay())
                        .displayShort(user.getDisplayShort())
                        .email(user.getEmail()))
                .groups(user
                        .getAuthorities()
                        .stream()
                        .map(personAndGroupMapper::authorityToApiGroup)
                        .toList()));

    }

}
