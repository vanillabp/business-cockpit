package io.vanillabp.cockpit.workflowmodules.model.changesets;

import com.phactum.mongodb.changesets.ChangesetInformation;
import com.phactum.mongodb.changesets.DbChangeset;
import com.phactum.mongodb.changesets.DbChangesetConfiguration;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import java.util.List;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * The second database changeset of the workflow modules. It repairs what {@link V000001} stored
 * for a rollback.
 * <p>
 * A change to {@link V000001} would not do it. The changeset library stores the rollback in the
 * record of a changeset when the changeset runs, so every database which ran it keeps the wrong
 * command. Only a new changeset reaches those records.
 */
@Component("V101_WorkflowModule")
@DbChangesetConfiguration(author = "stephanpelikan")
public class V000002 {

    /**
     * The id under which the changeset library stores the record of
     * {@link V000001#createWorkflowModuleCollection}: the class name of the bean, a {@code #} and
     * the name of the method.
     */
    static final String RECORD_OF_THE_MODULE_COLLECTION =
            V000001.class.getName() + "#createWorkflowModuleCollection";

    /** Creates the changeset. The changeset runner finds it as a Spring bean. */
    public V000002() {
    }

    /**
     * Makes a rollback of {@link V000001#createWorkflowModuleCollection} drop the collection of the
     * modules.
     * <p>
     * That changeset creates the collection {@code modules} and answers with a command which drops
     * the collection {@code workflow}. The changeset library stores the command in the record of the
     * changeset when it runs, and a rollback runs what is stored. So a rollback lost every workflow
     * and left the modules behind. Every database has this record, a new one too, because
     * {@link V000001} runs first. This changeset writes the right command into the record. A record
     * with any other command is left alone, because somebody changed it on purpose.
     *
     * @param mongo The template to access the database
     * @return {@code null}, because there is nothing to undo: the old command was wrong
     */
    @DbChangeset(order = 2001)
    public String correctTheRollbackOfTheWorkflowModuleCollection(
            final MongoTemplate mongo) {

        mongo.updateFirst(
                new Query(Criteria
                        .where("_id").is(RECORD_OF_THE_MODULE_COLLECTION)
                        .and("rollbackScripts").is(List.of(dropCommandOf(Workflow.COLLECTION_NAME)))),
                Update.update("rollbackScripts", List.of(dropCommandOf(WorkflowModule.COLLECTION_NAME))),
                // by the name of the collection and not by the class, so the version of the record
                // stays as the library wrote it
                ChangesetInformation.COLLECTION_NAME);

        return null;

    }

    private static String dropCommandOf(
            final String collection) {

        return "{ drop: '" + collection + "' }";

    }

}
