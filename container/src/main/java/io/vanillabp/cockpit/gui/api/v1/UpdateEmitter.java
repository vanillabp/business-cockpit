package io.vanillabp.cockpit.gui.api.v1;

import org.springframework.integration.channel.DirectChannel;

import java.util.LinkedList;
import java.util.List;

public class UpdateEmitter {

    private final DirectChannel channel;

    private int updateInterval;

    private int maxItemsPerUpdate;

    private List<String> roles;

    private long lastCommit;
    
    private List<GuiEvent> events;

    private UpdateEmitter(
            final DirectChannel channel) {

        this.channel = channel;

    }

    public static UpdateEmitter withChannel(
            final DirectChannel channel) {

        final var result = new UpdateEmitter(channel);
        result.events = new LinkedList<>();
        return result;

    }

    public UpdateEmitter maxItemsPerUpdate(
            final int maxItemsPerUpdate) {
        this.maxItemsPerUpdate = maxItemsPerUpdate;
        return this;
    }

    public DirectChannel getChannel() {
        return channel;
    }
    
    public List<String> getRoles() {
        return roles;
    }

    public UpdateEmitter roles(
            final List<String> roles) {
        this.roles = roles;
        return this;
    }

    public UpdateEmitter updateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
        return this;
    }

    public void collectEvent(
            final GuiEvent event) {

        if (event == null) {
            return;
        }

        synchronized (channel) {
            events.add(event);
        }

    }

    public List<GuiEvent> consumeEvents() {

        synchronized (channel) {
            if (events.isEmpty()) {
                return List.of();
            }

            final var now = System.currentTimeMillis();
            final var elapsed = now - lastCommit;
            if (elapsed > updateInterval) {
                lastCommit = now;
                final List<GuiEvent> result;
                if (events.size() > maxItemsPerUpdate) {
                    result = events.subList(0, maxItemsPerUpdate);
                    events = events.subList(maxItemsPerUpdate, events.size());
                } else {
                    result = events;
                    events = new LinkedList<>();
                }
                return result;
            }

            return List.of();
        }

    }

};
