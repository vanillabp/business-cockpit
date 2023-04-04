package io.vanillabp.cockpit.gui.api.v1;

import java.util.List;

import org.springframework.context.ApplicationEvent;

public class GuiEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;
    
    private Object event;
    
    private List<Role> targetRoles;

    public GuiEvent(
            final Object source,
            final List<Role> targetRoles,
            final Object event) {
        
        super(source);
        this.event = event;
        this.targetRoles = targetRoles;
        
    }
    
    public Object getEvent() {
        return event;
    }

    public List<Role> getTargetRoles() {
        return targetRoles;
    }
    
    public boolean matchesTargetRoles(
            final List<Role> roles) {
        
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
