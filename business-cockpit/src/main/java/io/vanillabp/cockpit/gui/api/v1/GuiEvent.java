package io.vanillabp.cockpit.gui.api.v1;

import java.util.Collection;
import org.springframework.context.ApplicationEvent;

public class GuiEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;
    
    private Object event;
    
    private Collection<String> targetGroups;

    public GuiEvent(
            final Object source,
            final Collection<String> targetGroups,
            final Object event) {
        
        super(source);
        this.event = event;
        this.targetGroups = targetGroups;
        
    }
    
    public Object getEvent() {
        return event;
    }

    public Collection<String> getTargetGroups() {
        return targetGroups;
    }
    
    public boolean matchesTargetGroups(
            final Collection<String> groups) {
        
        if (targetGroups == null) {
            return true;
        }
        
        return targetGroups
                .stream()
                .flatMap(targetGroup -> groups.stream().map(targetGroups::equals))
                .anyMatch(hasMatchingGroup -> hasMatchingGroup);
        
    }

}
