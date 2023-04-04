package io.vanillabp.cockpit.gui.api.v1;

import java.util.LinkedList;
import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class UpdateEmitter {
    
    private int updateInterval = 1000;
    
    private SseEmitter emitter;
    
    private List<Role> roles;
    
    private long lastCommit;
    
    private List<GuiEvent> events;
    
    private UpdateEmitter() { }
    
    public static UpdateEmitter withEmitter(
            final SseEmitter emitter) {
        
        final var result = new UpdateEmitter();
        result.emitter = emitter;
        result.events = new LinkedList<>();
        return result;
        
    }
    
    public SseEmitter getEmitter() {
        return emitter;
    }
    
    public List<Role> getRoles() {
        return roles;
    }
    
    public UpdateEmitter updateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
        return this;
    }
    
    public boolean sendEvent(
            final GuiEvent event) {

        if (event != null) {
            events.add(event);
        }
        if (events.isEmpty()) {
            return false;
        }
        
        final var now = System.currentTimeMillis();
        final var elapsed = now - lastCommit;
        if (elapsed > updateInterval) {
            lastCommit = now;
            return true;
        }
        
        return false;
        
    }

    public List<GuiEvent> consumeEvents() {
        
        final var result = events;
        events = new LinkedList<>();
        return result;
        
    }
    
};
