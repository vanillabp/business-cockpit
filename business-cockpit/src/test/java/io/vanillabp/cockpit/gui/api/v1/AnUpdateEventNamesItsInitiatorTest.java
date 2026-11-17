package io.vanillabp.cockpit.gui.api.v1;

import static io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.subscribe;
import static io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.userTasks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.Subscription;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.vanillabp.cockpit.config.web.JsonConfiguration;
import io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.Person;
import io.vanillabp.cockpit.mongo.MongoDbProperties;
import io.vanillabp.cockpit.mongo.changestreams.ChangeStreamUtils;
import io.vanillabp.cockpit.mongo.updateinfo.UpdateInformationAware;
import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import io.vanillabp.cockpit.tasklist.UserTaskChangedNotification;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.api.UserTaskStreamAudience;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.util.events.NotificationEvent;
import io.vanillabp.cockpit.workflowlist.WorkflowChangedNotification;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Each event of the update stream names who made the change. A page which shows one user task or
 * case reads it, and shows that somebody else changed it only for a person who is not the user
 * signed in, see decision 102 in the repository's DECISIONS.md.
 *
 * <p>The changes are made through the real services on a real MongoDB. Where a test is about who
 * made each of several changes in one tick, the change events come from the real change streams
 * of MongoDB, subscribed by the services the way the cockpit subscribes them. The other tests hand
 * the change over the way the cockpit's GUI notification services do, without saying who made it.
 * That is what a change event looks like on Azure Cosmos DB, and the update streams then read who
 * made the latest change from the stored entity. Then the filtering tick runs and the event
 * waiting for the browser is read off the stream.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AnUpdateEventNamesItsInitiatorTest {

  private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB).withReplicaSet();

  /** Generous, because a change stream is polled and the poll is answered at most once a second. */
  private static final Duration ARRIVAL = Duration.ofSeconds(30);

  private static final OffsetDateTime STARTED_AT = OffsetDateTime.parse("2026-11-16T08:00:00Z");

  /** Sees every task and every case of the group "sales". */
  private static final Person SAM = Person.of("sam", "sales");

  private static MongoClient mongoClient;

  private static MongoTemplate mongoTemplate;

  private static DefaultMessageListenerContainer listenerContainer;

  private UpdateStreams streams;

  private UserTaskService userTaskService;

  private WorkflowlistService workflowlistService;

  private io.vanillabp.cockpit.tasklist.api.GuiNotificationService userTaskNotifications;

  private io.vanillabp.cockpit.workflowlist.api.GuiNotificationService workflowNotifications;

  /** The notifications the services built from the change events of MongoDB, in their order. */
  private final Queue<NotificationEvent> notificationsFromMongoDb = new ConcurrentLinkedQueue<>();

  @BeforeAll
  static void startMongoDb() {

    MONGODB.start();
    mongoClient = MongoClients.create(MONGODB.getConnectionString());
    mongoTemplate = UpdateStreamFixtures.cockpitLikeTemplate(mongoClient);

    final var executor = new SimpleAsyncTaskExecutor("change-stream-test-");
    executor.setDaemon(true);
    listenerContainer = new DefaultMessageListenerContainer(mongoTemplate, executor);
    listenerContainer.start();

  }

  @AfterAll
  static void stopMongoDb() {

    if (listenerContainer != null) {
      listenerContainer.stop();
    }
    if (mongoClient != null) {
      mongoClient.close();
    }
    MONGODB.stop();

  }

  @BeforeEach
  void theCockpitAsItIsWired() {

    streams = UpdateStreamFixtures.updateStreams(mongoTemplate, mock(TaskScheduler.class), 2_000);
    final ApplicationEventPublisher toTheStreams = event -> streams.collectChange((GuiEvent) event);

    final var repositories = new MongoRepositoryFactory(mongoTemplate);
    final var userContext = mock(UserContext.class);
    when(userContext.getUserLoggedIn()).thenReturn(null);

    userTaskService = new UserTaskService();
    ReflectionTestUtils.setField(userTaskService, "userTasks", repositories.getRepository(UserTaskRepository.class));
    ReflectionTestUtils.setField(userTaskService, "mongoTemplate", mongoTemplate);
    ReflectionTestUtils.setField(userTaskService, "currentUserContext", userContext);

    workflowlistService = new WorkflowlistService();
    ReflectionTestUtils.setField(
        workflowlistService, "workflowRepository", repositories.getRepository(WorkflowRepository.class));
    ReflectionTestUtils.setField(workflowlistService, "mongoTemplate", mongoTemplate);

    userTaskNotifications = new io.vanillabp.cockpit.tasklist.api.GuiNotificationService();
    ReflectionTestUtils.setField(userTaskNotifications, "applicationEventPublisher", toTheStreams);
    workflowNotifications = new io.vanillabp.cockpit.workflowlist.api.GuiNotificationService();
    ReflectionTestUtils.setField(workflowNotifications, "applicationEventPublisher", toTheStreams);

  }

  @AfterEach
  void stopWatchingTheChangeStreams() {

    userTaskService.cleanup();
    workflowlistService.cleanup();

  }

  /**
   * Two people change the same task, and both changes are matched in the same filtering tick.
   * Both are notes, which MongoDB reports as updates without the document. Each event names the
   * person who made its own change.
   */
  @Test
  @DisplayName("Two users change a task in one tick, and each event names its own user")
  void twoUsersChangeATaskInOneTick() {

    final var stream = openedAndReloaded();
    final var taskId = aTaskReportedBy("martin", null);
    watchingTheChangeStreamsOfMongoDb();

    userTaskService.setNote(UserTaskVisibility.everyUserTask(), "petra", taskId, "Waiting for the papers");
    userTaskService.setNote(UserTaskVisibility.everyUserTask(), "martin", taskId, "The papers are here");
    arrivedFromMongoDb(2);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(
        tuple("petra", InitiatorKind.USER),
        tuple("martin", InitiatorKind.USER));

  }

  /**
   * MongoDB leaves a field which keeps its value out of the change event of an update. A user who
   * changes a task twice in a row leaves the initiator as it is, and the second event has to name
   * the user all the same. A change which saves the whole task, the follow-up date set by a job of
   * the cockpit, carries the whole document. Reading the task once per tick would name the cockpit
   * four times here.
   */
  @Test
  @DisplayName("Each of several changes of a task in one tick names its own initiator, even the same user twice in a row")
  void eachChangeOfATaskInOneTickNamesItsOwnInitiator() {

    final var stream = openedAndReloaded();
    final var taskId = aTaskReportedBy(UpdateInformationAware.SYSTEM_USER, "martin");
    watchingTheChangeStreamsOfMongoDb();

    userTaskService.setNote(UserTaskVisibility.everyUserTask(), "martin", taskId, "Not for me");
    userTaskService.unclaimTask(UserTaskVisibility.everyUserTask(), "martin", taskId, "martin");
    // reading a task is no change of it, so it produces no event and changes no initiator
    userTaskService.markAsRead(UserTaskVisibility.everyUserTask(), taskId, "sam");
    userTaskService.setNote(UserTaskVisibility.everyUserTask(), "petra", taskId, "I will ask Martin");
    // no user is signed in, a job of the cockpit for example
    userTaskService.setFollowUpDate(UserTaskVisibility.everyUserTask(), taskId, STARTED_AT.plusDays(1));
    arrivedFromMongoDb(4);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(
        tuple("martin", InitiatorKind.USER),
        tuple("martin", InitiatorKind.USER),
        tuple("petra", InitiatorKind.USER),
        tuple(null, InitiatorKind.COCKPIT));

  }

  /**
   * A note is an update, and a report saves the whole case. In one tick, each event names who made
   * its own change: the user who wrote the note, and the person the workflow module named.
   */
  @Test
  @DisplayName("A note and a report of a case in one tick each name their own initiator")
  void aNoteAndAReportOfACaseInOneTick() {

    final var stream = openedAndReloaded();
    final var caseId = aCaseReportedBy("martin");
    watchingTheChangeStreamsOfMongoDb();

    workflowlistService.setNote(WorkflowVisibility.everyWorkflow(), "petra", caseId, "Call the customer");
    workflowlistService.reportChangedWorkflow(
        caseId, STARTED_AT.plusMinutes(5), () -> null, stored -> stored.setInitiator(person("martin")));
    workflowlistService.setNote(WorkflowVisibility.everyWorkflow(), "sam", caseId, "Called, no answer");
    arrivedFromMongoDb(3);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(
        tuple("petra", InitiatorKind.USER),
        tuple("martin", InitiatorKind.USER),
        tuple("sam", InitiatorKind.USER));

  }

  @Test
  @DisplayName("A note on a case names the user who wrote it, not the initiator the workflow module reported")
  void aNoteOnACaseNamesWhoWroteIt() {

    final var stream = openedAndReloaded();
    final var caseId = aCaseReportedBy("martin");

    workflowlistService.setNote(WorkflowVisibility.everyWorkflow(), "petra", caseId, "Call the customer");
    workflowChanged(caseId);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(tuple("petra", InitiatorKind.USER));

  }

  @Test
  @DisplayName("A report of a case names the person the workflow module named")
  void aReportOfACaseNamesItsPerson() {

    final var stream = openedAndReloaded();
    final var caseId = aCaseReportedBy("martin");

    workflowChanged(caseId);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(tuple("martin", InitiatorKind.USER));

  }

  @Test
  @DisplayName("A report of a case after a note names the report again")
  void aReportAfterANoteNamesTheReport() {

    final var stream = openedAndReloaded();
    final var caseId = aCaseReportedBy("martin");
    workflowlistService.setNote(WorkflowVisibility.everyWorkflow(), "petra", caseId, "Call the customer");

    workflowlistService.reportChangedWorkflow(
        caseId, STARTED_AT.plusMinutes(5), () -> null, stored -> stored.setInitiator(person("martin")));
    workflowChanged(caseId);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(tuple("martin", InitiatorKind.USER));

  }

  @Test
  @DisplayName("A report of the workflow system names no person")
  void aReportOfTheSystemNamesNoPerson() {

    final var stream = openedAndReloaded();
    final var caseId = aCaseReportedBy(UpdateInformationAware.SYSTEM_USER);

    workflowChanged(caseId);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(tuple(null, InitiatorKind.SYSTEM));

  }

  @Test
  @DisplayName("A note on a task names the user who wrote it")
  void aNoteOnATaskNamesWhoWroteIt() {

    final var stream = openedAndReloaded();
    final var taskId = aTaskReportedBy("martin", "martin");

    userTaskService.setNote(UserTaskVisibility.everyUserTask(), "petra", taskId, "Waiting for the papers");
    userTaskChanged(taskId, NotificationEvent.Type.UPDATE);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(tuple("petra", InitiatorKind.USER));

  }

  /**
   * MongoDB reports giving a task back without the changed document, see
   * {@link UserTaskChangedNotification#build}. Here the change is handed over without saying who
   * made it, so the initiator is read in the filtering tick, and it is there all the same.
   */
  @Test
  @DisplayName("Giving a task back names the user who gave it back")
  void givingATaskBackNamesWhoDidIt() {

    final var stream = openedAndReloaded();
    final var taskId = aTaskReportedBy(UpdateInformationAware.SYSTEM_USER, "martin");

    userTaskService.unclaimTask(UserTaskVisibility.everyUserTask(), "martin", taskId, "martin");
    userTaskChanged(taskId, NotificationEvent.Type.UPDATE);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(tuple("martin", InitiatorKind.USER));

  }

  /**
   * Opening a task marks it as read for the person who opened it. That says nothing about the
   * task to anybody else. If it recorded the reader as initiator, the next event of the task
   * would tell every other open page that the reader changed it. The read state produces no event
   * of its own, see {@code ChangeStreamNotificationsTest}.
   */
  @Test
  @DisplayName("Reading a task is no change by the reader, and the next event still names who changed it")
  void readingATaskIsNoChangeByTheReader() {

    final var stream = openedAndReloaded();
    final var taskId = aTaskReportedBy("martin", null);
    final var before = userTaskService.getUserTask(taskId);

    userTaskService.markAsRead(UserTaskVisibility.everyUserTask(), taskId, "petra");
    userTaskService.markAsRead(UserTaskVisibility.everyUserTask(), List.of(taskId), "sam");
    userTaskService.markAsUnread(UserTaskVisibility.everyUserTask(), taskId, "sam");
    final var after = userTaskService.getUserTask(taskId);
    assertThat(after.getInitiator()).isEqualTo("martin");
    assertThat(after.getUpdatedAt()).isEqualTo(before.getUpdatedAt());
    assertThat(after.getUpdatedBy()).isEqualTo(before.getUpdatedBy());
    assertThat(after.getReadAt("petra")).isNotNull();

    userTaskChanged(taskId, NotificationEvent.Type.UPDATE);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(tuple("martin", InitiatorKind.USER));

  }

  @Test
  @DisplayName("A change the cockpit made without a user names the cockpit")
  void aChangeOfTheCockpitNamesTheCockpit() {

    final var stream = openedAndReloaded();
    final var taskId = aTaskReportedBy("martin", null);

    // no user is signed in, a job of the cockpit for example
    userTaskService.setFollowUpDate(UserTaskVisibility.everyUserTask(), taskId, STARTED_AT.plusDays(1));
    userTaskChanged(taskId, NotificationEvent.Type.UPDATE);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(tuple(null, InitiatorKind.COCKPIT));

  }

  @Test
  @DisplayName("A follow-up date reached names the cockpit, not who changed the task before")
  void aFollowUpNamesTheCockpit() {

    final var stream = openedAndReloaded();
    final var taskId = aTaskReportedBy("martin", null);

    userTaskChanged(taskId, NotificationEvent.Type.FOLLOWUP);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(tuple(null, InitiatorKind.COCKPIT));

  }

  /**
   * On Azure Cosmos DB for MongoDB a change event does not say who made the change. The update
   * streams then read who made the latest change from the stored task, which is the best answer
   * left.
   */
  @Test
  @DisplayName("Two changes of a task in one tick which do not say who made them both name who made the latest")
  void twoChangesWhichDoNotSayWhoMadeThemNameTheLatest() {

    final var stream = openedAndReloaded();
    final var taskId = aTaskReportedBy("martin", null);

    userTaskService.setNote(UserTaskVisibility.everyUserTask(), "martin", taskId, "Mine");
    userTaskChanged(taskId, NotificationEvent.Type.UPDATE);
    userTaskService.setNote(UserTaskVisibility.everyUserTask(), "petra", taskId, "Now mine");
    userTaskChanged(taskId, NotificationEvent.Type.UPDATE);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(
        tuple("petra", InitiatorKind.USER),
        tuple("petra", InitiatorKind.USER));

  }

  @Test
  @DisplayName("A change of a task the cockpit no longer holds names nobody")
  void aTaskNoLongerHeldNamesNobody() {

    final var stream = openedAndReloaded();
    final var taskId = aTaskReportedBy("martin", null);
    stream.rememberAnswer(
        UserTaskStreamAudience.KIND_OF_ENTITY, UserTaskVisibility.everyUserTask(), List.of(taskId), 2_000);
    mongoTemplate.remove(new Query(Criteria.where("_id").is(taskId)), userTasks());

    userTaskChanged(taskId, NotificationEvent.Type.DELETE);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsExactly(tuple(null, null));

  }

  @Test
  @DisplayName("A reload names nobody")
  void aReloadNamesNobody() {

    final var stream = subscribe(streams, SAM);
    streams.filterCollectedChanges();

    assertThat(initiatorsOf(stream)).containsOnly(tuple(null, null));

  }

  @Test
  @DisplayName("The browser reads the initiator inside the event, and no field for the system or the cockpit")
  void theBrowserReadsTheInitiatorInsideTheEvent() {

    final var stream = openedAndReloaded();
    final var byPetra = aTaskReportedBy("martin", null);
    userTaskService.setNote(UserTaskVisibility.everyUserTask(), "petra", byPetra, "Done");
    userTaskChanged(byPetra, NotificationEvent.Type.UPDATE);
    final var bySystem = aTaskReportedBy(UpdateInformationAware.SYSTEM_USER, null);
    userTaskChanged(bySystem, NotificationEvent.Type.UPDATE);
    streams.filterCollectedChanges();

    final var mapper = asTheCockpitWritesIt();
    final var json = mapper.readTree(mapper.writeValueAsString(stream.consumeEvents()));

    assertThat(json.get(0).get("event").get("initiator").asString()).isEqualTo("petra");
    assertThat(json.get(0).get("event").get("initiatorKind").asString()).isEqualTo("USER");
    assertThat(json.get(1).get("event").has("initiator")).isFalse();
    assertThat(json.get(1).get("event").get("initiatorKind").asString()).isEqualTo("SYSTEM");

  }

  /** A mapper with the customizers of the cockpit, which leave out what is {@code null}. */
  private static JsonMapper asTheCockpitWritesIt() {

    final var configuration = new JsonConfiguration();
    final var builder = JsonMapper.builder();
    configuration.jsonFormatDateTimes().customize(builder);
    configuration.jsonMinimizeOutput().customize(builder);
    return builder.build();

  }

  /**
   * Lets the services watch the change streams of MongoDB the way the cockpit does. What they
   * notify goes through the GUI notification services to the update streams. A test calls it
   * after it created its task or case, so only its own changes arrive.
   */
  private void watchingTheChangeStreamsOfMongoDb() {

    final var changeStreams = new ChangeStreamUtils();
    ReflectionTestUtils.setField(changeStreams, "properties", new MongoDbProperties());
    ReflectionTestUtils.setField(changeStreams, "messageListenerContainer", listenerContainer);

    ReflectionTestUtils.setField(userTaskService, "changeStreamUtils", changeStreams);
    ReflectionTestUtils.setField(userTaskService, "applicationEventPublisher", (ApplicationEventPublisher) event -> {
      if (event instanceof UserTaskChangedNotification notification) {
        userTaskNotifications.updateClients(notification);
        notificationsFromMongoDb.add(notification);
      }
    });
    ReflectionTestUtils.setField(workflowlistService, "changeStreamUtils", changeStreams);
    ReflectionTestUtils.setField(workflowlistService, "applicationEventPublisher",
        (ApplicationEventPublisher) event -> {
          if (event instanceof WorkflowChangedNotification notification) {
            workflowNotifications.updateClients(notification);
            notificationsFromMongoDb.add(notification);
          }
        });

    userTaskService.subscribeToDbChanges(null);
    workflowlistService.subscribeToDbChanges(null);
    activeWithinTheArrivalTime((Subscription) ReflectionTestUtils.getField(userTaskService, "dbChangesSubscription"));
    activeWithinTheArrivalTime(
        (Subscription) ReflectionTestUtils.getField(workflowlistService, "dbChangesSubscription"));

  }

  private static void activeWithinTheArrivalTime(
      final Subscription subscription) {

    try {
      assertThat(subscription.await(ARRIVAL)).as("change stream active").isTrue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while the change stream started", e);
    }

  }

  /** Waits until so many changes came from MongoDB, so all of them are matched in one tick. */
  private void arrivedFromMongoDb(
      final int changes) {

    await()
        .atMost(ARRIVAL)
        .untilAsserted(() -> assertThat(notificationsFromMongoDb).hasSize(changes));
    // nothing more is on its way, a read state for example, which would make a second tick
    await()
        .during(Duration.ofSeconds(2))
        .atMost(ARRIVAL)
        .untilAsserted(() -> assertThat(notificationsFromMongoDb).hasSize(changes));

  }

  private UpdateEmitter openedAndReloaded() {

    final var stream = subscribe(streams, SAM);
    // the first tick hands every new stream its reload. It is drained here, so a test sees
    // only what its own change caused
    streams.filterCollectedChanges();
    stream.consumeEvents();
    return stream;

  }

  private String aCaseReportedBy(
      final String initiator) {

    final var id = UUID.randomUUID().toString();
    workflowlistService.reportCreatedWorkflow(id, STARTED_AT, () -> {
      final var workflow = new Workflow();
      workflow.setId(id);
      workflow.setCreatedAt(STARTED_AT);
      workflow.setWorkflowModuleId("loans");
      workflow.setBpmnProcessId("loan-approval");
      workflow.setTitle(Map.of("en", "Loan approval"));
      workflow.setInitiator(person(initiator));
      workflow.setAccessibleToGroups(List.of(group("sales")));
      return workflow;
    });
    return id;

  }

  private String aTaskReportedBy(
      final String initiator,
      final String assignee) {

    final var id = UUID.randomUUID().toString();
    userTaskService.reportCreatedUserTask(id, STARTED_AT, () -> {
      final var task = new UserTask();
      task.setId(id);
      task.setCreatedAt(STARTED_AT);
      task.setWorkflowModuleId("loans");
      task.setBpmnProcessId("loan-approval");
      task.setTitle(Map.of("en", "Check the papers"));
      task.setInitiator(initiator);
      task.setCandidateGroups(List.of(group("sales")));
      if (assignee != null) {
        task.setAssignee(person(assignee));
      }
      return task;
    });
    return id;

  }

  private void workflowChanged(
      final String id) {

    workflowNotifications.updateClients(
        new WorkflowChangedNotification(NotificationEvent.Type.UPDATE, id, null));

  }

  private void userTaskChanged(
      final String id,
      final NotificationEvent.Type type) {

    userTaskNotifications.updateClients(new UserTaskChangedNotification(type, id, null));

  }

  /** The initiator and its kind of every event which waits for the browser, in their order. */
  private static List<Tuple> initiatorsOf(
      final UpdateEmitter stream) {

    return stream
        .consumeEvents()
        .stream()
        .map(GuiEvent::getEvent)
        .map(event -> switch (event) {
          case UserTaskEvent task -> tuple(task.getInitiator(), task.getInitiatorKind());
          case WorkflowEvent workflow -> tuple(workflow.getInitiator(), workflow.getInitiatorKind());
          default -> throw new AssertionError("Not an event of the update stream: "
              + event);
        })
        .toList();

  }

  private static io.vanillabp.cockpit.users.model.Person person(
      final String id) {

    final var person = new io.vanillabp.cockpit.users.model.Person();
    person.setId(id);
    return person;

  }

  private static io.vanillabp.cockpit.users.model.Group group(
      final String id) {

    final var group = new io.vanillabp.cockpit.users.model.Group();
    group.setId(id);
    return group;

  }

}
