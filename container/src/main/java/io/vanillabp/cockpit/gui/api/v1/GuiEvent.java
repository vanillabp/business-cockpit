package io.vanillabp.cockpit.gui.api.v1;

import org.springframework.context.ApplicationEvent;

import java.util.Collection;

public class GuiEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;
    
    private Object event;
    
    private Collection<String> targetRoles;

    public GuiEvent(
            final Object source,
            final Collection<String> targetRoles,
            final Object event) {
        
        super(source);
        this.event = event;
        this.targetRoles = targetRoles;
        
    }
    
    public Object getEvent() {
        return event;
    }

    public Collection<String> getTargetRoles() {
        return targetRoles;
    }
    
    public boolean matchesTargetRoles(
            final Collection<String> roles) {
        
        if (targetRoles == null) {
            return true;
        }
        
        return targetRoles
                .stream()
                .flatMap(targetRole -> roles.stream().map(targetRole::equals))
                .anyMatch(hasMatchingRole -> hasMatchingRole);
        
    }

}
