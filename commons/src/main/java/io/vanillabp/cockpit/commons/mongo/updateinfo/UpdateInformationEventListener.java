package io.vanillabp.cockpit.commons.mongo.updateinfo;

import io.vanillabp.cockpit.commons.security.usercontext.reactive.ReactiveUserContext;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.mapping.event.ReactiveBeforeConvertCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@ConditionalOnProperty(prefix = "spring.data.mongodb", name = "uri")
public class UpdateInformationEventListener implements ReactiveBeforeConvertCallback<Object> {

    @Autowired
    private ReactiveUserContext currentUser;

    @Override
    public Publisher<Object> onBeforeConvert(
            final Object entityObj,
            final String collection) {

        if (entityObj instanceof UpdateInformationAware) {

            return currentUser
                    .getUserLoggedInAsMono()
                    .map(currentUser -> {
                        final var entity = (UpdateInformationAware) entityObj;
                        entity.setUpdatedAt(OffsetDateTime.now());
                        entity.setUpdatedBy(currentUser);
                        return entity;
                    });

        }

        return Mono.just(entityObj);

    }
    
}
