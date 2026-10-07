package io.vanillabp.cockpit.bpms.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * The report which creates a user task in the cockpit says who sees it, over Kafka.
 * <p>
 * Who sees a task is its assignee, its candidate users, its candidate groups and its excluded
 * candidate users. No report after the one which created the task changes them. The admitted users
 * are the exception, and every report may set them. The cockpit itself still changes the assignee
 * and the candidate users, when somebody claims the task or assigns it to somebody.
 */
@ExtendWith(SuppressOutputExtension.class)
class KafkaTheCreatingReportSaysWhoSeesATaskTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-07T08:00:00Z");
    private static final OffsetDateTime CHANGED_AT = OffsetDateTime.parse("2026-10-07T09:00:00Z");
    private static final OffsetDateTime ENDED_AT = OffsetDateTime.parse("2026-10-07T10:00:00Z");

    private final Map<String, UserTask> storedTasks = new HashMap<>();

    private KafkaUserTaskController userTasks;

    @BeforeEach
    void setUp() throws Exception {

        final var personAndGroupMapper = mock(PersonAndGroupMapper.class);
        when(personAndGroupMapper.toModelPerson(anyString())).thenAnswer(invocation -> {
            final var person = new Person();
            person.setId(invocation.getArgument(0));
            return person;
        });
        when(personAndGroupMapper.toModelGroup(anyString())).thenAnswer(invocation -> {
            final var group = new Group();
            group.setId(invocation.getArgument(0));
            return group;
        });
        final var userTaskMapper = new ProtobufUserTaskMapperImpl();
        set(ProtobufUserTaskMapper.class, userTaskMapper, "personAndGroupMapper", personAndGroupMapper);

        final var repository = mock(UserTaskRepository.class);
        when(repository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(storedTasks.get(invocation.getArgument(0))));
        when(repository.save(any(UserTask.class))).thenAnswer(invocation -> {
            final UserTask task = invocation.getArgument(0);
            storedTasks.put(task.getId(), task);
            return task;
        });
        final var userTaskService = new UserTaskService();
        set(UserTaskService.class, userTaskService, "userTasks", repository);
        set(UserTaskService.class, userTaskService, "logger", mock(Logger.class));
        set(UserTaskService.class, userTaskService, "mongoTemplate", mock(MongoTemplate.class));

        userTasks = new KafkaUserTaskController(userTaskService, userTaskMapper);

    }

    private static void set(
            final Class<?> declaringClass,
            final Object target,
            final String field,
            final Object value) throws Exception {

        final var declaredField = declaringClass.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(target, value);

    }

    private void consume(
            final BcEvent event) {

        userTasks.consumeUserTaskEvent(
                new ConsumerRecord<>("topic", 0, 0, "key", event.toByteArray()));

    }

    private UserTask storedTask() {

        return storedTasks.get("task-1");

    }

    // --- what a report looks like -------------------------------------------------------------

    private static Timestamp protobuf(
            final OffsetDateTime timestamp) {

        return Timestamp
                .newBuilder()
                .setSeconds(timestamp.toEpochSecond())
                .setNanos(timestamp.getNano())
                .build();

    }

    private static UserTaskCreatedOrUpdatedEvent.Builder reported(
            final OffsetDateTime timestamp) {

        return UserTaskCreatedOrUpdatedEvent
                .newBuilder()
                .setId("event-" + timestamp)
                .setUserTaskId("task-1")
                .setTimestamp(protobuf(timestamp))
                .setBpmnProcessId("ride")
                .setTaskDefinition("assign-driver")
                .putTitle("en", "Assign a driver at " + timestamp)
                .setUiUriPath("/ui")
                .setUiUriType("WEBPACK_MF_REACT");

    }

    private static BcEvent createdForAnna() {

        return BcEvent
                .newBuilder()
                .setUserTaskCreatedV11(reported(CREATED_AT)
                        .setAssignee("anna")
                        .addCandidateUsers("bert")
                        .addCandidateGroups("drivers")
                        .addExcludedCandidateUsers("carl")
                        .addAdmittedUsers("dora"))
                .build();

    }

    private static UserTaskCreatedOrUpdatedEvent.Builder forEve(
            final OffsetDateTime timestamp) {

        return reported(timestamp)
                .setUpdated(true)
                .setAssignee("eve")
                .addCandidateUsers("frank")
                .addCandidateGroups("dispatchers")
                .addExcludedCandidateUsers("anna")
                .addAdmittedUsers("gina");

    }

    private static BcEvent changedForEve() {

        return BcEvent
                .newBuilder()
                .setUserTaskUpdatedV11(forEve(CHANGED_AT))
                .build();

    }

    private static BcEvent completedForEve() {

        return BcEvent
                .newBuilder()
                .setUserTaskCompletedV11(forEve(ENDED_AT))
                .build();

    }

    private static BcEvent cancelledForEve() {

        return BcEvent
                .newBuilder()
                .setUserTaskCancelledV11(forEve(ENDED_AT))
                .build();

    }

    private static void assertSeenAsCreatedForAnna(
            final UserTask task) {

        assertEquals("anna", task.getAssignee().getId());
        assertEquals(List.of("bert"), task.getCandidateUsers().stream().map(Person::getId).toList());
        assertEquals(List.of("drivers"), task.getCandidateGroups().stream().map(Group::getId).toList());
        assertEquals(List.of("carl"), task.getExcludedCandidateUsers().stream().map(Person::getId).toList());

    }

    private static void assertSeenAsReportedForEve(
            final UserTask task) {

        assertEquals("eve", task.getAssignee().getId());
        assertEquals(List.of("frank"), task.getCandidateUsers().stream().map(Person::getId).toList());
        assertEquals(List.of("dispatchers"), task.getCandidateGroups().stream().map(Group::getId).toList());
        assertEquals(List.of("anna"), task.getExcludedCandidateUsers().stream().map(Person::getId).toList());

    }

    private static List<String> admittedUsersOf(
            final UserTask task) {

        return task.getAdmittedUsers().stream().map(Person::getId).toList();

    }

    // --- a report about a task the cockpit holds ----------------------------------------------

    @Test
    void aChangeLeavesWhoSeesTheTaskAsTheCreationSaid() {

        consume(createdForAnna());
        consume(changedForEve());

        assertSeenAsCreatedForAnna(storedTask());
        assertEquals(Map.of("en", "Assign a driver at " + CHANGED_AT), storedTask().getTitle());
        assertEquals(List.of("gina"), admittedUsersOf(storedTask()));

    }

    @Test
    void aChangeWhichNamesNobodyDoesNotTakeTheTaskAwayFromItsAssignee() {

        consume(createdForAnna());
        consume(BcEvent
                .newBuilder()
                .setUserTaskUpdatedV11(reported(CHANGED_AT).setUpdated(true))
                .build());

        assertSeenAsCreatedForAnna(storedTask());
        // a change replaces the admitted users, with nobody as well
        assertEquals(List.of(), admittedUsersOf(storedTask()));

    }

    @Test
    void aCompletionLeavesWhoSeesTheTaskAsTheCreationSaid() {

        consume(createdForAnna());
        consume(completedForEve());

        assertSeenAsCreatedForAnna(storedTask());
        assertEquals(List.of("gina"), admittedUsersOf(storedTask()));

    }

    @Test
    void aCancellationLeavesWhoSeesTheTaskAsTheCreationSaid() {

        consume(createdForAnna());
        consume(cancelledForEve());

        assertSeenAsCreatedForAnna(storedTask());
        assertEquals(List.of("gina"), admittedUsersOf(storedTask()));

    }

    @Test
    void anEndWhichAdmitsNobodyKeepsTheAdmittedUsers() {

        consume(createdForAnna());
        consume(BcEvent
                .newBuilder()
                .setUserTaskCompletedV11(reported(ENDED_AT).setUpdated(true))
                .build());

        assertSeenAsCreatedForAnna(storedTask());
        assertEquals(List.of("dora"), admittedUsersOf(storedTask()));

    }

    // --- the cockpit itself -------------------------------------------------------------------

    /**
     * A claim and an assignment in the cockpit change the assignee and the candidate users, as
     * {@code UserTaskService.claimTask} and {@code assignTask} do. No report undoes them.
     */
    @Test
    void whatTheCockpitChangedSurvivesAChangeAndAnEnd() {

        consume(createdForAnna());
        final var petra = new Person();
        petra.setId("petra");
        storedTask().setAssignee(petra);
        final var hans = new Person();
        hans.setId("hans");
        storedTask().addCandidatePerson(hans);

        consume(changedForEve());
        consume(completedForEve());

        assertEquals("petra", storedTask().getAssignee().getId());
        assertEquals(
                List.of("bert", "hans"),
                storedTask().getCandidateUsers().stream().map(Person::getId).toList());

    }

    // --- the report which creates the task ----------------------------------------------------

    @Test
    void aChangeWhichCreatesTheTaskSaysWhoSeesIt() {

        consume(changedForEve());
        consume(createdForAnna());

        assertSeenAsReportedForEve(storedTask());
        assertEquals(CREATED_AT.toInstant(), storedTask().getCreatedAt().toInstant());

    }

    @Test
    void anEndWhichCreatesTheTaskSaysWhoSeesIt() {

        consume(completedForEve());
        assertSeenAsReportedForEve(storedTask());

        consume(createdForAnna());

        assertSeenAsReportedForEve(storedTask());

    }

}
