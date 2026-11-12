package io.vanillabp.cockpit.tasklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.AdditionalAnswers;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.mongodb.MongoDBContainer;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.vanillabp.cockpit.mongo.converters.OffsetDateTimeReadConverter;
import io.vanillabp.cockpit.mongo.converters.OffsetDateTimeWriteConverter;
import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A user changes a task in the cockpit while a report of the workflow module about the same task
 * is stored. A report is stored by reading the task, laying the report onto it and saving it whole.
 * Neither of the two may get lost.
 * <p>
 * The race is played on a real MongoDB, through the real {@link UserTaskService}. The user's
 * change runs exactly between the read and the save of the other side, so the outcome is the same
 * on every run.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AChangeInTheGuiAndAReportAtTheSameTimeTest {

  private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-11-01T08:00:00Z");

  private static final String TASK = "task-1";

  private static final String MARTIN = "martin";

  private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB);

  private static MongoClient mongoClient;

  private UserTaskService service;

  /** Runs once, right before the next save of a task, and is cleared then. */
  private final AtomicReference<Runnable> beforeTheNextSave = new AtomicReference<>();

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
  void aTaskAssignedToMartin() {

    final var factory = new SimpleMongoClientDatabaseFactory(mongoClient, "cockpit-"
        + UUID.randomUUID());
    final var conversions = new MongoCustomConversions(List.of(
        new OffsetDateTimeReadConverter(),
        new OffsetDateTimeWriteConverter()));
    final var mappingContext = new MongoMappingContext();
    mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
    mappingContext.afterPropertiesSet();
    final var converter = new MappingMongoConverter(new DefaultDbRefResolver(factory), mappingContext);
    converter.setCustomConversions(conversions);
    converter.afterPropertiesSet();
    final var mongoTemplate = new MongoTemplate(factory, converter);

    final var stored = new MongoRepositoryFactory(mongoTemplate).getRepository(UserTaskRepository.class);
    final var userTasks = mock(UserTaskRepository.class, AdditionalAnswers.delegatesTo(stored));
    doAnswer(invocation -> {
      runWhatComesBeforeTheNextSave();
      return stored.save(invocation.<UserTask>getArgument(0));
    }).when(userTasks).save(any(UserTask.class));
    doAnswer(invocation -> {
      runWhatComesBeforeTheNextSave();
      return stored.saveAll(invocation.<Iterable<UserTask>>getArgument(0));
    }).when(userTasks).saveAll(any());

    final var userContext = mock(UserContext.class);
    when(userContext.getUserLoggedIn()).thenReturn(MARTIN);

    service = new UserTaskService();
    ReflectionTestUtils.setField(service, "userTasks", userTasks);
    ReflectionTestUtils.setField(service, "mongoTemplate", mongoTemplate);
    ReflectionTestUtils.setField(service, "currentUserContext", userContext);

    final var outcome = service.reportCreatedUserTask(TASK, CREATED_AT, () -> {
      final var task = new UserTask();
      task.setId(TASK);
      task.setCreatedAt(CREATED_AT);
      task.setWorkflowModuleId("taxi-ride");
      task.setBpmnProcessId("ride");
      task.setTitle(Map.of("en", "Assign a driver"));
      task.setAssignee(person(MARTIN));
      return task;
    });
    assertThat(outcome.isUpToDate()).isTrue();

  }

  private void runWhatComesBeforeTheNextSave() {

    final var hook = beforeTheNextSave.getAndSet(null);
    if (hook != null) {
      hook.run();
    }

  }

  /**
   * A change the GUI API makes, and how to see afterwards that it was made.
   *
   * @param prepare What has to be there before, such as a candidate who is removed
   * @param make The change
   * @param isThere Asserts that the change is stored
   * @param readsAndSavesTheTask Whether the change reads the task and saves it whole. The other
   *        changes are one atomic update, which reads nothing a report could write in between.
   */
  private record ChangeInTheGui(
                                Consumer<UserTaskService> prepare,
                                Consumer<UserTaskService> make,
                                Consumer<UserTask> isThere,
                                boolean readsAndSavesTheTask) {
  }

  private static final Consumer<UserTaskService> NOTHING = service -> {
  };

  private static final UserTaskVisibility EVERY_TASK = UserTaskVisibility.everyUserTask();

  private static final OffsetDateTime FOLLOW_UP = OffsetDateTime.parse("2026-12-01T09:00:00Z");

  private static final String NOTE = "Customer calls back on Monday";

  static Stream<Arguments> everyChangeOfTheGuiApi() {

    return Stream.concat(theChangesWhichSaveTheTask(), Stream.of(
        change("giving the task back", new ChangeInTheGui(
            NOTHING, service -> service.unclaimTask(EVERY_TASK, MARTIN, TASK,
                MARTIN), task -> assertThat(task.getAssignee()).isNull(), false)),
        change("giving several tasks back", new ChangeInTheGui(
            NOTHING, service -> service.unclaimTask(EVERY_TASK, MARTIN, List.of(TASK),
                MARTIN), task -> assertThat(task.getAssignee()).isNull(), false)),
        change("writing the note", new ChangeInTheGui(
            NOTHING, service -> service.setNote(EVERY_TASK, MARTIN, TASK, NOTE), task -> assertThat(task.getNote())
                .isEqualTo(NOTE), false)),
        change("writing the note of several tasks", new ChangeInTheGui(
            NOTHING, service -> service.setNote(EVERY_TASK, MARTIN, List.of(TASK),
                NOTE), task -> assertThat(task.getNote()).isEqualTo(NOTE), false))));

  }

  static Stream<Arguments> theChangesWhichSaveTheTask() {

    final Consumer<UserTaskService> petraIsACandidate = service -> service.assignTask(EVERY_TASK, TASK,
        person("petra"));
    final Consumer<UserTaskService> martinReadIt = service -> service.markAsRead(EVERY_TASK, TASK, MARTIN);
    return Stream.of(
        change("taking the task", new ChangeInTheGui(
            NOTHING, service -> service.claimTask(EVERY_TASK, TASK,
                person("petra")), task -> assertThat(task.getAssignee().getId()).isEqualTo("petra"), true)),
        change("taking several tasks", new ChangeInTheGui(
            NOTHING, service -> service.claimTask(EVERY_TASK, List.of(TASK),
                person("petra")), task -> assertThat(task.getAssignee().getId()).isEqualTo("petra"), true)),
        change("adding a candidate", new ChangeInTheGui(
            NOTHING, petraIsACandidate, task -> assertThat(task.getCandidateUsers()).extracting(Person::getId)
                .contains("petra"), true)),
        change("adding a candidate to several tasks", new ChangeInTheGui(
            NOTHING, service -> service.assignTask(EVERY_TASK, List.of(TASK),
                person("petra")), task -> assertThat(task.getCandidateUsers()).extracting(Person::getId)
                    .contains("petra"), true)),
        change("removing a candidate", new ChangeInTheGui(
            petraIsACandidate, service -> service.unassignTask(EVERY_TASK, TASK,
                "petra"), task -> assertThat(task.getCandidateUsers()).extracting(Person::getId)
                    .doesNotContain("petra"), true)),
        change("removing a candidate from several tasks", new ChangeInTheGui(
            petraIsACandidate, service -> service.unassignTask(EVERY_TASK, List.of(TASK),
                "petra"), task -> assertThat(task.getCandidateUsers()).extracting(Person::getId)
                    .doesNotContain("petra"), true)),
        change("marking the task as read", new ChangeInTheGui(
            NOTHING, martinReadIt, task -> assertThat(task.getReadAt(MARTIN)).isNotNull(), true)),
        change("marking several tasks as read", new ChangeInTheGui(
            NOTHING, service -> service.markAsRead(EVERY_TASK, List.of(TASK),
                MARTIN), task -> assertThat(task.getReadAt(MARTIN)).isNotNull(), true)),
        change("marking the task as unread", new ChangeInTheGui(
            martinReadIt, service -> service.markAsUnread(EVERY_TASK, TASK,
                MARTIN), task -> assertThat(task.getReadAt(MARTIN)).isNull(), true)),
        change("marking several tasks as unread", new ChangeInTheGui(
            martinReadIt, service -> service.markAsUnread(EVERY_TASK, List.of(TASK),
                MARTIN), task -> assertThat(task.getReadAt(MARTIN)).isNull(), true)),
        change("setting a follow-up date", new ChangeInTheGui(
            NOTHING, service -> service.setFollowUpDate(EVERY_TASK, TASK,
                FOLLOW_UP), task -> assertThat(task.getFollowUpDate()).isAtSameInstantAs(FOLLOW_UP), true)));

  }

  private static Arguments change(
      final String name,
      final ChangeInTheGui change) {

    return Arguments.of(Named.of(name, change));

  }

  /**
   * The report read the task before the user changed it. Its save has to fail the version check,
   * or it writes the task back as it was before the change. The workflow module sends the report
   * again later, and then it goes through on top of the change.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("everyChangeOfTheGuiApi")
  void aReportWhichReadTheTaskBeforeTheChangeIsRefused(
      final ChangeInTheGui change) {

    change.prepare().accept(service);
    final var refused = service.reportChangedUserTask(
        TASK,
        CREATED_AT.plusMinutes(1),
        () -> null,
        // runs after the report read the task and before it saves it
        task -> {
          change.make().accept(service);
          task.setComment("reported");
        });

    assertThat(refused.isUpToDate()).isFalse();
    assertThat(refused.failure()).isInstanceOf(OptimisticLockingFailureException.class);
    assertThat(refused.failsEveryTime()).isFalse();
    change.isThere().accept(service.getUserTask(TASK));

    final var again = service.reportChangedUserTask(
        TASK,
        CREATED_AT.plusMinutes(1),
        () -> null,
        task -> task.setComment("reported"));

    assertThat(again.isUpToDate()).isTrue();
    final var stored = service.getUserTask(TASK);
    change.isThere().accept(stored);
    assertThat(stored.getComment()).isEqualTo("reported");

  }

  /**
   * The other way round: the user's change read the task, and a report was stored before the
   * change is saved. The user's change is read and made again, so the user gets no error and the
   * report stays.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("theChangesWhichSaveTheTask")
  void aChangeWhichReadTheTaskBeforeAReportIsMadeAgain(
      final ChangeInTheGui change) {

    assertThat(change.readsAndSavesTheTask()).isTrue();
    change.prepare().accept(service);
    beforeTheNextSave.set(() -> assertThat(service.reportChangedUserTask(
        TASK,
        CREATED_AT.plusMinutes(1),
        () -> null,
        task -> task.setComment("reported")).isUpToDate()).isTrue());

    change.make().accept(service);

    assertThat(beforeTheNextSave.get()).as("the report came in between").isNull();
    final var stored = service.getUserTask(TASK);
    change.isThere().accept(stored);
    assertThat(stored.getComment()).isEqualTo("reported");

  }

  /** The positive twin of the two above: a change with nothing in between. */
  @Test
  void aTaskGivenBackWithNothingInBetweenHasNoAssignee() {

    final var before = service.getUserTask(TASK).getVersion();

    service.unclaimTask(UserTaskVisibility.everyUserTask(), MARTIN, TASK, MARTIN);

    final var stored = service.getUserTask(TASK);
    assertThat(stored.getAssignee()).isNull();
    assertThat(stored.getVersion()).isEqualTo(before + 1);

  }

  private static Person person(
      final String id) {

    final var person = new Person();
    person.setId(id);
    return person;

  }

}
