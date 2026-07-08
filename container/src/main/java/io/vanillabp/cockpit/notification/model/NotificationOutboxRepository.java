package io.vanillabp.cockpit.notification.model;

import reactor.core.publisher.Flux;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationOutboxRepository extends ReactiveMongoRepository<NotificationOutboxEntry, String> {

    /**
     * All pending (not yet sent) entries that have not exceeded the maximum delivery attempts
     * ("stale"), oldest first. Used by the poller to drain bulks. Resetting {@code attempts} to a
     * value below the maximum in MongoDB makes a stale entry eligible again.
     */
    Flux<NotificationOutboxEntry> findBySentAtIsNullAndAttemptsLessThanOrderByCreatedAtAsc(int maxAttempts);

}
