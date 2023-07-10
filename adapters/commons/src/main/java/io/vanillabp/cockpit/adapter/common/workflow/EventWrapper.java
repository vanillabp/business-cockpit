package io.vanillabp.cockpit.adapter.common.workflow;

import java.time.OffsetDateTime;

public interface EventWrapper {

    Object getEvent();
    
    void setId(String id);
    
    void setComment(String comment);

    void setTimestamp(OffsetDateTime timestamp);
    
}
