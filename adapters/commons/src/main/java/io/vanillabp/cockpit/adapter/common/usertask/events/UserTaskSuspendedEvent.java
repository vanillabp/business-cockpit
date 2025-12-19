package io.vanillabp.cockpit.adapter.common.usertask.events;


import io.vanillabp.spi.cockpit.details.DetailsEvent;

public class UserTaskSuspendedEvent extends UserTaskLifecycleEvent {

    @Override
    public DetailsEvent.Event getEventType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
