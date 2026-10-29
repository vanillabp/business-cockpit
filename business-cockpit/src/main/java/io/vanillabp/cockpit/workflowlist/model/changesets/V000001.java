package io.vanillabp.cockpit.workflowlist.model.changesets;

import static io.vanillabp.cockpit.workflowlist.WorkflowlistService.INDEX_CUSTOM_SORT_PREFIX;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.bson.types.BasicBSONList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.phactum.mongodb.changesets.DbChangeset;
import com.phactum.mongodb.changesets.DbChangesetConfiguration;

import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;

/**
 * The changesets of the {@code workflow} collection: the collection itself, its indexes and the
 * changes to stored workflows which new fields brought. A changeset runs once per database, in
 * the order of its number.
 */
@Component("V100_Workflow")
@DbChangesetConfiguration(author = "gwieshammer")
public class V000001 {

  private static final String INDEX_DEFAULT_SORT = Workflow.COLLECTION_NAME
      + "_defaultSort";
  private static final String INDEX_WORKFLOWMODULE_URI = Workflow.COLLECTION_NAME
      + "_workflowModuleUri";
  private static final String INDEX_ENDED_AT = Workflow.COLLECTION_NAME
      + "_endedAt";
  private static final String INDEX_FULLTEXT = Workflow.COLLECTION_NAME
      + "_fulltext";
  private static final String INDEX_BUSINESS_ID = INDEX_CUSTOM_SORT_PREFIX
      + "businessId";

  @Autowired
  private PersonAndGroupMapper personAndGroupMapper;

  /**
   * Called by Spring.
   */
  public V000001() {

  }

  /**
   * Creates the collection, an index on the workflow module and its URI, and an index for the
   * default sort order.
   *
   * @param mongo The template to change the database with
   * @return The commands which undo the changeset by dropping the indexes and the collection
   */
  @DbChangeset(order = 1000)
  public List<String> createWorkflowCollection(
      final MongoTemplate mongo) {

    mongo
        .createCollection(Workflow.COLLECTION_NAME);

    // necessary to accelerate initialization of
    // microservice proxies on startup
    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .createIndex(new Index()
            .on("workflowModule", Sort.Direction.ASC)
            .on("workflowModuleUri", Sort.Direction.ASC)
            .named(INDEX_WORKFLOWMODULE_URI));

    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .createIndex(new Index()
            .on("createdAt", Sort.Direction.ASC)
            .named(INDEX_DEFAULT_SORT));

    return List.of(
        "{ dropIndexes: '"
            + Workflow.COLLECTION_NAME
            + "', index: '"
            + INDEX_DEFAULT_SORT
            + "' }",
        "{ dropIndexes: '"
            + Workflow.COLLECTION_NAME
            + "', index: '"
            + INDEX_WORKFLOWMODULE_URI
            + "' }",
        "{ drop: '"
            + Workflow.COLLECTION_NAME
            + "' }");

  }

  /**
   * Creates an index on {@code endedAt}, which tells running workflows from ended ones.
   *
   * @param mongo The template to change the database with
   * @return The command which undoes the changeset by dropping the index
   */
  @DbChangeset(order = 1002)
  public String createWorkflowEndedAtIndex(
      final MongoTemplate mongo) {

    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .createIndex(new Index()
            .on("endedAt", Sort.Direction.ASC)
            .named(INDEX_ENDED_AT));

    return "{ dropIndexes: '"
        + Workflow.COLLECTION_NAME
        + "', index: '"
        + INDEX_ENDED_AT
        + "' }";

  }

  /**
   * Adds {@code _id} to the index of the default sort order, so that two workflows created at the
   * same time have a fixed order. The name of the method speaks of user tasks, but it changes
   * the workflows.
   *
   * @param mongo The template to change the database with
   * @return {@code null}, because the changeset cannot be undone
   */
  @DbChangeset(order = 1003, author = "stephanpelikan")
  public String fixDefaultUserTaskIndex(
      final MongoTemplate mongo) {

    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .dropIndex(INDEX_DEFAULT_SORT);

    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .createIndex(new Index()
            .on("createdAt", Direction.ASC)
            .on("_id", Direction.ASC)
            .named(INDEX_DEFAULT_SORT));

    return null;

  }

