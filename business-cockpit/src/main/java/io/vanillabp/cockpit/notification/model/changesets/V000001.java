package io.vanillabp.cockpit.notification.model.changesets;

import io.vanillabp.cockpit.commons.mongo.changesets.DbChangeset;
import io.vanillabp.cockpit.commons.mongo.changesets.DbChangesetConfiguration;
import io.vanillabp.cockpit.notification.model.NotificationOutboxEntry;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Creates the {@code notification_outbox} collection, with the unique index which makes an insert
 * of a notification idempotent. The {@code users} collection is a general one and is created by
 * the changeset in {@code io.vanillabp.cockpit.users.model.changesets}.
 */
@Component("V100_Notification")
@DbChangesetConfiguration(author = "stephanpelikan")
public class V000001 {

    private static final String INDEX_OUTBOX_UNIQUE = "_outboxUnique";
    private static final String INDEX_OUTBOX_PENDING = "_outboxPending";

    @DbChangeset(order = 3000)
    public String createNotificationOutboxCollection(
            final MongoTemplate mongo) {

        mongo
                .createCollection(NotificationOutboxEntry.COLLECTION_NAME);

        // idempotency: at most one notification per (user task, type, recipient, medium)
        mongo
                .indexOps(NotificationOutboxEntry.COLLECTION_NAME)
                .createIndex(new Index()
                        .on("userTaskId", Direction.ASC)
                        .on("notificationType", Direction.ASC)
                        .on("recipientUserId", Direction.ASC)
                        .on("medium", Direction.ASC)
                        .named(INDEX_OUTBOX_UNIQUE)
                        .unique());

        // accelerates draining of pending entries
        mongo
                .indexOps(NotificationOutboxEntry.COLLECTION_NAME)
                .createIndex(new Index()
                        .on("sentAt", Direction.ASC)
                        .on("createdAt", Direction.ASC)
                        .named(INDEX_OUTBOX_PENDING));

        return "{ drop: '" + NotificationOutboxEntry.COLLECTION_NAME + "' }";

    }

}
