package io.vanillabp.cockpit.workflowmodules.model.changesets;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.phactum.mongodb.changesets.ChangesetInformation;
import io.vanillabp.cockpit.notification.model.NotificationOutboxEntry;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.User;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowmodules.model.WorkflowModule;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * A changeset which creates a collection answers with the commands that undo it, and the rollback
 * runs them. The commands of the workflow modules dropped the collection of the workflows, so a
 * rollback lost every workflow and left the modules behind.
 *
 * <p>The tests do what the changeset library does, against a real MongoDB, with every other
 * collection of the cockpit in place: run the changeset, store its commands in its record, run the
 * changesets which follow it, and roll back from what the record holds. Only the collection of the
 * changeset may go. Changeset 2001 of {@link V000002} is the one which follows, and it corrects the
 * record of the modules.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ARollbackDropsWhatItsChangesetCreatedTest {

    private static final String RECORD_OF_THE_MODULES_CHANGESET =
            V000001.class.getName() + "#createWorkflowModuleCollection";

    private static final Set<String> COLLECTIONS_OF_THE_COCKPIT = Set.of(
            UserTask.COLLECTION_NAME,
            Workflow.COLLECTION_NAME,
            WorkflowModule.COLLECTION_NAME,
            NotificationOutboxEntry.COLLECTION_NAME,
            User.COLLECTION_NAME);

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

    static Stream<Arguments> changesetsWhichCreateACollection() {

        return Stream.of(
                Arguments.of(
                        UserTask.COLLECTION_NAME,
                        io.vanillabp.cockpit.tasklist.model.changesets.V000001.class.getName()
                                + "#createUsertaskCollection",
                        (Function<MongoTemplate, Object>) mongo ->
                                new io.vanillabp.cockpit.tasklist.model.changesets.V000001()
                                        .createUsertaskCollection(mongo)),
                Arguments.of(
                        Workflow.COLLECTION_NAME,
                        io.vanillabp.cockpit.workflowlist.model.changesets.V000001.class.getName()
                                + "#createWorkflowCollection",
                        (Function<MongoTemplate, Object>) mongo ->
                                new io.vanillabp.cockpit.workflowlist.model.changesets.V000001()
                                        .createWorkflowCollection(mongo)),
                Arguments.of(
                        WorkflowModule.COLLECTION_NAME,
                        io.vanillabp.cockpit.workflowmodules.model.changesets.V000001.class.getName()
                                + "#createWorkflowModuleCollection",
                        (Function<MongoTemplate, Object>) mongo ->
                                new io.vanillabp.cockpit.workflowmodules.model.changesets.V000001()
                                        .createWorkflowModuleCollection(mongo)),
                Arguments.of(
                        NotificationOutboxEntry.COLLECTION_NAME,
                        io.vanillabp.cockpit.notification.model.changesets.V000001.class.getName()
                                + "#createNotificationOutboxCollection",
                        (Function<MongoTemplate, Object>) mongo ->
                                new io.vanillabp.cockpit.notification.model.changesets.V000001()
                                        .createNotificationOutboxCollection(mongo)),
                Arguments.of(
                        User.COLLECTION_NAME,
                        io.vanillabp.cockpit.users.model.changesets.V000001.class.getName()
                                + "#createUsersCollection",
                        (Function<MongoTemplate, Object>) mongo ->
                                new io.vanillabp.cockpit.users.model.changesets.V000001()
                                        .createUsersCollection(mongo)));

    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("changesetsWhichCreateACollection")
    void theRollbackDropsTheCollectionItsChangesetCreatedAndNothingElse(
            final String collection,
            final String record,
            final Function<MongoTemplate, Object> changeset) {

        final var theOthers = COLLECTIONS_OF_THE_COCKPIT
                .stream()
                .filter(name -> !name.equals(collection))
                .collect(Collectors.toSet());
        theOthers.forEach(mongo::createCollection);

        final var rollback = changeset.apply(mongo);
        assertThat(mongo.getCollectionNames()).contains(collection);

        storeTheRecord(record, rollbackScriptsOf(rollback));
        new V000002().correctTheRollbackOfTheWorkflowModuleCollection(mongo);
        storedRollbackOf(record).forEach(this::runLikeTheChangesetLibrary);

        assertThat(mongo.getCollectionNames())
                .filteredOn(name -> !name.equals(ChangesetInformation.COLLECTION_NAME))
                .containsExactlyInAnyOrderElementsOf(theOthers);

    }

    /**
     * A database which created the modules with the wrong rollback keeps the wrong command in the
     * record of that changeset, because the library stores the commands when the changeset runs. The
     * correction rewrites the record.
     */
    @Test
    void theCorrectionRewritesTheRollbackStoredByAnOlderVersion() {

        storeTheRecordOfTheModulesChangeset(List.of("{ drop: '" + Workflow.COLLECTION_NAME + "' }"));

        final var rollback = new V000002().correctTheRollbackOfTheWorkflowModuleCollection(mongo);

        assertThat(rollback).isNull();
        assertThat(storedRollbackOfTheModulesChangeset())
                .containsExactly("{ drop: '" + WorkflowModule.COLLECTION_NAME + "' }");

    }

    /**
     * A database which created the modules after the fix holds the right command already, and the
     * correction leaves it as it is.
     */
    @Test
    void theCorrectionLeavesARightRollbackAsItIs() {

        storeTheRecordOfTheModulesChangeset(List.of("{ drop: '" + WorkflowModule.COLLECTION_NAME + "' }"));

        new V000002().correctTheRollbackOfTheWorkflowModuleCollection(mongo);

        assertThat(storedRollbackOfTheModulesChangeset())
                .containsExactly("{ drop: '" + WorkflowModule.COLLECTION_NAME + "' }");

    }

    /**
     * A database without the record has nothing to correct, and the correction must not create one.
     */
    @Test
    void theCorrectionCreatesNoRecord() {

        new V000002().correctTheRollbackOfTheWorkflowModuleCollection(mongo);

        assertThat(mongo.getCollection(ChangesetInformation.COLLECTION_NAME).countDocuments()).isZero();

    }

    private void storeTheRecordOfTheModulesChangeset(
            final List<String> rollbackScripts) {

        storeTheRecord(RECORD_OF_THE_MODULES_CHANGESET, rollbackScripts);

    }

    private List<String> storedRollbackOfTheModulesChangeset() {

        return storedRollbackOf(RECORD_OF_THE_MODULES_CHANGESET);

    }

    /** Stores the record of a changeset the way the library stores it after the changeset ran. */
    private void storeTheRecord(
            final String record,
            final List<String> rollbackScripts) {

        final var changeset = new ChangesetInformation();
        changeset.setId(record);
        changeset.setRollbackScripts(rollbackScripts);
        mongo.save(changeset);

    }

    private List<String> storedRollbackOf(
            final String record) {

        return mongo
                .findOne(new Query(Criteria.where("_id").is(record)), ChangesetInformation.class)
                .getRollbackScripts();

    }

    @SuppressWarnings("unchecked")
    private static List<String> rollbackScriptsOf(
            final Object rollback) {

        if (rollback instanceof Collection<?> scripts) {
            return List.copyOf((Collection<String>) scripts);
        }
        return List.of((String) rollback);

    }

    /** The same call {@code ChangesetApplier} makes for every stored command. */
    private void runLikeTheChangesetLibrary(
            final String script) {

        mongo.execute(db -> db.runCommand(Document.parse(script)));

    }

}