  /**
   * Creates an index on the full text a filter without a path searches.
   *
   * @param mongo The template to change the database with
   * @return {@code null}, because the changeset cannot be undone
   */
  @DbChangeset(order = 1004, author = "stephanpelikan")
  public String createDetailsFulltextSearchIndex(
      final MongoTemplate mongo) {

    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .createIndex(new Index()
            .on("detailsFulltextSearch", Direction.ASC)
            .named(INDEX_FULLTEXT));

    return null;

  }

  /**
   * Builds the index on the workflow module again, because the field {@code workflowModule} was
   * renamed to {@code workflowModuleId}.
   *
   * @param mongo The template to change the database with
   * @return {@code null}, because the changeset cannot be undone
   */
  @DbChangeset(order = 1005, author = "stephanpelikan")
  public String renameWorkflowModuleIntoWorkflowModuleId(
      final MongoTemplate mongo) {

    // necessary to accelerate initialization of
    // microservice proxies on startup
    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .dropIndex(INDEX_WORKFLOWMODULE_URI);

    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .createIndex(new Index()
            .on("workflowModuleId", Direction.ASC)
            .on("workflowModuleUri", Direction.ASC)
            .named(INDEX_WORKFLOWMODULE_URI));

    return null;

  }

  /**
   * Drops the index on the workflow module and its URI. The URIs of the workflow modules are
   * stored in a collection of their own now.
   *
   * @param mongo The template to change the database with
   * @return {@code null}, because the changeset cannot be undone
   */
  @DbChangeset(order = 1006)
  public String moveWorkflowModuleUriIntoSeparateCollection(
      final MongoTemplate mongo) {

    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .dropIndex(INDEX_WORKFLOWMODULE_URI);

    return null;

  }

  /**
   * Drops the index of the default sort order. Sort indexes are created when a list needs them.
   *
   * @param mongo The template to change the database with
   * @return {@code null}, because the changeset cannot be undone
   */
  @DbChangeset(order = 1007)
  public String dropDefaultSortIndex( // will be created on demand
      final MongoTemplate mongo) {

    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .dropIndex(INDEX_DEFAULT_SORT);

    return null;

  }

  /**
   * Turns the initiator and the users and groups a workflow is accessible to from plain ids into
   * persons and groups, which carry the text a search looks at and the text a list sorts by.
   *
   * @param mongo The template to change the database with
   * @return {@code null}, because the changeset cannot be undone
   */
  @DbChangeset(order = 1008)
  @SuppressWarnings("unchecked")
  public String introducePersonAndGroupForWorkflows(
      final MongoTemplate mongo) {

    final var query = new Query();
    query.fields().include("_id", "version", "initiator", "accessibleToUsers", "accessibleToGroups");
    mongo
        .find(query, DBObject.class, Workflow.COLLECTION_NAME)
        .forEach(document -> {
          final var newDocument = new Update();
          newDocument.set("version", document.get("version")); // at least one field has to be updated, otherwise all document's fields are deleted
          final var initiator = document.get("initiator");
          if (initiator instanceof String) {
            newDocument.set("initiator", getPerson(initiator.toString()));
          }
          final var accessibleToUsers = document.get("accessibleToUsers");
          if ((accessibleToUsers instanceof List<?> accessibleToUsersList) && !accessibleToUsersList
              .isEmpty() && (accessibleToUsersList.getFirst() instanceof String)) {
            final var newAccessibleToUsers = new BasicBSONList();
            ((List<String>) accessibleToUsersList)
                .stream()
                .map(this::getPerson)
                .filter(Objects::nonNull)
                .forEach(newAccessibleToUsers::add);
            newDocument.set("accessibleToUsers", newAccessibleToUsers);
          }
          final var accessibleToGroups = document.get("accessibleToGroups");
          if ((accessibleToGroups instanceof List<?> accessibleToGroupsList) && !accessibleToGroupsList
              .isEmpty() && (accessibleToGroupsList.getFirst() instanceof String)) {
            final var newAccessibleToGroups = new BasicBSONList();
            ((List<String>) accessibleToGroupsList)
                .stream()
                .map(this::getGroup)
                .filter(Objects::nonNull)
                .forEach(newAccessibleToGroups::add);
            newDocument.set("accessibleToGroups", newAccessibleToGroups);
          }
          final var updateQuery = new Query(Criteria.where("_id").is(document.get("_id")));
          mongo.updateFirst(updateQuery, newDocument, Workflow.COLLECTION_NAME);
        });

    return null;

  }

