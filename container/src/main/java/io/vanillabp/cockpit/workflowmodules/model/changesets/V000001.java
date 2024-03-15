package io.vanillabp.cockpit.workflowmodules.model.changesets;

import io.vanillabp.cockpit.commons.mongo.changesets.Changeset;
import io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("V100_WorkflowModule")
@ChangesetConfiguration(author = "stephanpelikan")
public class V000001 {

    @Changeset(order = 2000)
    public List<String> createWorkflowModuleCollection(
            final ReactiveMongoTemplate mongo) {

        mongo
                .createCollection(WorkflowModule.COLLECTION_NAME)
                .block();

        return List.of(
                "{ drop: '" + Workflow.COLLECTION_NAME + "' }");

    }

}
