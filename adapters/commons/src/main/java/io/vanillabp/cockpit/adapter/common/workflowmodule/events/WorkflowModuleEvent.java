package io.vanillabp.cockpit.adapter.common.workflowmodule.events;

import java.time.OffsetDateTime;

public interface WorkflowModuleEvent {

    String getEventId();

    void setEventId(String eventId);

    String getSource();

    void setSource(String source);

    OffsetDateTime getTimestamp();

    void setTimestamp(OffsetDateTime timestamp);

    String getId(); // workflow module id

    void setId(String id);

}
