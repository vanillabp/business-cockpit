package io.vanillabp.cockpit.tasklist.model.changesets;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import io.vanillabp.cockpit.commons.mongo.changesets.Changeset;
import io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import java.util.List;
import java.util.Objects;
import org.bson.types.BasicBSONList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component("V100_UserTask")
@ChangesetConfiguration(author = "stephanpelikan")
public class V000001 {

    private static final String INDEX_DEFAULT_SORT = "_defaultSort";
    private static final String INDEX_WORKFLOWMODULE_URI = "_workflowModuleUri";
    private static final String INDEX_ENDED_AT = "_endedAt";
    private static final String INDEX_READ_BY = "_readBy";

    @Autowired
    private PersonAndGroupMapper personAndGroupMapper;

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
                        .on("workflowModuleUri", Direction.ASC)
                        .named(INDEX_WORKFLOWMODULE_URI))
                .block();

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("dueDate", Direction.ASC)
                        .on("createdAt", Direction.ASC)
                        .named(INDEX_DEFAULT_SORT))
                .block();

        return List.of(
                "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + INDEX_DEFAULT_SORT + "' }",
                "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + INDEX_WORKFLOWMODULE_URI + "' }",
                "{ drop: '" + UserTask.COLLECTION_NAME + "' }");

    }

    @Changeset(order = 2)
    public String createUsertaskEndedAtIndex(
            final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("endedAt", Direction.ASC)
                        .named(INDEX_ENDED_AT))
                .block();

        return "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + INDEX_ENDED_AT + "' }";

    }

    @Changeset(order = 3)
    public String changeDefaultUserTaskIndex(
            final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_DEFAULT_SORT)
                .then(
                        mongo
                                .indexOps(UserTask.COLLECTION_NAME)
                                .ensureIndex(new Index()
                                        .on("dueDate", Direction.ASC)
                                        .on("createdAt", Direction.ASC)
                                        .on("id", Direction.ASC)
                                        .named(INDEX_DEFAULT_SORT)))
                .block();

        return null;

    }

    @Changeset(order = 4)
    public String fixDefaultUserTaskIndex(
            final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_DEFAULT_SORT)
                .then(
                        mongo
                                .indexOps(UserTask.COLLECTION_NAME)
                                .ensureIndex(new Index()
                                        .on("dueDate", Direction.ASC)
                                        .on("createdAt", Direction.ASC)
                                        .on("_id", Direction.ASC)
                                        .named(INDEX_DEFAULT_SORT)))
                .block();

        return null;

    }

    @Changeset(order = 5)
    public String clearAndIndexReadBy(
            final ReactiveMongoTemplate mongo) {

        mongo
                .update(UserTask.class)
                .apply(Update.update("readBy", null))
                .all()
                .block();

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("readBy.userId", Direction.ASC)
                        .named(INDEX_READ_BY))
                .block();

        return "{ dropIndexes: '" + UserTask.COLLECTION_NAME + "', index: '" + INDEX_READ_BY + "' }";

    }

    @Changeset(order = 6)
    public String setDanglingFieldAccordingToAssigneeAndCandidates(
            final ReactiveMongoTemplate mongo) {

        mongo
                .updateMulti(
                        Query.query(
                                new Criteria().norOperator(
                                        Criteria.where("assignee").exists(true),
                                        Criteria.where("candidateUsers.0").exists(true),
                                        Criteria.where("candidateGroups.0").exists(true))),
                        Update.update("dangling", Boolean.TRUE),
                        UserTask.class)
                .block();

        return null;

    }

    @Changeset(order = 7)
    public String renameWorkflowModuleIntoWorkflowModuleId(
            final ReactiveMongoTemplate mongo) {

        // necessary to accelerate initialization of
        // microservice proxies on startup
        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_WORKFLOWMODULE_URI)
                .block();

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("workflowModuleId", Direction.ASC)
                        .on("workflowModuleUri", Direction.ASC)
                        .named(INDEX_WORKFLOWMODULE_URI))
                .block();

        return null;

    }

    @Changeset(order = 8)
    public String moveWorkflowModuleUriIntoSeparateCollection(
            final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_WORKFLOWMODULE_URI)
                .block();

        return null;

    }

    @Changeset(order = 9)
    public String dropDefaultSortIndex( // will be created on demand
            final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(UserTask.COLLECTION_NAME)
                .dropIndex(INDEX_DEFAULT_SORT)
                .block();

        return null;

    }

    @Changeset(order = 10)
    public String introducePersonAndGroupForUserTasks(
            final ReactiveMongoTemplate mongo) {

        final var query = new Query();
        query.fields().include("_id", "version", "assignee", "candidateUsers", "candidateGroups");
        mongo
                .find(query, DBObject.class, UserTask.COLLECTION_NAME)
                .collectList()
                .block()
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
                    mongo.updateFirst(updateQuery, newDocument, UserTask.COLLECTION_NAME).block();
                });

        return null;

    }

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

}