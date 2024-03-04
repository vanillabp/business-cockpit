package io.vanillabp.cockpit.workflowlist.model.changesets;

import io.vanillabp.cockpit.commons.mongo.changesets.Changeset;
import io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("V100_Workflow")
@ChangesetConfiguration(author = "gwieshammer")
public class V000001 {

    private static final String INDEX_DEFAULT_SORT = Workflow.COLLECTION_NAME + "_defaultSort";
    private static final String INDEX_WORKFLOWMODULE_URI = Workflow.COLLECTION_NAME + "_workflowModuleUri";
    private static final String INDEX_ENDED_AT = Workflow.COLLECTION_NAME + "_endedAt";
    private static final String INDEX_FULLTEXT = Workflow.COLLECTION_NAME + "_fulltext";

    @Changeset(order = 1000)
    public List<String> createWorkflowCollection(
            final ReactiveMongoTemplate mongo) {

        mongo
                .createCollection(Workflow.COLLECTION_NAME)
                .block();

        // necessary to accelerate initialization of
        // microservice proxies on startup
        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("workflowModule", Sort.Direction.ASC)
                        .on("workflowModuleUri", Sort.Direction.ASC)
                        .named(INDEX_WORKFLOWMODULE_URI))
                .block();

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

        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("endedAt", Sort.Direction.ASC)
                        .named(INDEX_ENDED_AT))
                .block();

        return "{ dropIndexes: '" + Workflow.COLLECTION_NAME + "', index: '" + INDEX_ENDED_AT + "' }";

    }
    
    @Changeset(order = 1003, author = "stephanpelikan")
    public String fixDefaultUserTaskIndex(
            final ReactiveMongoTemplate mongo) {
        
        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .dropIndex(INDEX_DEFAULT_SORT)
                .then(
                        mongo
                        .indexOps(Workflow.COLLECTION_NAME)
                        .ensureIndex(new Index()
                                .on("createdAt", Direction.ASC)
                                .on("_id", Direction.ASC)
                                .named(INDEX_DEFAULT_SORT)))
                .block();
        
        return null;
        
    }

    @Changeset(order = 1004, author = "stephanpelikan")
    public String createDetailsFulltextSearchIndex(
            final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("detailsFulltextSearch", Direction.ASC)
                        .named(INDEX_FULLTEXT))
                .block();

        return null;

    }

    @Changeset(order = 1005, author = "stephanpelikan")
    public String renameWorkflowModuleIntoWorkflowModuleId(
            final ReactiveMongoTemplate mongo) {

        // necessary to accelerate initialization of
        // microservice proxies on startup
        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .dropIndex(INDEX_WORKFLOWMODULE_URI)
                .block();

        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("workflowModuleId", Direction.ASC)
                        .on("workflowModuleUri", Direction.ASC)
                        .named(INDEX_WORKFLOWMODULE_URI))
                .block();

        return null;

    }

}
