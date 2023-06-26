package io.vanillabp.cockpit.gui.api.v1;

import java.util.LinkedList;
import java.util.List;

import org.springframework.messaging.SubscribableChannel;

public class UpdateEmitter {
    
    private int updateInterval = 1000;
    
    private SubscribableChannel channel;
    
    private List<String> roles;
    
    private long lastCommit;
    
    private List<GuiEvent> events;
    
    private List<GuiEvent> toBeSent = new LinkedList<>();
    
    private UpdateEmitter() { }
    
    public static UpdateEmitter withChannel(
            final SubscribableChannel channel) {
        
        final var result = new UpdateEmitter();
        result.channel = channel;
        result.events = new LinkedList<>();
        return result;
        
    }
    
    public SubscribableChannel getChannel() {
        return channel;
    }
    
    public List<String> getRoles() {
        return roles;
    }
    
    public UpdateEmitter updateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
        return this;
    }
    
    public boolean sendEvent(
            final GuiEvent event) {

        synchronized (this) { // use this as a first mutex
            
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
                synchronized (channel) { // use channel as a second mutex
                    if (events.isEmpty()) {
                        return false;
                    }
                    if (toBeSent.isEmpty()) {
                        toBeSent = events;
                    } else {
                        toBeSent.addAll(events);
                    }
                }
                events = new LinkedList<>();
                return true;
            }
            
            return false;
            
        }
        
    }

    public List<GuiEvent> consumeEvents() {
        
        synchronized (channel) { // use channel as a second mutex
            final var result = toBeSent;
            if (!toBeSent.isEmpty()) {
                toBeSent = new LinkedList<>();
            }
            return result;
        }
        
    }
    
};
