package io.vanillabp.cockpit.notification.model.changesets;

import io.vanillabp.cockpit.commons.mongo.changesets.Changeset;
import io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration;
import io.vanillabp.cockpit.notification.model.NotificationOutboxEntry;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Initializes the {@code notification_outbox} collection with the unique index that makes
 * notification inserts idempotent. (The generic {@code users} collection is created by the
 * changeset in {@code io.vanillabp.cockpit.users.model.changesets}.)
 */
@Component("V100_Notification")
@ChangesetConfiguration(author = "usertask-notifications")
public class V000001 {

    private static final String INDEX_OUTBOX_UNIQUE = "_outboxUnique";
    private static final String INDEX_OUTBOX_PENDING = "_outboxPending";

    @Changeset(order = 1)
    public String createNotificationOutboxCollection(
            final ReactiveMongoTemplate mongo) {

        mongo
                .createCollection(NotificationOutboxEntry.COLLECTION_NAME)
                .block();

        // idempotency: at most one notification per (user task, type, recipient, medium)
        mongo
                .indexOps(NotificationOutboxEntry.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("userTaskId", Direction.ASC)
                        .on("notificationType", Direction.ASC)
                        .on("recipientUserId", Direction.ASC)
                        .on("medium", Direction.ASC)
                        .named(INDEX_OUTBOX_UNIQUE)
                        .unique())
                .block();

        // accelerates draining of pending entries
        mongo
                .indexOps(NotificationOutboxEntry.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("sentAt", Direction.ASC)
                        .on("createdAt", Direction.ASC)
                        .named(INDEX_OUTBOX_PENDING))
                .block();

        return "{ drop: '" + NotificationOutboxEntry.COLLECTION_NAME + "' }";

    }

}
