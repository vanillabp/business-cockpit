package io.vanillabp.cockpit.adapter.common.workflow.events;

import java.time.OffsetDateTime;

public interface WorkflowEvent {

    String getEventId();

    void setEventId(String eventId);


    String getComment();

    void setComment(String comment);


    OffsetDateTime getTimestamp();

    void setTimestamp(OffsetDateTime timestamp);

}
