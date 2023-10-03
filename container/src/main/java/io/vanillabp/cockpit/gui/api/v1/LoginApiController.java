package io.vanillabp.cockpit.gui.api.v1;

import io.vanillabp.cockpit.commons.utils.UserContext;
import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
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
    private UserContext userContext;
    
    @Autowired
    private TaskScheduler taskScheduler;

    private Map<String, UpdateEmitter> updateEmitters = new HashMap<>();
    
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/updates",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<?>> updatesSubscription() throws Exception {
        
        final var id = UUID.randomUUID().toString();
        
        return userContext
                .getUserLoggedInAsMono()
                .flatMapMany(user -> {

                    final var channel = MessageChannels
                            .direct("SSE-" + user + "-" + id).get();
                    updateEmitters.put(id, UpdateEmitter
                            .withChannel(channel)
                            .updateInterval(properties.getGuiSseUpdateInterval()));

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
                        sink.onCancel(() -> channel.unsubscribe(handler));
                        channel.subscribe(handler);
                    }, FluxSink.OverflowStrategy.BUFFER);
                    
                });

    }
    
    @Scheduled(fixedDelayString = "PT1S")
    public void updateClients() {
        
        updateClients(null);
        
    }
    
    @EventListener(classes = GuiEvent.class)
    public void updateClients(
            final GuiEvent guiEvent) {
        
        final var toBeRemoved = new LinkedList<String>();
        updateEmitters
                .entrySet()
                .stream()
                .filter(emitter -> guiEvent == null || guiEvent.matchesTargetRoles(emitter.getValue().getRoles()))
                .filter(emitter -> emitter.getValue().sendEvent(guiEvent))
                .forEach(emitter -> {
                    final var updateEmitter = emitter.getValue();
                    updateEmitter
                            .consumeEvents()
                            .stream()
                            .collect(Collectors.groupingBy(GuiEvent::getSource))
                            .entrySet()
                            .stream()
                            .forEach(eventEntry -> {
                                try {
                                    sendSseEvent(
                                            updateEmitter.getChannel(),
                                            eventEntry.getKey().toString(),
                                            eventEntry.getValue());
                                } catch (Exception e) {
                                    logger.warn("Could not send update event", e);
                                    toBeRemoved.add(emitter.getKey());
                                }
                            });
                });
        toBeRemoved.forEach(updateEmitters::remove);
        
    }

    /**
     * SseEmitter timeouts are absolute. So we need to ping
     * the connection and if the user closed the browser we
     * will see an error which indicates we have to drop 
     * this emitter. 
     */
    @Scheduled(fixedDelayString = "PT29S")
    public void cleanupUpdateEmitters() {
        
        final var toBeDeleted = new LinkedList<String>();
        updateEmitters
                .entrySet()
                .forEach(entry -> {
                    if (!pingUpdateEmitter(entry.getValue().getChannel())) {
                        toBeDeleted.add(entry.getKey());
                    }
                });
        toBeDeleted.forEach(updateEmitters::remove);
        
    }
    
    private static final PingEvent pingEvent = new PingEvent();
    
    private boolean pingUpdateEmitter(
            final SubscribableChannel channel) {
        
        try {
            sendSseEvent(channel, "ping", pingEvent);
            return true;
        } catch (Exception e) {
            return false;
        }
        
    }
    
    private <T> void sendSseEvent(
            final SubscribableChannel channel,
            final String name,
            final T payload) throws Exception {
        
        channel.send(new GenericMessage<ServerSentEvent<?>>(
                ServerSentEvent
                       .builder(payload)
                       .id(UUID.randomUUID().toString())
                       .event(name)
                       .build()));
        
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
