package io.vanillabp.cockpit.users.model.changesets;

import io.vanillabp.cockpit.commons.mongo.changesets.Changeset;
import io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration;
import io.vanillabp.cockpit.users.model.User;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates the {@code users} collection, which holds the users who logged in at least once. The
 * notification feature brought it, but it is a general collection and other features may use it.
 */
@Component("V100_User")
@ChangesetConfiguration(author = "stephanpelikan")
public class V000001 {

    @Changeset(order = 4000)
    public String createUsersCollection(
            final MongoTemplate mongo) {

        mongo
                .createCollection(User.COLLECTION_NAME);

        return "{ drop: '" + User.COLLECTION_NAME + "' }";

    }

}
