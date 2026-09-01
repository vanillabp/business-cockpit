package io.vanillabp.cockpit.commons.mongo.updateinfo;

import java.time.OffsetDateTime;

public interface UpdateInformationAware {

    /**
     * Used as the username when updates are not triggered by
     * user interaction but by any other event like cron-jobs.
     */
    String SYSTEM_USER = "system";

    /**
     * Used as the initiator of updates caused by the business cockpit itself rather than by a
     * user interaction (e.g. a cockpit-side job). Told apart from {@link #SYSTEM_USER} on
     * purpose: the latter marks updates reported by the workflow system.
     */
    String COCKPIT_USER = "cockpit";

    void setUpdatedBy(String userId);
    
    void setUpdatedAt(OffsetDateTime timestamp);
    
}
