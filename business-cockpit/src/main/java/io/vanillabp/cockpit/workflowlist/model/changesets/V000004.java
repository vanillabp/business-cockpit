package io.vanillabp.cockpit.workflowlist.model.changesets;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.phactum.mongodb.changesets.ChangesetInformation;
import com.phactum.mongodb.changesets.DbChangeset;
import com.phactum.mongodb.changesets.DbChangesetConfiguration;

/**
 * The fourth database changeset of the workflows. It repairs what {@link V000001} stored for a
 * rollback of the collection.
 * <p>
 * A change to {@link V000001} would not do it. The changeset library stores the rollback in the
 * record of a changeset when the changeset runs, so every database which ran it keeps the old
 * commands. Only a new changeset reaches those records.
 * <p>
 * This is decision 67 in the repository's DECISIONS.md.
 */
@Component("V103_Workflow")
@DbChangesetConfiguration(author = "stephanpelikan")
public class V000004 {

  /**
   * The id under which the changeset library stores the record of
   * {@link V000001#createWorkflowCollection}: the class name of the bean, a {@code #} and the name
   * of the method. It is written out, like every name below, because the record in the database
   * holds these values as text. A renamed constant elsewhere must not make this changeset miss
   * the record.
   */
  public static final String RECORD_OF_THE_WORKFLOW_COLLECTION = "io.vanillabp.cockpit.workflowlist.model.changesets.V000001#createWorkflowCollection";

  /** The commands {@link V000001#createWorkflowCollection} stored, in their order. */
  static final List<String> STORED_ROLLBACK = List.of(
      "{ dropIndexes: 'workflow', index: 'workflow_defaultSort' }",
      "{ dropIndexes: 'workflow', index: 'workflow_workflowModuleUri' }",
      "{ drop: 'workflow' }");

  /** What the rollback runs from now on. */
  static final List<String> CORRECTED_ROLLBACK = List.of(
      "{ drop: 'workflow' }");

  /** Creates the changeset. The changeset runner finds it as a Spring bean. */
  public V000004() {
  }

  /**
   * Makes a rollback of {@link V000001#createWorkflowCollection} drop the collection only.
   * <p>
   * That changeset stored three commands: drop the index of the default sort order, drop the index
   * of the workflow module, drop the collection. Later changesets dropped both indexes, so on
   * every database the first command fails with "index not found". The changeset library stops
   * the rollback of a changeset at its first failing command and keeps its record. So the
   * collection stayed, and the database still counted the changeset as applied.
   * <p>
   * Dropping a collection drops all its indexes, so the drop alone undoes everything the changeset
   * did. Every database has this record, a new one too, because {@link V000001} runs first. A
   * record with any other commands is left alone, because somebody changed it on purpose.
   *
   * @param mongo The template to access the database
   * @return {@code null}, because there is nothing to undo: the old commands could not run
   */
  @DbChangeset(order = 1018)
  public String rollBackTheWorkflowCollectionByDroppingItOnly(
      final MongoTemplate mongo) {

    mongo.updateFirst(
        new Query(Criteria
            .where("_id").is(RECORD_OF_THE_WORKFLOW_COLLECTION)
            .and("rollbackScripts").is(STORED_ROLLBACK)),
        Update.update("rollbackScripts", CORRECTED_ROLLBACK),
        // by the name of the collection and not by the class, so the version of the record
        // stays as the library wrote it
        ChangesetInformation.COLLECTION_NAME);

    return null;

  }

}
