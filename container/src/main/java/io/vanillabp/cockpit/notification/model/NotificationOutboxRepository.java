package io.vanillabp.cockpit.notification.model;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationOutboxRepository extends MongoRepository<NotificationOutboxEntry, String> {

    /**
     * All pending (not yet sent) entries that have not exceeded the maximum delivery attempts
     * ("stale"), oldest first. Used by the poller to drain bulks. Resetting {@code attempts} to a
     * value below the maximum in MongoDB makes a stale entry eligible again.
     */
    List<NotificationOutboxEntry> findBySentAtIsNullAndAttemptsLessThanOrderByCreatedAtAsc(int maxAttempts);

}
