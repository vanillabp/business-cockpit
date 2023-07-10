package io.vanillabp.cockpit.workflowlist.model.changesets;

import java.util.List;

import io.vanillabp.cockpit.commons.mongo.changesets.Changeset;
import io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component("V100_Workflow")
@ChangesetConfiguration(author = "gwieshammer")
public class V000001 {

    @Changeset(order = 1000)
    public List<String> createWorkflowCollection(
            final ReactiveMongoTemplate mongo) {

        mongo
                .createCollection(Workflow.COLLECTION_NAME)
                .block();

        // necessary to accelerate initialization of
        // microservice proxies on startup

        final var INDEX_WORKFLOWMODULE_URI = Workflow.COLLECTION_NAME + "_workflowModuleUri";
        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("workflowModule", Sort.Direction.ASC)
                        .on("workflowModuleUri", Sort.Direction.ASC)
                        .named(INDEX_WORKFLOWMODULE_URI))
                .block();

        final var INDEX_DEFAULT_SORT = Workflow.COLLECTION_NAME + "_defaultSort";
        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("createdAt", Sort.Direction.ASC)
                        .named(INDEX_DEFAULT_SORT))
                .block();

        return List.of(
                "{ dropIndexes: '" + Workflow.COLLECTION_NAME + "', index: '" + INDEX_DEFAULT_SORT + "' }",
                "{ dropIndexes: '" + Workflow.COLLECTION_NAME + "', index: '" + INDEX_WORKFLOWMODULE_URI + "' }",
                "{ drop: '" + Workflow.COLLECTION_NAME + "' }");

    }

    @Changeset(order = 1002)
    public String createWorkflowEndedAtIndex(
            final ReactiveMongoTemplate mongo) {

        final var INDEX_ENDED_AT = Workflow.COLLECTION_NAME + "_endedAt";
        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("endedAt", Sort.Direction.ASC)
                        .named(INDEX_ENDED_AT))
                .block();

        return "{ dropIndexes: '" + Workflow.COLLECTION_NAME + "', index: '" + INDEX_ENDED_AT + "' }";

    }
}
