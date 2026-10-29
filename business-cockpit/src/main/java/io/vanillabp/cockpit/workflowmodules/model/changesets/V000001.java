package io.vanillabp.cockpit.workflowmodules.model.changesets;

import com.phactum.mongodb.changesets.DbChangeset;
import com.phactum.mongodb.changesets.DbChangesetConfiguration;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The first database changeset of the workflow modules.
 */
@Component("V100_WorkflowModule")
@DbChangesetConfiguration(author = "stephanpelikan")
public class V000001 {

    /** Creates the changeset. The changeset runner finds it as a Spring bean. */
    public V000001() {
    }

    /**
     * Creates the collection which holds the registered workflow modules.
     *
     * @param mongo The template to access the database
     * @return The script a rollback runs. It drops the collection of the workflows and not the one
     *     of the modules, so a rollback of this step leaves the modules in place
     */
    @DbChangeset(order = 2000)
    public List<String> createWorkflowModuleCollection(
            final MongoTemplate mongo) {

        mongo
                .createCollection(WorkflowModule.COLLECTION_NAME);

        return List.of(
                "{ drop: '" + Workflow.COLLECTION_NAME + "' }");

    }

}
