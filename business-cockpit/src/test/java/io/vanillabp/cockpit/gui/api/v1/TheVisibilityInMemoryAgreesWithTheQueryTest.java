package io.vanillabp.cockpit.gui.api.v1;

import static io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.userTasks;
import static io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.workflows;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility.TaskFacts;
import io.vanillabp.cockpit.tasklist.api.UserTaskStreamAudience;
import io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.Person;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility.WorkflowFacts;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.api.WorkflowStreamAudience;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * The rule of a visibility exists twice: as the MongoDB query a list is built from, and in Java,
 * where the update streams decide for every open tab without asking the database again. This test
 * holds the two together. It writes tasks and workflows which cover every combination of the
 * fields the rule reads, asks both versions for many views, and fails on the first task one of
 * them lets through and the other does not.
 *
 * <p>The views are built from the record components of the visibility, by reflection, so a new
 * component is varied here without anybody touching this test. And the fields the query reads are
 * compared with the fields the update streams read. A new criterion on a field the streams do not
 * read fails here, before the two rules can drift apart unnoticed.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class TheVisibilityInMemoryAgreesWithTheQueryTest {

    private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

    /** The values a collection of a view is given. Users and groups are mixed on purpose. */
    private static final List<List<String>> VALUES_OF_A_COLLECTION = Arrays.asList(
            null, List.of(), List.of("u1"), List.of("u2", "u3"), List.of("g1"), List.of("g1", "g2"));

    private static final int SAMPLED_TASK_VIEWS = 1_500;

    private static MongoClient mongoClient;

    private static MongoTemplate mongoTemplate;

    @BeforeAll
    static void startMongoDb() {

        MONGODB.start();
        mongoClient = MongoClients.create(
                MONGODB.getConnectionString() + "/" + UpdateStreamFixtures.DATABASE);
        mongoTemplate = UpdateStreamFixtures.cockpitLikeTemplate(mongoClient);
        mongoTemplate.insert(everyKindOfTask(), userTasks());
        mongoTemplate.insert(everyKindOfWorkflow(), workflows());

    }

    @AfterAll
    static void stopMongoDb() {

        if (mongoClient != null) {
            mongoClient.close();
        }
        MONGODB.stop();

    }

    @Test
    @DisplayName("A user task view lets through in memory exactly what its query finds")
    void userTaskViewsAgree() {

        final var service = new UserTaskService();
        final var audience = new UserTaskStreamAudience(mongoTemplate);
        final var ids = idsIn(userTasks());
        final var tasks = audience.factsOf(ids);
        assertThat(tasks).hasSize(ids.size());

        final var differences = new ArrayList<String>();
        userTaskViews().forEach(view -> compare(
                view,
                byQuery(service.buildUserTasksCriteria(
                        view, null, UserTaskService.RetrieveItemsMode.All,
                        List.of(Criteria.where("id").in(ids))), TaskFacts.class, userTasks(),
                        TaskFacts::id),
                inMemory(tasks, view::letsThrough, TaskFacts::id),
                differences));

        assertThat(differences).as("views which disagree").isEmpty();

    }

    @Test
    @DisplayName("A workflow view lets through in memory exactly what its query finds")
    void workflowViewsAgree() {

        final var service = new WorkflowlistService();
        final var audience = new WorkflowStreamAudience(mongoTemplate);
        final var ids = idsIn(workflows());
        final var workflows = audience.factsOf(ids);
        assertThat(workflows).hasSize(ids.size());

        final var differences = new ArrayList<String>();
        workflowViews().forEach(view -> compare(
                view,
                byQuery(service.buildWorkflowlistCriteria(
                        view, null, WorkflowlistService.RetrieveItemsMode.All,
                        List.of(Criteria.where("id").in(ids)), null), WorkflowFacts.class,
                        workflows(), WorkflowFacts::id),
                inMemory(workflows, view::letsThrough, WorkflowFacts::id),
                differences));

        assertThat(differences).as("views which disagree").isEmpty();

    }

    /**
     * The update streams read only the fields of {@link TaskFacts}. A query which reads another
     * field decides on something the rule in memory never sees.
     */
    @Test
    @DisplayName("The query of a user task view reads no field the update streams do not read")
    void userTaskQueryReadsOnlyTheFactsOfATask() {

        final var service = new UserTaskService();
        final var fields = new TreeSet<String>();
        userTaskViews().forEach(view -> fieldsReadBy(
                service.buildUserTasksCriteria(
                        view, null, UserTaskService.RetrieveItemsMode.All, null),
                fields));

        assertThat(fields).isSubsetOf(TaskFacts.fieldNames());

    }

    @Test
    @DisplayName("The query of a workflow view reads no field the update streams do not read")
    void workflowQueryReadsOnlyTheFactsOfAWorkflow() {

        final var service = new WorkflowlistService();
        final var fields = new TreeSet<String>();
        workflowViews().forEach(view -> fieldsReadBy(
                service.buildWorkflowlistCriteria(
                        view, null, WorkflowlistService.RetrieveItemsMode.All, null, null),
                fields));

        assertThat(fields).isSubsetOf(WorkflowFacts.fieldNames());

    }

    /**
     * The four named views of three people, every user task, and a sample of all views the record
     * components allow. The sample is fixed by its seed, so a failure comes back on the next run.
     */
    private static List<UserTaskVisibility> userTaskViews() {

        final var views = new LinkedHashSet<UserTaskVisibility>();
        for (final var person : List.of(
                Person.of("u1", "g1"), Person.of("u2", "g2"), Person.of("u3"))) {
            views.add(UserTaskVisibility.everythingTheUserMayWorkOn(person));
            views.add(UserTaskVisibility.onlyWhatIsTheUsersOwn(person));
            views.add(UserTaskVisibility.whatTheUsersGroupsMayTake(person));
        }
        views.add(UserTaskVisibility.everyUserTask());
        views.addAll(sampleOf(UserTaskVisibility.class, SAMPLED_TASK_VIEWS));
        return List.copyOf(views);

    }

    private static List<WorkflowVisibility> workflowViews() {

        final var views = new LinkedHashSet<WorkflowVisibility>();
        views.add(WorkflowVisibility.workflowsAddressedTo(Person.of("u1", "g1")));
        views.add(WorkflowVisibility.everyWorkflow());
        views.addAll(sampleOf(WorkflowVisibility.class, 1_000));
        return List.copyOf(views);

    }

    /**
     * Views built through the canonical constructor of the record, one random value per
     * component. A component of a type this method cannot vary fails the test, because a view
     * which cannot be varied cannot be compared.
     */
    private static <T extends Record> Set<T> sampleOf(
            final Class<T> type,
            final int size) {

        final var random = new Random(4711);
        final var components = type.getRecordComponents();
        final var parameterTypes = Arrays
                .stream(components)
                .map(RecordComponent::getType)
                .toArray(Class<?>[]::new);
        final var result = new LinkedHashSet<T>();
        try {
            final var constructor = type.getDeclaredConstructor(parameterTypes);
            for (var number = 0; number < size; ++number) {
                final var arguments = new Object[components.length];
                for (var index = 0; index < components.length; ++index) {
                    arguments[index] = randomValueOf(components[index], random);
                }
                result.add(constructor.newInstance(arguments));
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot build a " + type.getSimpleName(), e);
        }
        return result;

    }

    private static Object randomValueOf(
            final RecordComponent component,
            final Random random) {

        if (component.getType() == boolean.class) {
            return random.nextBoolean();
        }
        if (Collection.class.isAssignableFrom(component.getType())) {
            return VALUES_OF_A_COLLECTION.get(random.nextInt(VALUES_OF_A_COLLECTION.size()));
        }
        throw new AssertionError("This test does not know how to vary '" + component.getName()
                + "' of type " + component.getType().getName()
                + ". Teach randomValueOf, and make sure the rule in memory reads the new value.");

    }

    /**
     * Every combination of assignee, candidate users, candidate groups, excluded candidates and
     * admitted users, each with the dangling flag as the cockpit stores it and with the opposite.
     * The opposite is not made up: taking a task back is an {@code $unset}, which leaves the stored
     * flag behind as it was.
     */
    private static List<Document> everyKindOfTask() {

        final var assignees = Arrays.asList(null, "u1", "u2");
        final var users = Arrays.asList(null, List.<String>of(), List.of("u1"), List.of("u2", "u3"));
        final var groups = Arrays.asList(null, List.<String>of(), List.of("g1"), List.of("g2"));
        final var oneUser = Arrays.asList(null, List.of("u1"));
        final var documents = new ArrayList<Document>();
        var number = 0;
        for (final var assignee : assignees) {
            for (final var candidateUsers : users) {
                for (final var candidateGroups : groups) {
                    for (final var excluded : oneUser) {
                        for (final var admitted : oneUser) {
                            for (final var storedAsComputed : List.of(true, false)) {
                                final var dangling = (assignee == null)
                                        && isEmpty(candidateUsers) && isEmpty(candidateGroups);
                                final var document = new Document("_id", "task-" + number++)
                                        .append("dangling", storedAsComputed == dangling);
                                if (assignee != null) {
                                    document.append("assignee", new Document("_id", assignee));
                                }
                                appendIds(document, "candidateUsers", candidateUsers);
                                appendIds(document, "candidateGroups", candidateGroups);
                                appendIds(document, "excludedCandidateUsers", excluded);
                                appendIds(document, "admittedUsers", admitted);
                                documents.add(document);
                            }
                        }
                    }
                }
            }
        }
        return documents;

    }

    private static List<Document> everyKindOfWorkflow() {

        final var users = Arrays.asList(null, List.<String>of(), List.of("u1"), List.of("u2", "u3"));
        final var groups = Arrays.asList(null, List.<String>of(), List.of("g1"), List.of("g2"));
        final var documents = new ArrayList<Document>();
        var number = 0;
        for (final var accessibleToUsers : users) {
            for (final var accessibleToGroups : groups) {
                for (final var storedAsComputed : List.of(true, false)) {
                    final var dangling = isEmpty(accessibleToUsers) && isEmpty(accessibleToGroups);
                    final var document = new Document("_id", "workflow-" + number++)
                            .append("dangling", storedAsComputed == dangling);
                    appendIds(document, "accessibleToUsers", accessibleToUsers);
                    appendIds(document, "accessibleToGroups", accessibleToGroups);
                    documents.add(document);
                }
            }
        }
        return documents;

    }

    private static boolean isEmpty(
            final List<String> ids) {

        return (ids == null) || ids.isEmpty();

    }

    /** A person or a group embedded in a document keeps its id as {@code _id}. */
    private static void appendIds(
            final Document document,
            final String field,
            final List<String> ids) {

        if (ids != null) {
            document.append(field, ids.stream().map(id -> new Document("_id", id)).toList());
        }

    }

    private static List<String> idsIn(
            final String collection) {

        return mongoTemplate
                .getCollection(collection)
                .find()
                .map(document -> document.getString("_id"))
                .into(new ArrayList<>());

    }

    /**
     * The ids the query finds. It is mapped through the facts record, so a path like
     * {@code assignee.id} becomes {@code assignee._id}, the way the cockpit's entities map it.
     */
    private static <T> Set<String> byQuery(
            final CriteriaDefinition criteria,
            final Class<T> mappedAs,
            final String collection,
            final Function<T, String> id) {

        final var query = new Query(criteria);
        query.fields().include("_id");
        return mongoTemplate
                .find(query, mappedAs, collection)
                .stream()
                .map(id)
                .collect(Collectors.toSet());

    }

    private static <T> Set<String> inMemory(
            final List<T> entities,
            final Predicate<T> view,
            final Function<T, String> id) {

        return entities.stream().filter(view).map(id).collect(Collectors.toSet());

    }

    private static void compare(
            final Object view,
            final Set<String> byQuery,
            final Set<String> inMemory,
            final List<String> differences) {

        final var onlyByQuery = new TreeSet<>(byQuery);
        onlyByQuery.removeAll(inMemory);
        final var onlyInMemory = new TreeSet<>(inMemory);
        onlyInMemory.removeAll(byQuery);
        if (!onlyByQuery.isEmpty() || !onlyInMemory.isEmpty()) {
            differences.add(view + ": only the query finds " + onlyByQuery
                    + ", only the rule in memory lets through " + onlyInMemory);
        }

    }

    /**
     * The fields a query reads, by the first part of their path. The id a test adds is left out:
     * it names the entity and decides nothing.
     */
    private static void fieldsReadBy(
            final CriteriaDefinition criteria,
            final Set<String> fields) {

        collectFields(criteria.getCriteriaObject(), fields);

    }

    private static void collectFields(
            final Object value,
            final Set<String> fields) {

        if (value instanceof Map<?, ?> map) {
            map.forEach((key, nested) -> {
                final var name = key.toString();
                if (!name.startsWith("$")) {
                    fields.add(name.split("\\.")[0]);
                }
                collectFields(nested, fields);
            });
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(nested -> collectFields(nested, fields));
        }

    }

}
