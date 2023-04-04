package io.vanillabp.cockpit.commons.mongo.updateinfo;

import java.time.OffsetDateTime;

public interface UpdateInformationAware {

    void setUpdatedBy(String userId);
    
    void setUpdatedAt(OffsetDateTime timestamp);
    
}