  /**
   * Turns the id of a user into the stored form of a person. If the application's mapper knows
   * no such person, the id is used for every field.
   *
   * @param userId The id of the user
   * @return The person to store, or {@code null} if the id is {@code null}
   */
  public BasicDBObject getPerson(
      final String userId) {

    if (userId == null) {
      return null;
    }

    final var person = personAndGroupMapper.toModelPerson(userId);
    final var bsonPerson = new BasicDBObject();
    if (person == null) {
      bsonPerson.put("id", userId);
      bsonPerson.put("fulltext", userId);
      bsonPerson.put("sort", userId);
    } else {
      bsonPerson.put("id", person.getId());
      bsonPerson.put("fulltext", person.getFulltext());
      bsonPerson.put("sort", person.getSort());
    }
    return bsonPerson;

  }

  /**
   * Turns the id of a group into the stored form of a group. If the application's mapper knows
   * no such group, the id is used for every field.
   *
   * @param groupId The id of the group
   * @return The group to store, or {@code null} if the id is {@code null}
   */
  public BasicDBObject getGroup(
      final String groupId) {

    if (groupId == null) {
      return null;
    }

    final var group = personAndGroupMapper.toModelGroup(groupId);
    final var bsonGroup = new BasicDBObject();
    if (group == null) {
      bsonGroup.put("id", groupId);
      bsonGroup.put("fulltext", groupId);
      bsonGroup.put("sort", groupId);
    } else {
      bsonGroup.put("id", group.getId());
      bsonGroup.put("fulltext", group.getFulltext());
      bsonGroup.put("sort", group.getSort());
    }
    return bsonGroup;

  }

  /**
   * Drops the custom sort indexes, because their names changed. They are created again when a
   * list needs them.
   *
   * @param mongo The template to change the database with
   * @return {@code null}, because the changeset cannot be undone
   */
  @DbChangeset(order = 1009, author = "stephanpelikan")
  public String dropSortIndexesDueToNewNaming( // will be recreated on demand
      final MongoTemplate mongo) {

    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .getIndexInfo()
        .stream()
        .map(IndexInfo::getName)
        .filter(name -> name.startsWith(INDEX_CUSTOM_SORT_PREFIX))
        .toList()
        .forEach(name -> mongo.indexOps(Workflow.COLLECTION_NAME).dropIndex(name));

    return null;

  }

  /**
   * Removes the field {@code workflowId} from every workflow, because it was redundant.
   *
   * @param mongo The template to change the database with
   * @return {@code null}, because the changeset cannot be undone
   */
  @DbChangeset(order = 1010, author = "stephanpelikan")
  public String dropRedundantColumnWorkflowId(
      final MongoTemplate mongo) {

    final var unsetUpdate = new Update();
    unsetUpdate.unset("workflowId");
    mongo
        .updateMulti(
            new Query(),
            unsetUpdate,
            Workflow.class);

    return null;

  }

