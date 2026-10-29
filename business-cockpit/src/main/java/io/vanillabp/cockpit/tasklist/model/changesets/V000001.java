package io.vanillabp.cockpit.tasklist.model.changesets;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.phactum.mongodb.changesets.DbChangeset;
import com.phactum.mongodb.changesets.DbChangesetConfiguration;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.bson.types.BasicBSONList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * The changesets of the {@code usertask} collection: the collection itself, its indexes and the
 * changes to stored tasks which new fields brought. A changeset runs once per database, in the
 * order of its number.
 */
@Component("V100_UserTask")
@DbChangesetConfiguration(author = "stephanpelikan")
public class V000001 {

    private static final String INDEX_DEFAULT_SORT = "_defaultSort";
    private static final String INDEX_WORKFLOWMODULE_URI = "_workflowModuleUri";
    private static final String INDEX_ENDED_AT = "_endedAt";
    private static final String INDEX_READ_BY = "_readBy";

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
    @DbChangeset(order = 1)
    public List<String> createUsertaskCollection(
            final MongoTemplate mongo) {

        mongo
                .createCollection(UserTask.COLLECTION_NAME);

        // necessary to accelerate initialization of
        // microservice proxies on startup
        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .createIndex(new Index()
                        .on("workflowModule", Direction.ASC)
                        .on("workflowModuleUri", Direction.ASC)
                        .named(INDEX_WORKFLOWMODULE_URI));

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .createIndex(new Index()
                        .on("dueDate", Direction.ASC)
                        .on("createdAt", Direction.ASC)
                        .named(INDEX_DEFAULT_SORT));

        return List.of(
                "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + INDEX_DEFAULT_SORT + "' }",
                "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + INDEX_WORKFLOWMODULE_URI + "' }",
                "{ drop: '" + UserTask.COLLECTION_NAME + "' }");

    }

    /**
     * Creates an index on {@code endedAt}, which tells open tasks from ended ones.
     *
     * @param mongo The template to change the database with
     * @return The command which undoes the changeset by dropping the index
     */
    @DbChangeset(order = 2)
    public String createUsertaskEndedAtIndex(
            final MongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .createIndex(new Index()
                        .on("endedAt", Direction.ASC)
                        .named(INDEX_ENDED_AT));

        return "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + INDEX_ENDED_AT + "' }";

    }

    /**
     * Adds the id to the index of the default sort order, so that two tasks with the same dates
     * have a fixed order.
     *
     * @param mongo The template to change the database with
     * @return {@code null}, because the changeset cannot be undone
     */
    @DbChangeset(order = 3)
    public String changeDefaultUserTaskIndex(
            final MongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_DEFAULT_SORT);

                        mongo
                                .indexOps(UserTask.COLLECTION_NAME)
                                .createIndex(new Index()
                                        .on("dueDate", Direction.ASC)
                                        .on("createdAt", Direction.ASC)
                                        .on("id", Direction.ASC)
                                        .named(INDEX_DEFAULT_SORT));

