package io.vanillabp.cockpit.tasklist.model.changesets;

import java.util.List;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import io.vanillabp.cockpit.commons.mongo.changesets.Changeset;
import io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration;
import io.vanillabp.cockpit.tasklist.model.UserTask;

@Component("V100_UserTask")
@ChangesetConfiguration(author = "stephanpelikan")
public class V000001 {

    private static final String INDEX_WORKFLOWMODULE_URI = UserTask.COLLECTION_NAME + "_workflowModuleUri";
    private static final String INDEX_DEFAULT_SORT = UserTask.COLLECTION_NAME + "_defaultSort";

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
    
}
