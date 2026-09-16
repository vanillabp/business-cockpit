package io.vanillabp.cockpit.workflowmodules.model.changesets;

import io.vanillabp.cockpit.commons.mongo.changesets.DbChangeset;
import io.vanillabp.cockpit.commons.mongo.changesets.DbChangesetConfiguration;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("V100_WorkflowModule")
@DbChangesetConfiguration(author = "stephanpelikan")
public class V000001 {

    @DbChangeset(order = 2000)
    public List<String> createWorkflowModuleCollection(
            final MongoTemplate mongo) {

        mongo
                .createCollection(WorkflowModule.COLLECTION_NAME);

        return List.of(
                "{ drop: '" + Workflow.COLLECTION_NAME + "' }");

    }

}
