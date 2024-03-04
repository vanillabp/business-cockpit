package io.vanillabp.cockpit.tasklist.model.changesets;

import io.vanillabp.cockpit.commons.mongo.changesets.Changeset;
import io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("V100_UserTask")
@ChangesetConfiguration(author = "stephanpelikan")
public class V000001 {

    private static final String INDEX_DEFAULT_SORT = "_defaultSort";
    private static final String INDEX_WORKFLOWMODULE_URI = "_workflowModuleUri";
    private static final String INDEX_ENDED_AT = "_endedAt";
    private static final String INDEX_READ_BY = "_readBy";

    @Changeset(order = 1)
    public List<String> createUsertaskCollection(
            final ReactiveMongoTemplate mongo) {

        mongo
                .createCollection(UserTask.COLLECTION_NAME)
                .block();

        // necessary to accelerate initialization of
        // microservice proxies on startup
        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("workflowModule", Direction.ASC)
                        .on("workflowModuleUri", Direction.ASC)
                        .named(INDEX_WORKFLOWMODULE_URI))
                .block();

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("dueDate", Direction.ASC)
                        .on("createdAt", Direction.ASC)
                        .named(INDEX_DEFAULT_SORT))
                .block();

        return List.of(
                "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + INDEX_DEFAULT_SORT + "' }",
                "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + INDEX_WORKFLOWMODULE_URI + "' }",
                "{ drop: '" + UserTask.COLLECTION_NAME + "' }");

    }

    @Changeset(order = 2)
    public String createUsertaskEndedAtIndex(
            final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("endedAt", Direction.ASC)
                        .named(INDEX_ENDED_AT))
                .block();

        return "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + INDEX_ENDED_AT + "' }";

    }

    @Changeset(order = 3)
    public String changeDefaultUserTaskIndex(
            final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_DEFAULT_SORT)
                .then(
                        mongo
                                .indexOps(UserTask.COLLECTION_NAME)
                                .ensureIndex(new Index()
                                        .on("dueDate", Direction.ASC)
                                        .on("createdAt", Direction.ASC)
                                        .on("id", Direction.ASC)
                                        .named(INDEX_DEFAULT_SORT)))
                .block();

        return null;

    }

    @Changeset(order = 4)
    public String fixDefaultUserTaskIndex(
            final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_DEFAULT_SORT)
                .then(
                        mongo
                                .indexOps(UserTask.COLLECTION_NAME)
                                .ensureIndex(new Index()
                                        .on("dueDate", Direction.ASC)
                                        .on("createdAt", Direction.ASC)
                                        .on("_id", Direction.ASC)
                                        .named(INDEX_DEFAULT_SORT)))
                .block();

        return null;

    }

    @Changeset(order = 5)
    public String clearAndIndexReadBy(
            final ReactiveMongoTemplate mongo) {

        mongo
                .update(UserTask.class)
                .apply(Update.update("readBy", null))
                .all()
                .block();

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("readBy.userId", Direction.ASC)
                        .named(INDEX_READ_BY))
                .block();

        return "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + INDEX_READ_BY + "' }";

    }

    @Changeset(order = 6)
    public String setDanglingFieldAccordingToAssigneeAndCandidates(
            final ReactiveMongoTemplate mongo) {

        mongo
                .updateMulti(
                        Query.query(
                                new Criteria().norOperator(
                                        Criteria.where("assignee").exists(true),
                                        Criteria.where("candidateUsers.0").exists(true),
                                        Criteria.where("candidateGroups.0").exists(true))),
                        Update.update("dangling", Boolean.TRUE),
                        UserTask.class)
                .block();

        return null;

    }

    @Changeset(order = 7)
    public String renameWorkflowModuleIntoWorkflowModuleId(
            final ReactiveMongoTemplate mongo) {

        // necessary to accelerate initialization of
        // microservice proxies on startup
        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_WORKFLOWMODULE_URI)
                .block();

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("workflowModuleId", Direction.ASC)
                        .on("workflowModuleUri", Direction.ASC)
                        .named(INDEX_WORKFLOWMODULE_URI))
                .block();

        return null;

    }

}