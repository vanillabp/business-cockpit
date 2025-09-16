package io.vanillabp.cockpit.adapter.common.usertask.events;

import io.vanillabp.spi.cockpit.details.DetailsEvent;
import java.time.OffsetDateTime;

public interface UserTaskEvent {

    String getEventId();

    void setEventId(String id);
    DetailsEvent.Event getEventType();

    String getUserTaskId();

    void setUserTaskId(String id);

    String getWorkflowModuleId();

    void setWorkflowModuleId(String id);

    String getComment();

    void setComment(String comment);


    OffsetDateTime getTimestamp();

    void setTimestamp(OffsetDateTime timestamp);
}
