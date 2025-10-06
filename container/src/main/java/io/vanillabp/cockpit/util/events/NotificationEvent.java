package io.vanillabp.cockpit.util.events;

import java.io.Serial;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import org.springframework.context.ApplicationEvent;

public abstract class NotificationEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1L;
    
    public enum Type { INSERT, UPDATE, DELETE };
    
    private final Type type;
    private final Collection<String> targetGroups;
    private final Collection<String> targetUsers;

    public NotificationEvent(
            final Object source,
            final Clock clock,
            final Type type,
            final Collection<String> targetGroups,
	    final Collection<String> targetUsers) {
        
        super(source, clock);
        this.type = type;
        this.targetGroups = targetGroups;
	this.targetUsers = targetUsers;
        
    }

    public NotificationEvent(
            final Object source,
            final Type type,
            Collection<String> targetGroups,
	    Collection<String> targetUsers) {
        
        super(source);
        this.type = type;
        this.targetGroups = targetGroups;
	this.targetUsers = targetUsers;
        
    }

    public Type getType() {
        return type;
    }

    public Collection<String> getTargetGroups() {
        return targetGroups;
    }

    public Collection<String> getTargetUsers() {
        return targetUsers;
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
