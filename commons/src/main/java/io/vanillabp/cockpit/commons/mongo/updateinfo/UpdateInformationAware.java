package io.vanillabp.cockpit.commons.mongo.updateinfo;

import java.time.OffsetDateTime;

public interface UpdateInformationAware {

    /**
     * The user name recorded where no user triggered the update, a cron job for example.
     */
    String SYSTEM_USER = "system";

    /**
     * The initiator recorded where the Business Cockpit itself caused the update, a job of its
     * own for example. It is kept apart from {@link #SYSTEM_USER} on purpose, which marks an
     * update the workflow system reported.
     */
    String COCKPIT_USER = "cockpit";

    void setUpdatedBy(String userId);
    
    void setUpdatedAt(OffsetDateTime timestamp);
    
}
