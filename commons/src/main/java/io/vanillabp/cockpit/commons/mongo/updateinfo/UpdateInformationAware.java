package io.vanillabp.cockpit.commons.mongo.updateinfo;

import java.time.OffsetDateTime;

public interface UpdateInformationAware {

    /**
     * The user name recorded where no user triggered the update, a cron job for example.
     *
     * <p>The cockpit's SPI holds the same text as <code>Initiator.SYSTEM</code>, in the artifact
     * <code>io.vanillabp.businesscockpit:spi-for-java</code>, where a workflow module sets it on
     * a report. The two modules do not see each other, so there are two constants of one value,
     * and neither may be changed alone.
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
