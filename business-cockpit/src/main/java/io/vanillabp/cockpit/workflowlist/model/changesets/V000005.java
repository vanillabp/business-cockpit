package io.vanillabp.cockpit.workflowlist.model.changesets;

import java.util.List;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.stereotype.Component;

import com.phactum.mongodb.changesets.DbChangeset;
import com.phactum.mongodb.changesets.DbChangesetConfiguration;

import io.vanillabp.cockpit.workflowlist.model.Workflow;

/**
 * The fifth database changeset of the workflows. It adds the index for a list which shows the
 * workflows of some workflow modules only.
 */
@Component("V104_Workflow")
@DbChangesetConfiguration(author = "stephanpelikan")
public class V000005 {

  /** The name of the index. It is written out, because the rollback stored in the database names it. */
  static final String INDEX_OF_THE_WORKFLOW_MODULE = "_workflowModuleIdAndEndedAt";

  /** The fields of the index, in their order. */
  static final List<String> FIELDS_OF_THE_INDEX = List.of("workflowModuleId", "endedAt");

  /** Creates the changeset. The changeset runner finds it as a Spring bean. */
  public V000005() {
  }

  /**
   * Creates an index on {@code workflowModuleId}, followed by {@code endedAt}. A list limited to
   * workflow modules asks for both, because every primary filter asks about the end.
   * <p>
   * A database may already hold an index on the same fields under another name. A build of the
   * cockpit outside the main line created one like that. MongoDB refuses a second index on the
   * same fields, so then this changeset keeps the one which is there and has nothing to undo.
   *
   * @param mongo The template to change the database with
   * @return The command which undoes the changeset by dropping the index, or {@code null} if an
   *         index on the same fields was there already
   */
  @DbChangeset(order = 1019)
  public String indexTheWorkflowModuleOfAWorkflow(
      final MongoTemplate mongo) {

    final var indexes = mongo.indexOps(Workflow.COLLECTION_NAME);
    final var anIndexOnTheSameFieldsExists = indexes
        .getIndexInfo()
        .stream()
        .anyMatch(index -> FIELDS_OF_THE_INDEX.equals(index
            .getIndexFields()
            .stream()
            .map(IndexField::getKey)
            .toList()));
    if (anIndexOnTheSameFieldsExists) {
      return null;
    }

    indexes.createIndex(new Index()
        .on(FIELDS_OF_THE_INDEX.get(0), Direction.ASC)
        .on(FIELDS_OF_THE_INDEX.get(1), Direction.ASC)
        .named(INDEX_OF_THE_WORKFLOW_MODULE));

    return "{ dropIndexes: '"
        + Workflow.COLLECTION_NAME
        + "', index: '"
        + INDEX_OF_THE_WORKFLOW_MODULE
        + "' }";

  }

}