  /**
   * Creates a sort index on the business id, so that the list sorts by it fast.
   *
   * @param mongo The template to change the database with
   * @return The command which undoes the changeset by dropping the index
   */
  @DbChangeset(order = 1011, author = "martingreilinger")
  public String addBusinessIdIndex(
      final MongoTemplate mongo) {

    mongo
        .indexOps(Workflow.COLLECTION_NAME)
        .createIndex(new Index()
            .on("businessId", Sort.Direction.ASC)
            .on("createdAt", Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .named(INDEX_BUSINESS_ID));

    return "{ dropIndexes: '"
        + Workflow.COLLECTION_NAME
        + "', index: '"
        + INDEX_BUSINESS_ID
        + "' }";
  }

  /**
   * Fills {@code reportedAt} in for workflows reported before that property existed. The
   * closest value at hand is {@code createdAt}, the timestamp of the reporting system.
   *
   * @param mongo The template to change the database with
   * @return {@code null}, because the changeset cannot be undone
   * @see io.vanillabp.cockpit.workflowlist.model.Workflow#getReportedAt()
   */
  @DbChangeset(order = 1012)
  public String introduceReportedAt(
      final MongoTemplate mongo) {

    final var query = new Query(Criteria.where("reportedAt").exists(false));
    query.fields().include("_id", "createdAt");
    mongo
        .find(query, DBObject.class, Workflow.COLLECTION_NAME)
        .forEach(document -> mongo
            .updateFirst(
                new Query(Criteria.where("_id").is(document.get("_id"))),
                new Update().set("reportedAt", document.get("createdAt")),
                Workflow.COLLECTION_NAME));

    return null;

  }

  /**
   * Fills {@code latestEventAt} in for workflows reported before that property existed, the way
   * changeset 14 of the user tasks does it, and for the same reason.
   *
   * @param mongo The template to change the database with
   * @return {@code null}, because the changeset cannot be undone
   * @see io.vanillabp.cockpit.workflowlist.model.Workflow#getLatestEventAt()
   */
  @DbChangeset(order = 1013)
  public String introduceLatestEventAt(
      final MongoTemplate mongo) {

    final var query = new Query(Criteria
        .where("latestEventAt").exists(false)
        .and("createdAt").ne(null));
    query.fields().include("_id", "createdAt");
    mongo
        .find(query, DBObject.class, Workflow.COLLECTION_NAME)
        .forEach(document -> mongo
            .updateFirst(
                new Query(Criteria.where("_id").is(document.get("_id"))),
                new Update().set("latestEventAt", document.get("createdAt")),
                Workflow.COLLECTION_NAME));

    return null;

  }

  /**
   * Gives a start to every case the cockpit knows from its end alone. Such a case used to be
   * stored without {@code createdAt}, and that was how the cockpit knew that it still waited for
   * its creation. The user interface needs a start for every case, so such a case now starts
   * at its end, which is what the cockpit does today for an end which arrives alone.
   * {@code knownFromItsEndAlone} says from now on that the case still waits for its creation.
   * <p>
   * Where the end is missing too, the latest event the cockpit knows of is taken, and then the
   * moment the cockpit stored the case. A record with none of the three has nothing to tell and
   * stays as it is.
   *
   * @param mongo The template to change the database with
   * @return {@code null}, because the changeset cannot be undone
   * @see io.vanillabp.cockpit.workflowlist.model.Workflow#isKnownFromItsEndAlone()
   */
  @DbChangeset(order = 1014)
  public String startWhatIsKnownFromItsEndAlone(
      final MongoTemplate mongo) {

    final var query = new Query(Criteria.where("createdAt").is(null));
    query.fields().include("_id", "endedAt", "latestEventAt", "reportedAt");
    mongo
        .find(query, DBObject.class, Workflow.COLLECTION_NAME)
        .forEach(document -> {
          final var start = Stream
              .of("endedAt", "latestEventAt", "reportedAt")
              .map(document::get)
              .filter(Objects::nonNull)
              .findFirst();
          start.ifPresent(value -> mongo
              .updateFirst(
                  new Query(Criteria.where("_id").is(document.get("_id"))),
                  new Update()
                      .set("createdAt", value)
                      .set("knownFromItsEndAlone", true),
                  Workflow.COLLECTION_NAME));
        });

    return null;

  }

}
