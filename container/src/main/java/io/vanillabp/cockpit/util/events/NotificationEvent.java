package io.vanillabp.cockpit.util.events;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import org.springframework.context.ApplicationEvent;

public abstract class NotificationEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;
    
    public static enum Type { INSERT, UPDATE, DELETE };
    
    private final Type type;
    
    private Collection<String> targetGroups;

    public NotificationEvent(
            final Object source,
            final Clock clock,
            final Type type,
            final Collection<String> targetGroups) {
        
        super(source, clock);
        this.type = type;
        this.targetGroups = targetGroups;
        
    }

    public NotificationEvent(
            final Object source,
            final Type type,
            Collection<String> targetGroups) {
        
        super(source);
        this.type = type;
        this.targetGroups = targetGroups;
        
    }

    public Type getType() {
        return type;
    }

    public Collection<String> getTargetGroups() {
        return targetGroups;
    }
    
    public boolean matchesTargetGroups(
            final List<String> groups) {
        
        if (targetGroups == null) {
            return true;
        }
        
        return targetGroups
                .stream()
                .flatMap(targetGroup -> groups.stream().map(targetGroup::equals))
                .anyMatch(hasMatchingGroup -> hasMatchingGroup);
        
    }
    
}
