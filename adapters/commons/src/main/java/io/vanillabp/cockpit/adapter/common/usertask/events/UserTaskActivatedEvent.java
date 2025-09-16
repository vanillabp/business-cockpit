package io.vanillabp.cockpit.adapter.common.usertask.events;

import io.vanillabp.spi.cockpit.details.DetailsEvent;

public class UserTaskActivatedEvent extends UserTaskLifecycleEvent {

    @Override
    public DetailsEvent.Event getEventType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
