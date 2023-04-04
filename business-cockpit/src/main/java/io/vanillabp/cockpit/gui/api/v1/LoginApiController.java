package io.vanillabp.cockpit.gui.api.v1;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.vanillabp.cockpit.commons.utils.UserContext;

@RestController
@RequestMapping(path = "/gui/api/v1")
public class LoginApiController implements LoginApi {

    @Autowired
    private Logger logger;
    
    @Autowired
    private UserContext userContext;
    
    @Autowired
    private TaskScheduler taskScheduler;

    private Map<String, UpdateEmitter> updateEmitters = new HashMap<>();
    
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/updates"
        )
    public SseEmitter updatesSubscription() throws Exception {
        
        final var id = UUID.randomUUID().toString();
        
        final var user = userContext.getUserLoggedIn();
        
        final var updateEmitter = new SseEmitter(-1l);
        updateEmitters.put(id, UpdateEmitter
                .withEmitter(updateEmitter));

        // This ping forces the browser to treat the text/event-stream request
        // as closed and therefore the lock created in fetchApi.ts is released
        // to avoid the UI would stuck in cases of errors.
        taskScheduler.schedule(
                () -> {
                    try {
                        final var ping = new PingEvent();
                        updateEmitter
                                .send(
                                        SseEmitter
                                                .event()
                                                .id(UUID.randomUUID().toString())
                                                .data(ping, MediaType.APPLICATION_JSON)
                                                .name("ping"));
                    } catch (Exception e) {
                        logger.warn("Could not SSE send confirmation, client might stuck");
                    }
                }, Instant.now().plusMillis(300));
        
        return updateEmitter;

    }
    
    @EventListener(classes = GuiEvent.class)
    public void updateClients(
            final GuiEvent guiEvent) {
        
        updateEmitters
                .values()
                .stream()
                .filter(emitter -> guiEvent.matchesTargetRoles(emitter.getRoles()))
                .forEach(emitter -> {
                    try {
                        emitter
                                .getEmitter()
                                .send(
                                        SseEmitter
                                                .event()
                                                .id(UUID.randomUUID().toString())
                                                .data(guiEvent.getEvent(), MediaType.APPLICATION_JSON)
                                                .name(guiEvent.getSource().toString())
                                                .reconnectTime(30000));
                    } catch (Exception e) {
                        logger.warn("Could not send update event", e);
                    }
                });
        
    }

    /**
     * SseEmitter timeouts are absolute. So we need to ping
     * the connection and if the user closed the browser we
     * will see an error which indicates we have to drop 
     * this emitter. 
     */
    @Scheduled(fixedDelayString = "PT1M")
    public void cleanupUpdateEmitters() {
        
        final var ping = new PingEvent();
        
        final var toBeDeleted = new LinkedList<String>();
        updateEmitters
                .entrySet()
                .forEach(entry -> {
                    try {
                        final var emitter = entry.getValue();
                        emitter
                                .getEmitter()
                                .send(
                                        SseEmitter
                                                .event()
                                                .id(UUID.randomUUID().toString())
                                                .data(ping, MediaType.APPLICATION_JSON)
                                                .name("ping"));
                    } catch (Exception e) {
                        toBeDeleted.add(entry.getKey());
                    }
                });
        toBeDeleted.forEach(updateEmitters::remove);
        
    }
    
    @Override
    public ResponseEntity<AppInformation> appInformation() {
        
        return ResponseEntity.ok(
                new AppInformation()
                        .titleLong("DKE BPMS Cockpit")
                        .titleShort("DKE")
                        .version("0.0.1-SNAPSHOT"));
        
    }
    
    @Override
    public ResponseEntity<User> currentUser(
            final String xRefreshToken) {
        
        final var user = userContext.getUserLoggedIn();
        
        return ResponseEntity.ok(
                new User()
                        .id(user)
                        .sex(Sex.OTHER)
                        .roles(List.of()));

        
    }
    
}
