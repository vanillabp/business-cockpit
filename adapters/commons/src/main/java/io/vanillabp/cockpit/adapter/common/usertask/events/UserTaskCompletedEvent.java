package io.vanillabp.cockpit.adapter.common.usertask.events;


import io.vanillabp.spi.cockpit.details.DetailsEvent;
import java.util.List;

public class UserTaskCompletedEvent extends UserTaskEventImpl {

    public UserTaskCompletedEvent(String workflowModuleId, List<String> i18nLanguages) {
        super(workflowModuleId, i18nLanguages);
    }

    @Override
    public DetailsEvent.Event getEventType() {
        return DetailsEvent.Event.COMPLETED;
    }

}