package io.vanillabp.cockpit.gui.api.v1;

import java.io.Serial;
import java.util.Collection;
import java.util.Optional;

import org.springframework.context.ApplicationEvent;
import org.springframework.lang.NonNull;

public class GuiEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Object event;

    private final Collection<String> targetGroups;

    public GuiEvent(
            @NonNull final Object source,
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

    public boolean relevantForTargetGroups(@NonNull Collection<String> groups) {
        return Optional.ofNullable(targetGroups)
		.map(tg -> tg.stream().anyMatch(groups::contains))
		.orElse(true);
    }
}
