package io.vanillabp.cockpit.notification.model;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the outbox of notifications, the collection
 * {@value NotificationOutboxEntry#COLLECTION_NAME}.
 */
@Repository
public interface NotificationOutboxRepository extends MongoRepository<NotificationOutboxEntry, String> {

  /**
   * Every entry which is still waiting to be sent and has not used up its delivery attempts,
   * oldest first. The poller reads it to drain the bulks. Setting {@code attempts} back below
   * the maximum in MongoDB brings a stale entry back into this list.
   *
   * @param maxAttempts the number of delivery attempts after which an entry counts as stale
   * @return the entries still to be sent, oldest first
   */
  List<NotificationOutboxEntry> findBySentAtIsNullAndAttemptsLessThanOrderByCreatedAtAsc(
      int maxAttempts);

}
