package io.vanillabp.cockpit.gui.api.v1;

import static io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.subscribe;
import static io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.userTask;
import static io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.userTasks;
import static io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.workflow;
import static io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.workflows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mongodb.MongoClientSettings;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import io.vanillabp.cockpit.commons.security.jwt.JwtAuthenticationToken;
import io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.Person;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.api.UserTaskStreamAudience;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
import io.vanillabp.cockpit.workflowlist.api.WorkflowStreamAudience;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.testcontainers.mongodb.MongoDBContainer;

/**
 * Which update stream a change reaches. The streams decide with the visibility of the lists and
 * ask a real MongoDB, so what is tested here is the query a stream asks, not a double of it.
 *
 * <p>The streams are driven by hand: a change is collected the way the event listener collects it,
 * the filtering tick is called, and what waits for delivery is read off the stream. Nothing is
 * written to a browser, and the scheduler is a double which never runs anything on its own.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AChangeReachesTheStreamsItConcernsTest {

    private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

    private static final AtomicInteger FINDS_OF_USER_TASKS = new AtomicInteger();

    private static final AtomicInteger FINDS_OF_WORKFLOWS = new AtomicInteger();

    private static MongoClient mongoClient;

    private static MongoTemplate mongoTemplate;

    private static final Person MARTIN = Person.of("martin", "accounting");

    private static final Person PETRA = Person.of("petra", "sales");

    private static final Person SUPPORT = Person.of("sam", "support");

    private TaskScheduler taskScheduler;

    private UpdateStreams streams;

    @BeforeAll
    static void startMongoDb() {

        MONGODB.start();
        mongoClient = MongoClients.create(MongoClientSettings
                .builder()
                .applyConnectionString(new ConnectionString(
                        MONGODB.getConnectionString() + "/" + UpdateStreamFixtures.DATABASE))
                .addCommandListener(new CommandListener() {
                    @Override
                    public void commandStarted(
                            final CommandStartedEvent event) {
                        if (!"find".equals(event.getCommandName())) {
                            return;
                        }
                        final var collection = event.getCommand().getString("find").getValue();
                        if (userTasks().equals(collection)) {
                            FINDS_OF_USER_TASKS.incrementAndGet();
                        } else if (workflows().equals(collection)) {
                            FINDS_OF_WORKFLOWS.incrementAndGet();
                        }
                    }
                })
                .build());
        mongoTemplate = UpdateStreamFixtures.cockpitLikeTemplate(mongoClient);

    }

    @AfterAll
    static void stopMongoDb() {

        if (mongoClient != null) {
            mongoClient.close();
        }
        MONGODB.stop();

    }

    @BeforeEach
    void openNoStreamYet() {

        taskScheduler = mock(TaskScheduler.class);
        streams = UpdateStreamFixtures.updateStreams(mongoTemplate, taskScheduler, 2_000);

    }

    @Test
    @DisplayName("A change of a task of somebody else reaches no other stream")
    void aForeignTaskReachesNoOtherStream() {

        final var martin = openedAndReloaded(MARTIN);
        final var petra = openedAndReloaded(PETRA);

        final var task = newId();
        mongoTemplate.insert(userTask(task, null, List.of("sales")), userTasks());
        changed(UserTaskStreamAudience.KIND_OF_ENTITY, task);
        streams.filterCollectedChanges();

        assertThat(deliveredIds(martin)).isEmpty();
        assertThat(deliveredIds(petra)).containsExactly(task);

    }

    @Test
    @DisplayName("A task given to somebody else leaves the one list and enters the other")
    void aTaskGivenToSomebodyElseMovesBetweenTheLists() {

        final var martin = openedAndReloaded(MARTIN);
        final var petra = openedAndReloaded(PETRA);

        final var task = newId();
        mongoTemplate.insert(userTask(task, "martin", List.of()), userTasks());
        listShows(MARTIN, UserTaskVisibility.everythingTheUserMayWorkOn(MARTIN), task);

        mongoTemplate.save(userTask(task, "petra", List.of()), userTasks());
        changed(UserTaskStreamAudience.KIND_OF_ENTITY, task);
        streams.filterCollectedChanges();

        assertThat(deliveredIds(martin))
                .as("martin's list shows the task, so it is told to drop it")
                .containsExactly(task);
        assertThat(deliveredIds(petra))
                .as("petra may see the task now")
                .containsExactly(task);

    }

    /**
     * The case the memory of a stream exists for. The task came with the list, so no stream ever
     * delivered it, and the database no longer lets martin see it. Only the memory knows that his
     * browser still shows it. Both of martin's tabs are told, because a list request does not say
     * which tab it comes from.
     */
    @Test
    @DisplayName("A task delivered by loading the list is taken away again")
    void aTaskDeliveredWithTheListIsTakenAway() {

        final var martinsFirstTab = openedAndReloaded(MARTIN);
        final var martinsSecondTab = openedAndReloaded(MARTIN);
        final var petra = openedAndReloaded(PETRA);

        final var task = newId();
        mongoTemplate.insert(userTask(task, null, List.of("accounting")), userTasks());
        listShows(MARTIN, UserTaskVisibility.everythingTheUserMayWorkOn(MARTIN), task);

        mongoTemplate.save(userTask(task, null, List.of("controlling")), userTasks());
        changed(UserTaskStreamAudience.KIND_OF_ENTITY, task);
        streams.filterCollectedChanges();

        assertThat(deliveredIds(martinsFirstTab)).containsExactly(task);
        assertThat(deliveredIds(martinsSecondTab)).containsExactly(task);
        assertThat(deliveredIds(petra))
                .as("petra neither sees the task nor was shown it")
                .isEmpty();

    }

    @Test
    @DisplayName("A change waits for the filtering tick and is not delivered before")
    void aChangeWaitsForTheFilteringTick() {

        final var petra = openedAndReloaded(PETRA);
        final var task = newId();
        mongoTemplate.insert(userTask(task, "petra", List.of()), userTasks());

        changed(UserTaskStreamAudience.KIND_OF_ENTITY, task);
        assertThat(petra.consumeEvents())
                .as("collected for nobody yet")
                .isEmpty();

        streams.filterCollectedChanges();
        assertThat(deliveredIds(petra)).containsExactly(task);

    }

    @Test
    @DisplayName("The filter asks once per stream when something was collected, and never otherwise")
    void theFilterAsksOnlyWhenSomethingWasCollected() {

        openedAndReloaded(MARTIN);
        openedAndReloaded(PETRA);
        openedAndReloaded(SUPPORT);

        final var findsBefore = FINDS_OF_USER_TASKS.get();
        streams.filterCollectedChanges();
        assertThat(FINDS_OF_USER_TASKS.get() - findsBefore)
                .as("a tick without changes")
                .isZero();

        final var task = newId();
        mongoTemplate.insert(userTask(task, null, List.of("sales")), userTasks());
        changed(UserTaskStreamAudience.KIND_OF_ENTITY, task);
        changed(UserTaskStreamAudience.KIND_OF_ENTITY, task);
        final var workflowFindsBefore = FINDS_OF_WORKFLOWS.get();
        streams.filterCollectedChanges();

        assertThat(FINDS_OF_USER_TASKS.get() - findsBefore)
                .as("one query per open stream, for two changes of the same task")
                .isEqualTo(3);
        assertThat(FINDS_OF_WORKFLOWS.get() - workflowFindsBefore)
                .as("no workflow changed")
                .isZero();

    }

    @Test
    @DisplayName("A new stream asks its lists to load again, so it learns what they show")
    void aNewStreamAsksItsListsToReload() {

        final var stream = subscribe(streams, MARTIN);
        streams.filterCollectedChanges();

        assertThat(reloadedKinds(stream.consumeEvents()))
                .containsExactlyInAnyOrder(
                        UserTaskStreamAudience.KIND_OF_ENTITY,
                        WorkflowStreamAudience.KIND_OF_ENTITY);

    }

    @Test
    @DisplayName("Above the limit a stream forgets, asks its lists to reload, and does not loop")
    void theMemoryOfAStreamIsLimited() {

        streams = UpdateStreamFixtures.updateStreams(mongoTemplate, taskScheduler, 3);
        final var martin = openedAndReloaded(MARTIN);
        final var view = UserTaskVisibility.everythingTheUserMayWorkOn(MARTIN);

        listShows(MARTIN, view, "a", "b");
        listShows(MARTIN, view, "c");
        streams.filterCollectedChanges();
        assertThat(martin.consumeEvents())
                .as("three ids fit")
                .isEmpty();

        listShows(MARTIN, view, "c", "d");
        streams.filterCollectedChanges();
        assertThat(reloadedKinds(martin.consumeEvents()))
                .as("a and b were forgotten, so the task list loads again")
                .containsExactly(UserTaskStreamAudience.KIND_OF_ENTITY);
        assertThat(martin.whichOfTheseTheBrowserShows(
                UserTaskStreamAudience.KIND_OF_ENTITY, List.of("a", "b", "c", "d")))
                .containsExactlyInAnyOrder("c", "d");

        listShows(MARTIN, view, "e", "f", "g", "h");
        streams.filterCollectedChanges();
        assertThat(reloadedKinds(martin.consumeEvents()))
                .as("one answer above the limit replaces the memory")
                .containsExactly(UserTaskStreamAudience.KIND_OF_ENTITY);

        listShows(MARTIN, view, "e", "f", "g", "h");
        streams.filterCollectedChanges();
        assertThat(martin.consumeEvents())
                .as("the answer to the reload loses nothing, so there is no second reload")
                .isEmpty();

    }

    @Test
    @DisplayName("A change of a workflow of somebody else reaches no other stream")
    void aForeignWorkflowReachesNoOtherStream() {

        final var martin = openedAndReloaded(MARTIN);
        final var petra = openedAndReloaded(PETRA);

        final var addressedToSales = newId();
        mongoTemplate.insert(workflow(addressedToSales, List.of("sales")), workflows());
        changed(WorkflowStreamAudience.KIND_OF_ENTITY, addressedToSales);
        streams.filterCollectedChanges();

        assertThat(deliveredIds(martin)).isEmpty();
        assertThat(deliveredIds(petra)).containsExactly(addressedToSales);

    }

    /**
     * A support team may see every workflow in its list. Its stream starts with the workflows
     * addressed to its person, and learns the wider view from the list.
     */
    @Test
    @DisplayName("A list with a wider view than the default wakes its stream for what it shows")
    void aWiderViewOfAListWidensItsStream() {

        final var support = openedAndReloaded(SUPPORT);
        final var addressedToSales = newId();
        mongoTemplate.insert(workflow(addressedToSales, List.of("sales")), workflows());

        changed(WorkflowStreamAudience.KIND_OF_ENTITY, addressedToSales);
        streams.filterCollectedChanges();
        assertThat(deliveredIds(support))
                .as("before the list answered with every workflow")
                .isEmpty();

        streams.listAnswered(
                WorkflowStreamAudience.KIND_OF_ENTITY, SUPPORT, WorkflowVisibility.everyWorkflow(),
                List.of());
        changed(WorkflowStreamAudience.KIND_OF_ENTITY, addressedToSales);
        streams.filterCollectedChanges();
        assertThat(deliveredIds(support)).containsExactly(addressedToSales);

    }

    @Test
    @DisplayName("An event about no single entity reaches every stream")
    void anEventAboutNoEntityReachesEveryStream() {

        final var martin = openedAndReloaded(MARTIN);
        final var petra = openedAndReloaded(PETRA);

        streams.collectChange(new GuiEvent("WorkflowModule", null, Map.of("id", "module")));
        streams.filterCollectedChanges();

        assertThat(martin.consumeEvents()).hasSize(1);
        assertThat(petra.consumeEvents()).hasSize(1);

    }

    @Test
    @DisplayName("A stream ends when the sign-in it was opened with expires")
    void aStreamEndsWithItsSignIn() {

        final var expiresAt = Instant.now().plusSeconds(3_600);
        final var emitter = streams.subscribe(MARTIN, Optional.of(expiresAt));
        assertThat(streams.openStreams()).hasSize(1);

        final var endOfTheStream = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(endOfTheStream.capture(), eq(expiresAt));
        endOfTheStream.getValue().run();

        assertThat(streams.openStreams())
                .as("the stream of %s is gone", emitter)
                .isEmpty();

    }

    @Test
    @DisplayName("The end of a sign-in is read from the cockpit's JWT")
    void theEndOfASignInIsReadFromTheJwt() {

        final var expiresAt = Instant.now().plusSeconds(600).truncatedTo(
                java.time.temporal.ChronoUnit.SECONDS);
        final var jwt = Jwt
                .withTokenValue("token")
                .header("alg", "HS256")
                .subject("martin")
                .issuedAt(expiresAt.minusSeconds(60))
                .expiresAt(expiresAt)
                .build();
        final var signIn = new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("accounting")));

        assertThat(LoginApiController.whenTheSignInExpires(signIn)).contains(expiresAt);
        assertThat(LoginApiController.whenTheSignInExpires(null)).isEmpty();

    }

    private UpdateEmitter openedAndReloaded(
            final Person person) {

        final var stream = subscribe(streams, person);
        // the first tick hands every new stream its reload. It is drained here, so a test sees
        // only what its own change caused
        streams.filterCollectedChanges();
        stream.consumeEvents();
        return stream;

    }

    private void listShows(
            final Person person,
            final Object view,
            final String... ids) {

        streams.listAnswered(
                view instanceof WorkflowVisibility
                        ? WorkflowStreamAudience.KIND_OF_ENTITY
                        : UserTaskStreamAudience.KIND_OF_ENTITY,
                person,
                view,
                List.of(ids));

    }

    private void changed(
            final String kindOfEntity,
            final String id) {

        streams.collectChange(new GuiEvent(kindOfEntity, id, Map.of("id", id)));

    }

    private static List<String> deliveredIds(
            final UpdateEmitter stream) {

        return stream
                .consumeEvents()
                .stream()
                .map(GuiEvent::getEntityId)
                .distinct()
                .toList();

    }

    private static Set<String> reloadedKinds(
            final List<GuiEvent> events) {

        return events
                .stream()
                .filter(event -> event.getEntityId() == null)
                .map(GuiEvent::getKindOfEntity)
                .collect(Collectors.toSet());

    }

    private static String newId() {

        return UUID.randomUUID().toString();

    }

}
