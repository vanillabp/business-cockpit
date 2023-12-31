package io.vanillabp.cockpit.gui.api.v1;

import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserContext;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "/gui/api/v1")
public class LoginApiController implements LoginApi {

    @Autowired
    private Logger logger;
    
    @Autowired
    private ApplicationProperties properties;
    
    @Autowired
    private ReactiveUserContext userContext;
    
    @Autowired
    private TaskScheduler taskScheduler;

    private final Map<String, UpdateEmitter> updateEmitters = new HashMap<>();
    
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/updates",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<?>> updatesSubscription() throws Exception {
        
        final var id = UUID.randomUUID().toString();
        
        return userContext
                .getUserLoggedInDetailsAsMono()
                .flatMapMany(user -> {

                    final var channel = MessageChannels
                            .direct("SSE-" + user.getId() + "-" + id)
                            .get();
                    synchronized (updateEmitters) {
                        logger.debug("Register update Channel '{}': {}", id, user.getAuthorities());
                        updateEmitters.put(id, UpdateEmitter
                                .withChannel(channel)
                                .roles(user.getAuthorities())
                                .maxItemsPerUpdate(properties.getGuiSse().getMaxItemsPerUpdate())
                                .updateInterval(properties.getGuiSse().getUpdateInterval()));
                    }

                    // This ping forces the browser to treat the text/event-stream request
                    // as closed and therefore the lock created in fetchApi.ts is released
                    // to avoid the UI would stuck in cases of errors.
                    taskScheduler.schedule(
                            () -> {
                                if (!pingUpdateEmitter(channel)) {
                                    logger.warn("Could not SSE send confirmation, client might stuck");
                                }
                            }, Instant.now().plusMillis(300));

                    return Flux.create(sink -> {
                        final MessageHandler handler = message -> sink.next(
                                ServerSentEvent.class.cast(message.getPayload()));
                        sink.onDispose(() -> channel.unsubscribe(handler));
                        channel.subscribe(handler);
                    }, FluxSink.OverflowStrategy.BUFFER);
                    
                });

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
                                final var hasSubscribers = sendSseEventIfSubscribersAvailable(
                                        updateEmitter.getChannel(),
                                        source.toString(),
                                        event);
                                if (!hasSubscribers) {
                                    toBeRemoved.add(key);
                                }
                            } catch (Exception e) {
                                logger.warn("Could not send update event", e);
                            }
                        }));
        if (!toBeRemoved.isEmpty()) {
            synchronized (updateEmitters) {
                toBeRemoved.forEach(updateEmitters::remove);
            }
        }

    }
    
    @EventListener(classes = GuiEvent.class)
    public void updateClients(
            final GuiEvent guiEvent) {

        final List<Map.Entry<String, UpdateEmitter>> activeSubscribers;
        synchronized (updateEmitters) {
            updateEmitters
                    .entrySet()
                    .stream()
                    .filter(emitter -> guiEvent.matchesTargetRoles(emitter.getValue().getRoles()))
                    .forEach(emitter -> emitter.getValue().collectEvent(guiEvent));
        }

    }

    /**
     * SSE channel is closed on idle, so we ping the client.
     */
    @Scheduled(fixedDelayString = "PT27S")
    public void cleanupUpdateEmitters() {

        final List<Map.Entry<String, UpdateEmitter>> activeSubscribers;
        synchronized (updateEmitters) {
            activeSubscribers = updateEmitters
                    .entrySet()
                    .stream()
                    .toList();
        }
        final var toBeDeleted = new LinkedList<String>();
        activeSubscribers
                .forEach(entry -> {
                        if (!pingUpdateEmitter(entry.getValue().getChannel())) {
                            toBeDeleted.add(entry.getKey());
                        }
                    });
        if (!toBeDeleted.isEmpty()) {
            synchronized (updateEmitters) {
                toBeDeleted.forEach(updateEmitters::remove);
            }
        }
        
    }
    
    private static final PingEvent pingEvent = new PingEvent();
    
    private boolean pingUpdateEmitter(
            final DirectChannel channel) {
        
        try {
            final var hasSubscribers = sendSseEventIfSubscribersAvailable(channel, "ping", pingEvent);
            return hasSubscribers;
        } catch (Exception e) {
            logger.warn(
                    "Could not ping SSE channel '{}'!",
                    channel.getFullChannelName(),
                    e);
            return true; // channel may still have subscribers
        }
        
    }
    
    private <T> boolean sendSseEventIfSubscribersAvailable(
            final DirectChannel channel,
            final String name,
            final T payload) throws Exception {

        if (channel.getSubscriberCount() == 0) {
            return false;
        }
        channel.send(new GenericMessage<ServerSentEvent<?>>(
                ServerSentEvent
                       .builder(payload)
                       .id(UUID.randomUUID().toString())
                       .event(name)
                       .build()));
        return true;
        
    }
    
    @Override
    public Mono<ResponseEntity<AppInformation>> appInformation(
            final ServerWebExchange exchange) {

        return Mono.just(ResponseEntity.ok(
                new AppInformation()
                        .titleLong(properties.getTitleLong())
                        .titleShort(properties.getTitleShort())
                        .version(properties.getApplicationVersion())));
        
    }
    
    @Override
    public Mono<ResponseEntity<User>> currentUser(
            final String xRefreshToken,
            final ServerWebExchange exchange) {
        
        return userContext
                .getUserLoggedInDetailsAsMono()
                .map(user -> new User()
                        .id(user.getId())
                        .lastName(user.getLastName())
                        .firstName(user.getFirstName())
                        .email(user.getEmail())
                        .sex(
                                user.isFemale() == null
                                        ? Sex.OTHER
                                        : user.isFemale()
                                        ? Sex.FEMALE
                                        : Sex.MALE)
                        .status(user.isActive()
                                        ? UserStatus.ACTIVE
                                        : UserStatus.INACTIVE)
                        .roles(user.getAuthorities()))
                .map(ResponseEntity::ok);
        
    }
    
}
