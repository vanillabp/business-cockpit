package io.vanillabp.cockpit.commons.mongo.updateinfo;

import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Order(1)
@Component
@ConditionalOnProperty(prefix = "spring.data.mongodb", name = "uri")
public class UpdateInformationEventListener implements BeforeConvertCallback<Object> {

    @Autowired
    private UserContext currentUser;

    @Override
    public Object onBeforeConvert(Object entityObj, String collection) {
        
        final var now = OffsetDateTime.now();

        if (entityObj instanceof UpdateInformationAware) {
            
            final var entity = (UpdateInformationAware) entityObj;
            entity.setUpdatedAt(now);
            entity.setUpdatedBy(currentUser.getUserLoggedIn());
            
        }
        
        return entityObj;
        
    }
    
}
