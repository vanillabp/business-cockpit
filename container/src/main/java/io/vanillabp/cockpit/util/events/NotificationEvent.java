package io.vanillabp.cockpit.util.events;

import java.time.Clock;
import java.util.List;

import org.springframework.context.ApplicationEvent;

public abstract class NotificationEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;
    
    public static enum Type { INSERT, UPDATE, DELETE };
    
    private final Type type;
    
    private List<String> targetRoles;

    public NotificationEvent(
            final Object source,
            final Clock clock,
            final Type type,
            final List<String> targetRoles) {
        
        super(source, clock);
        this.type = type;
        this.targetRoles = targetRoles;
        
    }

    public NotificationEvent(
            final Object source,
            final Type type,
            List<String> targetRoles) {
        
        super(source);
        this.type = type;
        this.targetRoles = targetRoles;
        
    }

    public Type getType() {
        return type;
    }

    public List<String> getTargetRoles() {
        return targetRoles;
    }
    
    public boolean matchesTargetRoles(
            final List<String> roles) {
        
        if (targetRoles == null) {
            return true;
        }
        
        return targetRoles
                .stream()
                .flatMap(targetRole -> roles.stream().map(role -> targetRole == role))
                .filter(hasMatchingRole -> hasMatchingRole)
                .findFirst()
                .isPresent();
        
    }
    
}
