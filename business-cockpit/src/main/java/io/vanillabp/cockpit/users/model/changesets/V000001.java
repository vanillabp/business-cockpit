package io.vanillabp.cockpit.users.model.changesets;

import com.phactum.mongodb.changesets.DbChangeset;
import com.phactum.mongodb.changesets.DbChangesetConfiguration;
import io.vanillabp.cockpit.users.model.User;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates the {@code users} collection, which holds the users who logged in at least once. The
 * notification feature brought it, but it is a general collection and other features may use it.
 */
@Component("V100_User")
@DbChangesetConfiguration(author = "stephanpelikan")
public class V000001 {

    @DbChangeset(order = 4000)
    public String createUsersCollection(
            final MongoTemplate mongo) {

        mongo
                .createCollection(User.COLLECTION_NAME);

        return "{ drop: '" + User.COLLECTION_NAME + "' }";

    }

}
