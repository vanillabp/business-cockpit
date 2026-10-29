package io.vanillabp.cockpit.workflowlist.model.changesets;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import com.phactum.mongodb.changesets.DbChangeset;
import com.phactum.mongodb.changesets.DbChangesetConfiguration;

import io.vanillabp.cockpit.workflowlist.model.Workflow;

/**
 * The second database changeset of the workflows. It adds the indexes which let MongoDB count a
 * list of workflows without reading the whole collection, the same way as for the user tasks.
 */
@Component("V101_Workflow")
@DbChangesetConfiguration(author = "stephanpelikan")
public class V000002 {

  /**
   * One index per reason a workflow is visible to a user, each followed by {@code endedAt}, for
   * the same reason as {@link io.vanillabp.cockpit.tasklist.model.changesets.V000002}. The code
   * which creates them is a copy of the one there, because a changeset which ran once must never
   * change, and a shared method could.
   */
  static final Map<String, String> INDEXES_OF_THE_VISIBILITY = indexes(
      "_visibility_accessibleToUsers", "accessibleToUsers._id",
      "_visibility_accessibleToGroups", "accessibleToGroups._id",
      "_visibility_dangling", "dangling");

  /** Creates the changeset. The changeset runner finds it as a Spring bean. */
  public V000002() {
  }

  /**
   * Creates the indexes of the visibility, see {@link #INDEXES_OF_THE_VISIBILITY}.
   *
   * @param mongo The template to change the database with
   * @return The commands which undo the changeset by dropping the indexes
   */
  @DbChangeset(order = 1015)
  public List<String> indexTheVisibilityOfAWorkflow(
      final MongoTemplate mongo) {

    return createIndexes(mongo, Workflow.COLLECTION_NAME, INDEXES_OF_THE_VISIBILITY);

  }

  /**
   * Creates one index per entry, on the path and then on {@code endedAt}.
   *
   * @param mongo The template to change the database with
   * @param collection The collection to index
   * @param indexes The name of each index and the path it starts with
   * @return The commands which drop the indexes again
   */
  static List<String> createIndexes(
      final MongoTemplate mongo,
      final String collection,
      final Map<String, String> indexes) {

    indexes.forEach((
        name,
        path) -> mongo
            .indexOps(collection)
            .createIndex(new Index()
                .on(path, Direction.ASC)
                .on("endedAt", Direction.ASC)
                .named(name)));

    return indexes
        .keySet()
        .stream()
        .map(name -> "{ dropIndexes: '"
            + collection
            + "', index: '"
            + name
            + "' }")
        .toList();

  }

  /**
   * @param namesAndPaths The name of an index, then the path it starts with, and so on
   * @return The same pairs as a map, in their order
   */
  static Map<String, String> indexes(
      final String... namesAndPaths) {

    final var indexes = new LinkedHashMap<String, String>();
    for (var position = 0; position < namesAndPaths.length; position += 2) {
      indexes.put(namesAndPaths[position], namesAndPaths[position + 1]);
    }
    return indexes;

  }

}
