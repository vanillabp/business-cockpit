package io.vanillabp.cockpit.gui.api.v1;

import static io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.subscribe;
import static io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.userTasks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
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
 * <p>The changes are made through the real services on a real MongoDB. The change itself reaches
 * the update streams the way the cockpit's GUI notification services hand it over, because a
 * test does not run the change streams of MongoDB. Then the filtering tick runs and the event
 * waiting for the browser is read off the stream.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AnUpdateEventNamesItsInitiatorTest {

  private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

  private static final OffsetDateTime STARTED_AT = OffsetDateTime.parse("2026-11-16T08:00:00Z");

  /** Sees every task and every case of the group "sales". */
  private static final Person SAM = Person.of("sam", "sales");

  private static MongoClient mongoClient;

  private static MongoTemplate mongoTemplate;

  private UpdateStreams streams;

  private UserTaskService userTaskService;

  private WorkflowlistService workflowlistService;

  private io.vanillabp.cockpit.tasklist.api.GuiNotificationService userTaskNotifications;

  private io.vanillabp.cockpit.workflowlist.api.GuiNotificationService workflowNotifications;

  @BeforeAll
  static void startMongoDb() {

    MONGODB.start();
    mongoClient = MongoClients.create(MONGODB.getConnectionString());
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
   * {@link UserTaskChangedNotification#build}. The initiator is read in the filtering tick, so it
   * is there all the same.
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

  @Test
  @DisplayName("Two changes of a task in one tick both name who made the latest")
  void twoChangesInOneTickNameTheLatest() {

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
