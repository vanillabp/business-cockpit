package io.vanillabp.cockpit.commons.mongo.updateinfo;

import com.mongodb.reactivestreams.client.MongoClient;
import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserContext;
import java.time.OffsetDateTime;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.ReactiveBeforeConvertCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ConditionalOnClass({ MongoClient.class, ReactiveMongoTemplate.class })
public class UpdateInformationEventListener implements ReactiveBeforeConvertCallback<Object> {

    @Autowired
    private ReactiveUserContext currentUser;

    @Override
    public Publisher<Object> onBeforeConvert(
            final Object entityObj,
            final String collection) {

        if (entityObj instanceof UpdateInformationAware entity) {

            return currentUser
                    .getUserLoggedInAsMono()
                    .switchIfEmpty(Mono.just(UpdateInformationAware.SYSTEM_USER))
                    .map(currentUser -> {
                        entity.setUpdatedAt(OffsetDateTime.now());
                        entity.setUpdatedBy(currentUser);
                        return (Object) entity;
                    });

        }

        return Mono.just(entityObj);

    }

}
