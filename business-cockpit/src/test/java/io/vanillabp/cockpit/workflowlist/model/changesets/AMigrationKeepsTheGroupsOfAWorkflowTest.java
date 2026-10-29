package io.vanillabp.cockpit.workflowlist.model.changesets;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Release 0.0.3 stored the users and groups a workflow is accessible to as plain ids. Changeset
 * 1008 turns them into persons and groups. Up to release 0.4.3 it turned every list of groups into
 * an empty list, so a database which came from 0.0.3 lost who may see its workflows.
 *
 * <p>The tests store workflows the way 0.0.3 did, run changeset 1008 against a real MongoDB and
 * read what is left.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AMigrationKeepsTheGroupsOfAWorkflowTest {

    private static final org.testcontainers.mongodb.MongoDBContainer MONGODB =
            new org.testcontainers.mongodb.MongoDBContainer(ContainerImages.MONGODB);

    private static MongoClient mongoClient;

    private MongoTemplate mongo;

    @BeforeAll
    static void startMongoDb() {

        MONGODB.start();
        mongoClient = MongoClients.create(MONGODB.getConnectionString());

    }

    @AfterAll
    static void stopMongoDb() {

        if (mongoClient != null) {
            mongoClient.close();
        }
        MONGODB.stop();

    }

    @BeforeEach
    void aDatabaseOfItsOwn() {

        mongo = new MongoTemplate(mongoClient, "cockpit-" + UUID.randomUUID());

    }

    @Test
    void theGroupsOfAWorkflowFromReleaseZeroZeroThreeSurviveTheMigration() {

        storeAWorkflowTheWayReleaseZeroZeroThreeDid("w1", List.of("u1"), List.of("accounting", "sales"));
        storeAWorkflowTheWayReleaseZeroZeroThreeDid("w2", List.of(), List.of("accounting"));

        migrate();

        assertThat(groupIdsOf("w1")).containsExactly("accounting", "sales");
        assertThat(groupIdsOf("w2")).containsExactly("accounting");
        assertThat(userIdsOf("w1")).containsExactly("u1");
        assertThat(workflow("w1").get("initiator", Document.class).getString("id")).isEqualTo("starter");

    }

    @Test
    void theGroupsOfAWorkflowCarryTheTextsOfTheMapper() {

        storeAWorkflowTheWayReleaseZeroZeroThreeDid("w1", List.of(), List.of("accounting"));

        migrate();

        final var group = workflow("w1").getList("accessibleToGroups", Document.class).getFirst();
        assertThat(group.getString("fulltext")).isEqualTo("Group accounting");
        assertThat(group.getString("sort")).isEqualTo("accounting-sort");

    }

    /**
     * A workflow whose groups are groups already, the way every release after 0.0.3 stores them.
     * Changeset 1008 leaves them as they are.
     */
    @Test
    void groupsWhichAreGroupsAlreadyStayAsTheyAre() {

        final var group = new Document("id", "accounting")
                .append("fulltext", "Accounting team")
                .append("sort", "a");
        mongo.getCollection(Workflow.COLLECTION_NAME).insertOne(new Document("_id", "w1")
                .append("version", 3)
                .append("accessibleToGroups", List.of(group)));

        migrate();

        assertThat(workflow("w1").getList("accessibleToGroups", Document.class)).containsExactly(group);

    }

    /**
     * Changeset 1008 the way the library runs it on a database of release 0.0.3, which has seen
     * 1000 to 1007 and not 1008.
     */
    private void migrate() {

        final var changeset1008 = new V000001();
        ReflectionTestUtils.setField(changeset1008, "personAndGroupMapper", new TextsOfTheMapper());
        changeset1008.introducePersonAndGroupForWorkflows(mongo);

    }

    private void storeAWorkflowTheWayReleaseZeroZeroThreeDid(
            final String id,
            final List<String> users,
            final List<String> groups) {

        mongo.getCollection(Workflow.COLLECTION_NAME).insertOne(new Document("_id", id)
                .append("version", 1)
                .append("initiator", "starter")
                .append("accessibleToUsers", users)
                .append("accessibleToGroups", groups));

    }

    private Document workflow(
            final String id) {

        return mongo.getCollection(Workflow.COLLECTION_NAME).find(new Document("_id", id)).first();

    }

    private List<String> groupIdsOf(
            final String id) {

        return workflow(id)
                .getList("accessibleToGroups", Document.class)
                .stream()
                .map(group -> group.getString("id"))
                .toList();

    }

    private List<String> userIdsOf(
            final String id) {

        return workflow(id)
                .getList("accessibleToUsers", Document.class)
                .stream()
                .map(user -> user.getString("id"))
                .toList();

    }

    /** Knows every group and gives it texts which differ from its id. */
    private static class TextsOfTheMapper implements PersonAndGroupMapper {

        @Override
        public Person toModelPerson(
                final String personId) {

            return null;

        }

        @Override
        public Group toModelGroup(
                final String groupId) {

            final var group = new Group();
            group.setId(groupId);
            group.setFulltext("Group " + groupId);
            group.setSort(groupId + "-sort");
            return group;

        }

        @Override
        public Person toModelPerson(
                final UserDetails user) {

            return null;

        }

    }

}
