package io.vanillabp.cockpit.tasklist.model.changesets;

import io.vanillabp.cockpit.commons.mongo.changesets.Changeset;
import io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration;
import io.vanillabp.cockpit.tasklist.model.OpenedUserTask;
import java.util.List;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Creates the collection of notes about who opened which user task.
 */
@Component("V101_OpenedUserTask")
@ChangesetConfiguration(author = "stephanpelikan")
public class V000002 {

    private static final String INDEX_OPENED_BY_USER = "_openedByUser";
    private static final String INDEX_OPENED_TASK = "_openedTask";

    @Changeset(order = 14)
    public List<String> createOpenedUsertaskCollection(
            final MongoTemplate mongo) {

        mongo
                .createCollection(OpenedUserTask.COLLECTION_NAME);

        // one note per user task and person, and the way a person's notes are read back
        mongo
                .indexOps(OpenedUserTask.COLLECTION_NAME)
                .createIndex(new Index()
                        .on("userId", Direction.ASC)
                        .on("userTaskId", Direction.ASC)
                        .named(INDEX_OPENED_BY_USER)
                        .unique());

        // the notes of one user task, which is how they are dropped with the task
        mongo
                .indexOps(OpenedUserTask.COLLECTION_NAME)
                .createIndex(new Index()
                        .on("userTaskId", Direction.ASC)
                        .named(INDEX_OPENED_TASK));

        return List.of(
                "{ dropIndexes: '" + OpenedUserTask.COLLECTION_NAME
                        + "', index: '" + INDEX_OPENED_BY_USER + "' }",
                "{ dropIndexes: '" + OpenedUserTask.COLLECTION_NAME
                        + "', index: '" + INDEX_OPENED_TASK + "' }",
                "{ drop: '" + OpenedUserTask.COLLECTION_NAME + "' }");

    }

}
