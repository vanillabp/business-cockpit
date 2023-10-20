package io.vanillabp.cockpit.util.events;

import org.springframework.context.ApplicationEvent;

import java.time.Clock;
import java.util.Collection;
import java.util.List;

public abstract class NotificationEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;
    
    public static enum Type { INSERT, UPDATE, DELETE };
    
    private final Type type;
    
    private Collection<String> targetRoles;

    public NotificationEvent(
            final Object source,
            final Clock clock,
            final Type type,
            final Collection<String> targetRoles) {
        
        super(source, clock);
        this.type = type;
        this.targetRoles = targetRoles;
        
    }

    public NotificationEvent(
            final Object source,
            final Type type,
            Collection<String> targetRoles) {
        
        super(source);
        this.type = type;
        this.targetRoles = targetRoles;
        
    }

    public Type getType() {
        return type;
    }

    public Collection<String> getTargetRoles() {
        return targetRoles;
    }
    
    public boolean matchesTargetRoles(
            final List<String> roles) {
        
        if (targetRoles == null) {
            return true;
        }
        
        return targetRoles
                .stream()
                .flatMap(targetRole -> roles.stream().map(targetRole::equals))
                .anyMatch(hasMatchingRole -> hasMatchingRole);
        
    }
    
}