        return null;

    }

    /**
     * Builds the index of the default sort order again with {@code _id}, the name MongoDB stores
     * the id under, instead of {@code id}.
     *
     * @param mongo The template to change the database with
     * @return {@code null}, because the changeset cannot be undone
     */
    @DbChangeset(order = 4)
    public String fixDefaultUserTaskIndex(
            final MongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_DEFAULT_SORT);

                        mongo
                                .indexOps(UserTask.COLLECTION_NAME)
                                .createIndex(new Index()
                                        .on("dueDate", Direction.ASC)
                                        .on("createdAt", Direction.ASC)
                                        .on("_id", Direction.ASC)
                                        .named(INDEX_DEFAULT_SORT));

        return null;

    }

    /**
     * Clears who has read which task, and creates an index on the users who read a task.
     *
     * @param mongo The template to change the database with
     * @return The command which undoes the changeset by dropping the index
     */
    @DbChangeset(order = 5)
    public String clearAndIndexReadBy(
            final MongoTemplate mongo) {

        mongo
                .update(UserTask.class)
                .apply(Update.update("readBy", null))
                .all();

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .createIndex(new Index()
                        .on("readBy.userId", Direction.ASC)
                        .named(INDEX_READ_BY));

        return "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + INDEX_READ_BY + "' }";

    }

    /**
     * Marks every task as {@code dangling} which has no assignee and no candidates, so that nobody
     * is responsible for it.
     *
     * @param mongo The template to change the database with
     * @return {@code null}, because the changeset cannot be undone
     */
    @DbChangeset(order = 6)
    public String setDanglingFieldAccordingToAssigneeAndCandidates(
            final MongoTemplate mongo) {

        mongo
                .updateMulti(
                        Query.query(
                                new Criteria().norOperator(
                                        Criteria.where("assignee").exists(true),
                                        Criteria.where("candidateUsers.0").exists(true),
                                        Criteria.where("candidateGroups.0").exists(true))),
                        Update.update("dangling", Boolean.TRUE),
                        UserTask.class);

        return null;

    }

    /**
     * Builds the index on the workflow module again, because the field {@code workflowModule} was
     * renamed to {@code workflowModuleId}.
     *
     * @param mongo The template to change the database with
     * @return {@code null}, because the changeset cannot be undone
     */
    @DbChangeset(order = 7)
    public String renameWorkflowModuleIntoWorkflowModuleId(
            final MongoTemplate mongo) {

        // necessary to accelerate initialization of
        // microservice proxies on startup
        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_WORKFLOWMODULE_URI);

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
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
    @DbChangeset(order = 8)
    public String moveWorkflowModuleUriIntoSeparateCollection(
            final MongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_WORKFLOWMODULE_URI);

        return null;

    }

    /**
     * Drops the index of the default sort order. Sort indexes are created when a list needs them.
     *
     * @param mongo The template to change the database with
     * @return {@code null}, because the changeset cannot be undone
     */
    @DbChangeset(order = 9)
    public String dropDefaultSortIndex( // will be created on demand
            final MongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_DEFAULT_SORT);

        return null;

    }

    /**
     * Turns the assignee and the candidates of every task from plain ids into persons and groups,
     * which carry the text a search looks at and the text a list sorts by.
     *
     * @param mongo The template to change the database with
     * @return {@code null}, because the changeset cannot be undone
     */
    @DbChangeset(order = 10)
    public String introducePersonAndGroupForUserTasks(
            final MongoTemplate mongo) {

        final var query = new Query();
        query.fields().include("_id", "version", "assignee", "candidateUsers", "candidateGroups");
        mongo
                .find(query, DBObject.class, UserTask.COLLECTION_NAME)
                .forEach(document -> {
                    final var newDocument = new Update();
                    newDocument.set("version", document.get("version")); // at least one field has to be updated, otherwise all document's fields are deleted
                    final var assignee = document.get("assignee");
                    if (assignee instanceof String) {
                        newDocument.set("assignee", getPerson(assignee.toString()));
                    }
                    final var candidateUsers = document.get("candidateUsers");
                    if ((candidateUsers != null)
                            && (candidateUsers instanceof List)
                            && (((List<?>) candidateUsers).size() > 0)
                            && (((List<?>) candidateUsers).get(0) instanceof String)) {
                        final var newCandidateUsers = new BasicBSONList();
                        ((List<String>) candidateUsers)
                                .stream()
                                .map(this::getPerson)
                                .filter(Objects::nonNull)
                                .forEach(newCandidateUsers::add);
                        newDocument.set("candidateUsers", newCandidateUsers);
                    }
                    final var candidateGroups = document.get("candidateGroups");
                    if ((candidateGroups != null)
                            && (candidateGroups instanceof List)
                            && (((List<?>) candidateGroups).size() > 0)
                            && (((List<?>) candidateGroups).get(0) instanceof String)) {
                        final var newCandidateGroups = new BasicBSONList();
                        ((List<String>) candidateGroups)
                                .stream()
                                .map(this::getGroup)
                                .filter(Objects::nonNull)
                                .forEach(newCandidateGroups::add);
                        newDocument.set("candidateGroups", newCandidateGroups);
                    }
                    final var updateQuery = new Query(Criteria.where("_id").is(document.get("_id")));
                    mongo.updateFirst(updateQuery, newDocument, UserTask.COLLECTION_NAME);
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
     * Fills {@code reportedAt} in for tasks reported before that property existed, and
     * {@code candidateUsersSince} for each of their candidates. The closest value at hand is
     * {@code createdAt}. It is the timestamp of the reporting system, but for a record which is
     * already stored it lies in the past, and that is all the scan for what changed needs.
     *
     * @param mongo The template to change the database with
     * @return {@code null}, because the changeset cannot be undone
     */
    @DbChangeset(order = 12)
    public String introduceReportedAtAndCandidateUsersSince(
            final MongoTemplate mongo) {

        final var query = new Query(Criteria.where("reportedAt").exists(false));
        query.fields().include("_id", "createdAt", "candidateUsers");
        mongo
                .find(query, DBObject.class, UserTask.COLLECTION_NAME)
                .forEach(document -> {
                    final var createdAt = document.get("createdAt");
                    final var update = new Update();
                    update.set("reportedAt", createdAt);
                    final var candidateUsers = document.get("candidateUsers");
                    if (candidateUsers instanceof List<?> candidates) {
                        final var candidateUsersSince = new BasicBSONList();
                        candidates
                                .stream()
                                // a nested document is a Map for both BasicDBObject and
                                // org.bson.Document, whichever the driver hands out
                                .filter(candidate -> candidate instanceof Map)
                                .map(candidate -> ((Map<?, ?>) candidate).get("id"))
                                .filter(Objects::nonNull)
                                .forEach(id -> {
                                    final var since = new BasicDBObject();
                                    since.put("userId", id);
                                    since.put("timestamp", createdAt);
                                    candidateUsersSince.add(since);
                                });
                        update.set("candidateUsersSince", candidateUsersSince);
                    }
                    mongo
                            .updateFirst(
                                    new Query(Criteria.where("_id").is(document.get("_id"))),
                                    update,
                                    UserTask.COLLECTION_NAME);
                });

        return null;

    }

    /**
     * Drops the custom sort indexes, because their names changed. They are created again when a
     * list needs them.
     *
     * @param mongo The template to change the database with
     * @return {@code null}, because the changeset cannot be undone
     */
    @DbChangeset(order = 11)
    public String dropSortIndexesDueToNewNaming( // will be recreated on demand
            final MongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .getIndexInfo()
                .stream()
                .map(indexInfo -> indexInfo.getName())
                .filter(name -> name.startsWith(UserTaskService.INDEX_CUSTOM_SORT_PREFIX))
                .toList()
                .forEach(name -> mongo.indexOps(UserTask.COLLECTION_NAME).dropIndex(name));

        return null;

    }

    /**
     * Fills {@code latestEventAt} in for tasks reported before that property existed.
     * {@code createdAt} is the timestamp of the event which created the task. So it comes from the
     * reporting system's own clock, and it lies before every later event of that task. That is
     * what the comparison of reports needs. A task whose value is missing takes the next report
     * whatever its timestamp says. A task whose value is too early takes a report it should have
     * refused, which is the smaller loss of the two.
     * <p>
     * A task without a {@code createdAt} keeps an empty {@code latestEventAt}. There is nothing to
     * copy for it. An invented value would claim that the cockpit knows which event its state came
     * from, and it does not.
     *
     * @param mongo The template to change the database with
     * @return {@code null}, because the changeset cannot be undone
     * @see io.vanillabp.cockpit.tasklist.model.UserTask#getLatestEventAt()
     */
    @DbChangeset(order = 14)
    public String introduceLatestEventAt(
            final MongoTemplate mongo) {

        final var query = new Query(Criteria
                .where("latestEventAt").exists(false)
                .and("createdAt").ne(null));
        query.fields().include("_id", "createdAt");
        mongo
                .find(query, DBObject.class, UserTask.COLLECTION_NAME)
                .forEach(document -> mongo
                        .updateFirst(
                                new Query(Criteria.where("_id").is(document.get("_id"))),
                                new Update().set("latestEventAt", document.get("createdAt")),
                                UserTask.COLLECTION_NAME));

        return null;

    }

    /**
     * Drops the assignee of tasks which were reported without one. Until the mappers of the REST
     * ingress learned that a missing user id means no person, they stored an assignee whose id was
     * null. Such a task looks unassigned in the user interface but cannot be claimed. Comparing
     * the stored assignee to the claiming user reads the null id, and the request ends in HTTP
     * 500.
     * <p>
     * That nobody is responsible for a task is stored as {@code dangling}, so that it can be
     * queried. The tasks which lose their phantom assignee therefore have to be judged again, the
     * way changeset 6 did it.
     *
     * @param mongo The template to change the database with
     * @return {@code null}, because the changeset cannot be undone
     */
    @DbChangeset(order = 13)
    public String removeAssigneesHavingNoUserId(
            final MongoTemplate mongo) {

        mongo
                .updateMulti(
                        Query.query(Criteria
                                .where("assignee").ne(null)
                                .and("assignee.id").is(null)),
                        new Update().unset("assignee"),
                        UserTask.class);

        mongo
                .updateMulti(
                        Query.query(
                                new Criteria().norOperator(
                                        Criteria.where("assignee").exists(true),
                                        Criteria.where("candidateUsers.0").exists(true),
                                        Criteria.where("candidateGroups.0").exists(true))),
                        Update.update("dangling", Boolean.TRUE),
                        UserTask.class);

        return null;

    }

    /**
     * Gives a start to every task the cockpit knows from its end alone. Such a task used to be
     * stored without {@code createdAt}, and that was how the cockpit knew that it still waited for
     * its creation. The user interface needs a start for every task, so such a task now starts
     * at its end, which is what the cockpit does today for an end which arrives alone.
     * {@code knownFromItsEndAlone} says from now on that the task still waits for its creation.
     * <p>
     * Where the end is missing too, the latest event the cockpit knows of is taken, and then the
     * moment the cockpit stored the task. A record with none of the three has nothing to tell and
     * stays as it is.
     *
     * @param mongo The template to change the database with
     * @return {@code null}, because the changeset cannot be undone
     * @see io.vanillabp.cockpit.tasklist.model.UserTask#isKnownFromItsEndAlone()
     */
    @DbChangeset(order = 15)
    public String startWhatIsKnownFromItsEndAlone(
            final MongoTemplate mongo) {

        final var query = new Query(Criteria.where("createdAt").is(null));
        query.fields().include("_id", "endedAt", "latestEventAt", "reportedAt");
        mongo
                .find(query, DBObject.class, UserTask.COLLECTION_NAME)
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
                                    UserTask.COLLECTION_NAME));
                });

        return null;

    }

}