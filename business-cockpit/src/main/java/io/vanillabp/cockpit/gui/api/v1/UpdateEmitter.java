package io.vanillabp.cockpit.gui.api.v1;

import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class UpdateEmitter {
    
    private SseEmitter emitter;
    
    private List<Role> roles;
    
    private UpdateEmitter() { }
    
    public static UpdateEmitter withEmitter(final SseEmitter emitter) {
        final var result = new UpdateEmitter();
        result.emitter = emitter;
        return result;
    }
    
    public SseEmitter getEmitter() {
        return emitter;
    }
    
    public List<Role> getRoles() {
        return roles;
    }
    
};
