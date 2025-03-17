package io.vanillabp.cockpit.commons.mongo.updateinfo;

import java.time.OffsetDateTime;

public interface UpdateInformationAware {

    /**
     * Used as the username when updates are not triggered by
     * user interaction but by any other event like cron-jobs.
     */
    String SYSTEM_USER = "system";

    void setUpdatedBy(String userId);
    
    void setUpdatedAt(OffsetDateTime timestamp);
    
}