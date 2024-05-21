package io.vanillabp.cockpit.workflowlist.model.changesets;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import io.vanillabp.cockpit.commons.mongo.changesets.Changeset;
import io.vanillabp.cockpit.commons.mongo.changesets.ChangesetConfiguration;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.util.List;
import java.util.Objects;
import org.bson.types.BasicBSONList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component("V100_Workflow")
@ChangesetConfiguration(author = "gwieshammer")
public class V000001 {

    private static final String INDEX_DEFAULT_SORT = Workflow.COLLECTION_NAME + "_defaultSort";
    private static final String INDEX_WORKFLOWMODULE_URI = Workflow.COLLECTION_NAME + "_workflowModuleUri";
    private static final String INDEX_ENDED_AT = Workflow.COLLECTION_NAME + "_endedAt";
    private static final String INDEX_FULLTEXT = Workflow.COLLECTION_NAME + "_fulltext";

    @Autowired
    private PersonAndGroupMapper personAndGroupMapper;

    @Changeset(order = 1000)
    public List<String> createWorkflowCollection(
            final ReactiveMongoTemplate mongo) {

        mongo
                .createCollection(Workflow.COLLECTION_NAME)
                .block();

        // necessary to accelerate initialization of
        // microservice proxies on startup
        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("workflowModule", Sort.Direction.ASC)
                        .on("workflowModuleUri", Sort.Direction.ASC)
                        .named(INDEX_WORKFLOWMODULE_URI))
                .block();

        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("createdAt", Sort.Direction.ASC)
                        .named(INDEX_DEFAULT_SORT))
                .block();

        return List.of(
                "{ dropIndexes: '" + Workflow.COLLECTION_NAME + "', index: '" + INDEX_DEFAULT_SORT + "' }",
                "{ dropIndexes: '" + Workflow.COLLECTION_NAME + "', index: '" + INDEX_WORKFLOWMODULE_URI + "' }",
                "{ drop: '" + Workflow.COLLECTION_NAME + "' }");

    }

    @Changeset(order = 1002)
    public String createWorkflowEndedAtIndex(
            final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("endedAt", Sort.Direction.ASC)
                        .named(INDEX_ENDED_AT))
                .block();

        return "{ dropIndexes: '" + Workflow.COLLECTION_NAME + "', index: '" + INDEX_ENDED_AT + "' }";

    }
    
    @Changeset(order = 1003, author = "stephanpelikan")
    public String fixDefaultUserTaskIndex(
            final ReactiveMongoTemplate mongo) {
        
        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .dropIndex(INDEX_DEFAULT_SORT)
                .then(
                        mongo
                        .indexOps(Workflow.COLLECTION_NAME)
                        .ensureIndex(new Index()
                                .on("createdAt", Direction.ASC)
                                .on("_id", Direction.ASC)
                                .named(INDEX_DEFAULT_SORT)))
                .block();
        
        return null;
        
    }

    @Changeset(order = 1004, author = "stephanpelikan")
    public String createDetailsFulltextSearchIndex(
            final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("detailsFulltextSearch", Direction.ASC)
                        .named(INDEX_FULLTEXT))
                .block();

        return null;

    }

    @Changeset(order = 1005, author = "stephanpelikan")
    public String renameWorkflowModuleIntoWorkflowModuleId(
            final ReactiveMongoTemplate mongo) {

        // necessary to accelerate initialization of
        // microservice proxies on startup
        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .dropIndex(INDEX_WORKFLOWMODULE_URI)
                .block();

        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .ensureIndex(new Index()
                        .on("workflowModuleId", Direction.ASC)
                        .on("workflowModuleUri", Direction.ASC)
                        .named(INDEX_WORKFLOWMODULE_URI))
                .block();

        return null;

    }

    @Changeset(order = 1006)
    public String moveWorkflowModuleUriIntoSeparateCollection(
            final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .dropIndex(INDEX_WORKFLOWMODULE_URI)
                .block();

        return null;

    }

    @Changeset(order = 1007)
    public String dropDefaultSortIndex( // will be created on demand
                                        final ReactiveMongoTemplate mongo) {

        mongo
                .indexOps(Workflow.COLLECTION_NAME)
                .dropIndex(INDEX_DEFAULT_SORT)
                .block();

        return null;

    }

    @Changeset(order = 1008)
    public String introducePersonAndGroupForWorkflows(
            final ReactiveMongoTemplate mongo) {

        final var query = new Query();
        query.fields().include("_id", "initiator", "accessibleToUsers", "accessibleToGroups");
        mongo
                .find(query, DBObject.class, Workflow.COLLECTION_NAME)
                .collectList()
                .block()
                .forEach(document -> {
                    final var newDocument = new Update();
                    final var initiator = document.get("initiator");
                    if (initiator instanceof String) {
                        newDocument.set("initiator", getPerson(initiator.toString()));
                    }
                    final var accessibleToUsers = document.get("accessibleToUsers");
                    if ((accessibleToUsers != null)
                            && (accessibleToUsers instanceof List)
                            && (((List<?>) accessibleToUsers).size() > 0)
                            && (((List<?>) accessibleToUsers).get(0) instanceof String)) {
                        final var newAccessibleToUsers = new BasicBSONList();
                        ((List<String>) accessibleToUsers)
                                .stream()
                                .map(this::getPerson)
                                .filter(Objects::nonNull)
                                .forEach(newAccessibleToUsers::add);
                        newDocument.set("accessibleToUsers", newAccessibleToUsers);
                    }
                    final var accessibleToGroups = document.get("accessibleToGroups");
                    if ((accessibleToGroups != null)
                            && (accessibleToGroups instanceof List)
                            && (((List<?>) accessibleToGroups).size() > 0)
                            && (((List<?>) accessibleToGroups).get(0) instanceof String)) {
                        final var newAccessibleToGroups = new BasicBSONList();
                        if ((newAccessibleToGroups.size() > 0)
                                && (newAccessibleToGroups.get(0) instanceof String)) {
                            ((List<String>) accessibleToGroups)
                                    .stream()
                                    .map(this::getGroup)
                                    .filter(Objects::nonNull)
                                    .forEach(newAccessibleToGroups::add);
                        }
                        newDocument.set("accessibleToGroups", newAccessibleToGroups);
                    }
                    final var updateQuery = new Query(Criteria.where("_id").is(document.get("_id")));
                    mongo.updateFirst(updateQuery, newDocument, Workflow.COLLECTION_NAME).block();
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
