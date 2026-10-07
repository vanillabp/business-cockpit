package io.vanillabp.cockpit.bpms.api.v1_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;

/**
 * The report which creates a user task in the cockpit says who sees it, over REST version 1.1.
 * <p>
 * Who sees a task is its assignee, its candidate users, its candidate groups and its excluded
 * candidate users. No report after the one which created the task changes them. The admitted users
 * are the exception, and every report may set them. The cockpit itself still changes the assignee
 * and the candidate users, when somebody claims the task or assigns it to somebody.
 * <p>
 * The tests drive the controller with the generated mapper and the real service, and hold the
 * stored tasks in memory.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheCreatingReportSaysWhoSeesATaskTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-07T08:00:00Z");
    private static final OffsetDateTime CHANGED_AT = OffsetDateTime.parse("2026-10-07T09:00:00Z");
    private static final OffsetDateTime ENDED_AT = OffsetDateTime.parse("2026-10-07T10:00:00Z");

    private final Map<String, UserTask> userTasks = new HashMap<>();

    private BpmsApiController bpmsApi;

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
        final var userTaskMapper = new UserTaskMapperV1_1Impl();
        set(UserTaskMapper.class, userTaskMapper, "personAndGroupMapper", personAndGroupMapper);

        final var repository = mock(UserTaskRepository.class);
        when(repository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(userTasks.get(invocation.getArgument(0))));
        when(repository.save(any(UserTask.class))).thenAnswer(invocation -> {
            final UserTask task = invocation.getArgument(0);
            userTasks.put(task.getId(), task);
            return task;
        });
        final var userTaskService = new UserTaskService();
        set(UserTaskService.class, userTaskService, "userTasks", repository);
        set(UserTaskService.class, userTaskService, "logger", mock(Logger.class));
        set(UserTaskService.class, userTaskService, "mongoTemplate", mock(MongoTemplate.class));

        bpmsApi = new BpmsApiController();
        set(BpmsApiController.class, bpmsApi, "userTaskMapper", userTaskMapper);
        set(BpmsApiController.class, bpmsApi, "workflowMapper", new WorkflowMapperV1_1Impl());
        set(BpmsApiController.class, bpmsApi, "userTaskService", userTaskService);
        set(BpmsApiController.class, bpmsApi, "workflowlistService", mock(WorkflowlistService.class));
        set(BpmsApiController.class, bpmsApi, "workflowModuleService", mock(WorkflowModuleService.class));

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

    private UserTask storedTask() {

        return userTasks.get("task-1");

    }

    // --- what a report looks like -------------------------------------------------------------

    private static UserTaskCreatedEvent createdForAnna() {

        return new UserTaskCreatedEvent()
                .id("event-created")
                .userTaskId("task-1")
                .timestamp(CREATED_AT)
                .bpmnProcessId("ride")
                .taskDefinition("assign-driver")
                .title(Map.of("en", "Assign a driver"))
                .uiUriPath("/ui")
                .uiUriType("WEBPACK_MF_REACT")
                .assignee("anna")
                .candidateUsers(List.of("bert"))
                .candidateGroups(List.of("drivers"))
                .excludedCandidateUsers(List.of("carl"))
                .admittedUsers(List.of("dora"));

    }

    private static UserTaskUpdatedEvent changedForEve() {

        return new UserTaskUpdatedEvent()
                .id("event-changed")
                .userTaskId("task-1")
                .timestamp(CHANGED_AT)
                .bpmnProcessId("ride")
                .taskDefinition("assign-driver")
                .title(Map.of("en", "Assign a driver now"))
                .uiUriPath("/ui")
                .uiUriType("WEBPACK_MF_REACT")
                .assignee("eve")
                .candidateUsers(List.of("frank"))
                .candidateGroups(List.of("dispatchers"))
                .excludedCandidateUsers(List.of("anna"))
                .admittedUsers(List.of("gina"));

    }

    private static UserTaskCompletedEvent completedForEve() {

        return new UserTaskCompletedEvent()
                .id("event-completed")
                .userTaskId("task-1")
                .timestamp(ENDED_AT)
                .bpmnProcessId("ride")
                .taskDefinition("assign-driver")
                .title(Map.of("en", "Assign a driver"))
                .uiUriPath("/ui")
                .uiUriType("WEBPACK_MF_REACT")
                .assignee("eve")
                .candidateUsers(List.of("frank"))
                .candidateGroups(List.of("dispatchers"))
                .excludedCandidateUsers(List.of("anna"))
                .admittedUsers(List.of("gina"));

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

        bpmsApi.userTaskCreatedEvent(createdForAnna());
        final var answer = bpmsApi.userTaskUpdatedEvent("task-1", changedForEve());

        assertEquals(HttpStatus.OK, answer.getStatusCode());
        assertSeenAsCreatedForAnna(storedTask());
        // everything else of the change is stored
        assertEquals(Map.of("en", "Assign a driver now"), storedTask().getTitle());

    }

    @Test
    void aChangeSetsTheAdmittedUsers() {

        bpmsApi.userTaskCreatedEvent(createdForAnna());
        bpmsApi.userTaskUpdatedEvent("task-1", changedForEve());

        assertEquals(List.of("gina"), admittedUsersOf(storedTask()));

    }

    @Test
    void aChangeWhichNamesNobodyDoesNotTakeTheTaskAwayFromItsAssignee() {

        bpmsApi.userTaskCreatedEvent(createdForAnna());
        bpmsApi.userTaskUpdatedEvent("task-1", changedForEve()
                .assignee(null)
                .candidateUsers(List.of())
                .candidateGroups(List.of())
                .excludedCandidateUsers(List.of()));

        assertSeenAsCreatedForAnna(storedTask());

    }

    @Test
    void anEndLeavesWhoSeesTheTaskAsTheCreationSaid() {

        bpmsApi.userTaskCreatedEvent(createdForAnna());
        bpmsApi.userTaskCompletedEvent("task-1", completedForEve());

        assertSeenAsCreatedForAnna(storedTask());
        assertEquals(List.of("gina"), admittedUsersOf(storedTask()));

    }

    @Test
    void anEndWhichAdmitsNobodyKeepsTheAdmittedUsers() {

        bpmsApi.userTaskCreatedEvent(createdForAnna());
        bpmsApi.userTaskCompletedEvent("task-1", completedForEve().admittedUsers(List.of()));

        assertEquals(List.of("dora"), admittedUsersOf(storedTask()));

    }

    // --- the cockpit itself -------------------------------------------------------------------

    /**
     * A claim and an assignment in the cockpit change the assignee and the candidate users, as
     * {@code UserTaskService.claimTask} and {@code assignTask} do. No report undoes them.
     */
    @Test
    void whatTheCockpitChangedSurvivesAChange() {

        bpmsApi.userTaskCreatedEvent(createdForAnna());
        final var petra = new Person();
        petra.setId("petra");
        storedTask().setAssignee(petra);
        final var hans = new Person();
        hans.setId("hans");
        storedTask().addCandidatePerson(hans);

        bpmsApi.userTaskUpdatedEvent("task-1", changedForEve());

        assertEquals("petra", storedTask().getAssignee().getId());
        assertEquals(
                List.of("bert", "hans"),
                storedTask().getCandidateUsers().stream().map(Person::getId).toList());

    }

    // --- the report which creates the task ----------------------------------------------------

    @Test
    void aChangeWhichCreatesTheTaskSaysWhoSeesIt() {

        bpmsApi.userTaskUpdatedEvent("task-1", changedForEve());

        assertSeenAsReportedForEve(storedTask());
        assertEquals(List.of("gina"), admittedUsersOf(storedTask()));

    }

    @Test
    void aCreationAfterTheChangeWhichCreatedTheTaskChangesNothingAboutWhoSeesIt() {

        bpmsApi.userTaskUpdatedEvent("task-1", changedForEve());
        bpmsApi.userTaskCreatedEvent(createdForAnna());

        assertSeenAsReportedForEve(storedTask());
        // and still takes the earlier start
        assertEquals(CREATED_AT, storedTask().getCreatedAt());

    }

    @Test
    void anEndWhichCreatesTheTaskSaysWhoSeesIt() {

        bpmsApi.userTaskCompletedEvent("task-1", completedForEve());

        assertSeenAsReportedForEve(storedTask());
        assertEquals(List.of("gina"), admittedUsersOf(storedTask()));

    }

    @Test
    void aCreationAfterTheEndWhichCreatedTheTaskChangesNothingAboutWhoSeesIt() {

        bpmsApi.userTaskCompletedEvent("task-1", completedForEve());
        bpmsApi.userTaskCreatedEvent(createdForAnna());

        assertSeenAsReportedForEve(storedTask());
        // the creation still fills in the rest
        assertEquals(CREATED_AT, storedTask().getCreatedAt());

    }

    /**
     * A BPMS which no longer knows the task reports its end with little more than the identifiers.
     * Such an end said nothing about who sees the task, so the creation fills it in, as it fills in
     * everything else the end could not report.
     */
    @Test
    void aCreationSaysWhoSeesATaskWhoseEndNamedNobody() {

        bpmsApi.userTaskCompletedEvent("task-1", completedForEve()
                .assignee(null)
                .candidateUsers(List.of())
                .candidateGroups(List.of())
                .excludedCandidateUsers(List.of()));
        assertNull(storedTask().getAssignee());

        bpmsApi.userTaskCreatedEvent(createdForAnna());

        assertSeenAsCreatedForAnna(storedTask());

    }

}
