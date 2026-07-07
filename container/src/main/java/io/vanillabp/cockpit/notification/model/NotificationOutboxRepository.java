package io.vanillabp.cockpit.notification.model;

import reactor.core.publisher.Flux;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationOutboxRepository extends ReactiveMongoRepository<NotificationOutboxEntry, String> {

    /**
     * All pending (not yet sent) entries, oldest first. Used by the poller to drain bulks.
     */
    Flux<NotificationOutboxEntry> findBySentAtIsNullOrderByCreatedAtAsc();

}
