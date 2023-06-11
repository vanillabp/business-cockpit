package io.vanillabp.cockpit.adapter.common.usertask;

import java.time.OffsetDateTime;

public interface EventWrapper {

    Object getEvent();
    
    void setId(String id);
    
    void setComment(String comment);

    void setTimestamp(OffsetDateTime timestamp);
    
    void setUserTaskId(String userTaskId);
    
}
