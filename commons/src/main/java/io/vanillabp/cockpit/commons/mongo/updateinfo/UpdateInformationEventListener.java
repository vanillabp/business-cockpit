package io.vanillabp.cockpit.commons.mongo.updateinfo;

import com.mongodb.client.MongoClient;
import io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException;
import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ConditionalOnClass({ MongoClient.class, MongoTemplate.class })
public class UpdateInformationEventListener implements BeforeConvertCallback<Object> {

    @Autowired
    private UserContext currentUser;

    @Override
    public Object onBeforeConvert(
            final Object entityObj,
            final String collection) {

        if (entityObj instanceof UpdateInformationAware entity) {

            entity.setUpdatedAt(OffsetDateTime.now());
            entity.setUpdatedBy(userLoggedInOrSystem());

        }

        return entityObj;

    }

    /**
     * Writes happening outside a request - change-stream reactions, schedulers, Kafka consumers -
     * have no authenticated user, and neither has an anonymous request. Both are recorded as the
     * system user rather than as no user at all.
     */
    private String userLoggedInOrSystem() {

        final String userLoggedIn;
        try {
            userLoggedIn = currentUser.getUserLoggedIn();
        } catch (BcUnauthorizedException e) {
            return UpdateInformationAware.SYSTEM_USER;
        }
        return userLoggedIn == null
                ? UpdateInformationAware.SYSTEM_USER
                : userLoggedIn;

    }

}
