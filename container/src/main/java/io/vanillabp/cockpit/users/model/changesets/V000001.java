package io.vanillabp.cockpit.users.model.changesets;

import io.vanillabp.cockpit.commons.mongo.changesets.Changeset;
import io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration;
import io.vanillabp.cockpit.users.model.User;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Initializes the {@code users} collection (users who logged in at least once). Introduced by the
 * notification feature but generic - other features may reuse the {@code users} collection.
 */
@Component("V100_User")
@ChangesetConfiguration(author = "stephanpelikan")
public class V000001 {

    @Changeset(order = 4000)
    public String createUsersCollection(
            final ReactiveMongoTemplate mongo) {

        mongo
                .createCollection(User.COLLECTION_NAME)
                .block();

        return "{ drop: '" + User.COLLECTION_NAME + "' }";

    }

}
