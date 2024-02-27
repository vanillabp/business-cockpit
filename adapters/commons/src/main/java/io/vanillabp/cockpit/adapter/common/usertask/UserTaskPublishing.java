package io.vanillabp.cockpit.adapter.common.usertask;

import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEvent;

public interface UserTaskPublishing {

    void publish(UserTaskEvent events);

}
