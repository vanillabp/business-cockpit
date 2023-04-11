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

    private static final String INDEX_WORKFLOWMODULE_URL = "workflowModule_url";

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
                        .on("url", Direction.ASC)
                        .named("workflowModule_url"))
                .block();
        
        return List.of(
                "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + V000001.INDEX_WORKFLOWMODULE_URL + "' }",
                "{ drop: '" + UserTask.COLLECTION_NAME + "' }");
        
    }
    
}
