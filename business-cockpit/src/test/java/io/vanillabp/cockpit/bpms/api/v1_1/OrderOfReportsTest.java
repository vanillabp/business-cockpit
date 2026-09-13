package io.vanillabp.cockpit.bpms.api.v1_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskEndReason;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * What the cockpit stores when the reports of one user task or one case do not arrive in the order
 * they happened in.
 * <p>
 * The outbox of a workflow module gives its entries no order: it dispatches them in parallel and
 * repeats a failed one after the entries planned later have gone through. So a completion can reach
 * the cockpit before the creation it completes, and a change can reach it after the change which
 * followed it. Every test here drives the REST ingress of the current API version, because the guard
 * has to hold for what a mapper produces and not only for what a service is handed.
 */
@ExtendWith(SuppressOutputExtension.class)
class OrderOfReportsTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-09-11T08:00:00Z");
    private static final OffsetDateTime CHANGED_AT = OffsetDateTime.parse("2026-09-11T09:00:00Z");
    private static final OffsetDateTime ENDED_AT = OffsetDateTime.parse("2026-09-11T10:00:00Z");
    private static final OffsetDateTime CHANGED_AFTER_THE_END = OffsetDateTime.parse("2026-09-11T11:00:00Z");
    private static final OffsetDateTime DUE_AT = OffsetDateTime.parse("2026-09-12T08:00:00Z");

    /**
     * One cockpit server, with the two lists it keeps held in memory instead of in MongoDB. The
     * mappers are the generated ones and the services are the real ones, so a test says what an
     * application reporting over REST would get.
     */
    private static final class Cockpit {

        private final Map<String, UserTask> userTasks = new HashMap<>();

        private final Map<String, Workflow> workflows = new HashMap<>();

        private final Logger logger = mock(Logger.class);

        private final BpmsApiController bpmsApi;

        private Cockpit() {
            try {
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
                final var workflowMapper = new WorkflowMapperV1_1Impl();
                set(WorkflowMapper.class, workflowMapper, "personAndGroupMapper", personAndGroupMapper);

                final var userTaskService = new UserTaskService();
                set(UserTaskService.class, userTaskService, "userTasks", userTaskRepository());
                set(UserTaskService.class, userTaskService, "logger", logger);
                set(UserTaskService.class, userTaskService, "mongoTemplate", mock(MongoTemplate.class));

                final var workflowlistService = new WorkflowlistService();
                set(WorkflowlistService.class, workflowlistService, "workflowRepository", workflowRepository());
                set(WorkflowlistService.class, workflowlistService, "logger", logger);
                set(WorkflowlistService.class, workflowlistService, "mongoTemplate", mock(MongoTemplate.class));

                bpmsApi = new BpmsApiController();
                set(BpmsApiController.class, bpmsApi, "userTaskMapper", userTaskMapper);
                set(BpmsApiController.class, bpmsApi, "workflowMapper", workflowMapper);
                set(BpmsApiController.class, bpmsApi, "userTaskService", userTaskService);
                set(BpmsApiController.class, bpmsApi, "workflowlistService", workflowlistService);
                set(BpmsApiController.class, bpmsApi, "workflowModuleService", mock(WorkflowModuleService.class));
            } catch (Exception e) {
                throw new IllegalStateException("Could not build the cockpit under test", e);
            }
        }

        private UserTaskRepository userTaskRepository() {
            final var repository = mock(UserTaskRepository.class);
            when(repository.findById(anyString()))
                    .thenAnswer(invocation -> Optional.ofNullable(userTasks.get(invocation.getArgument(0))));
            when(repository.save(any(UserTask.class))).thenAnswer(invocation -> {
                final UserTask task = invocation.getArgument(0);
                userTasks.put(task.getId(), task);
                return task;
            });
            return repository;
        }

        private WorkflowRepository workflowRepository() {
            final var repository = mock(WorkflowRepository.class);
            when(repository.findById(anyString()))
                    .thenAnswer(invocation -> Optional.ofNullable(workflows.get(invocation.getArgument(0))));
            when(repository.save(any(Workflow.class))).thenAnswer(invocation -> {
                final Workflow workflow = invocation.getArgument(0);
                workflows.put(workflow.getId(), workflow);
                return workflow;
            });
            return repository;
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

        private Workflow storedWorkflow() {
            return workflows.get("workflow-1");
        }

    }

    private static Cockpit cockpitWhichWasSent(
            final Consumer<BpmsApiController> reports) {

        final var cockpit = new Cockpit();
        reports.accept(cockpit.bpmsApi);
        return cockpit;

    }

    // --- what a report looks like -------------------------------------------------------------

    private static UserTaskCreatedEvent taskCreated() {

        return new UserTaskCreatedEvent()
                .id("event-created")
                .userTaskId("task-1")
                .timestamp(CREATED_AT)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .workflowId("workflow-1")
                .taskDefinition("assign-driver")
                .title(Map.of("en", "Assign a driver"))
                .uiUriPath("/ui")
                .uiUriType(UiUriType.WEBPACK_MF_REACT)
                .dueDate(DUE_AT)
                .details(new LinkedHashMap<>(Map.of("customer", "Anna")))
                .detailsFulltextSearch("Anna");

    }

    private static UserTaskUpdatedEvent taskChanged(
            final OffsetDateTime timestamp,
            final String customer) {

        return new UserTaskUpdatedEvent()
                .id("event-changed-" + customer)
                .userTaskId("task-1")
                .timestamp(timestamp)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .workflowId("workflow-1")
                .taskDefinition("assign-driver")
                .title(Map.of("en", "Assign a driver"))
                .uiUriPath("/ui")
                .uiUriType(UiUriType.WEBPACK_MF_REACT)
                .details(new LinkedHashMap<>(Map.of("customer", customer)))
                .detailsFulltextSearch(customer);

    }

    /**
     * An end of the current API version carries the same fields as a change, and a BPMS which still
     * knows the task fills them, so this is what the cockpit is sent for a task which was completed.
     */
    private static UserTaskCompletedEvent taskCompleted() {

        return new UserTaskCompletedEvent()
                .id("event-completed")
                .userTaskId("task-1")
                .timestamp(ENDED_AT)
                .initiator("anna")
                .dueDate(DUE_AT)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .workflowId("workflow-1")
                .taskDefinition("assign-driver")
                .title(Map.of("en", "Assign a driver"))
                .uiUriPath("/ui")
                .uiUriType(UiUriType.WEBPACK_MF_REACT)
                .details(new LinkedHashMap<>(Map.of("customer", "Anna")))
                .detailsFulltextSearch("Anna");

    }

    private static WorkflowCreatedEvent workflowCreated() {

        return new WorkflowCreatedEvent()
                .id("event-created")
                .workflowId("workflow-1")
                .timestamp(CREATED_AT)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .title(Map.of("en", "A ride"))
                .uiUriPath("/ui")
                .uiUriType(UiUriType.WEBPACK_MF_REACT)
                .details(new LinkedHashMap<>(Map.of("customer", "Anna")))
                .detailsFulltextSearch("Anna");

    }

    private static WorkflowUpdatedEvent workflowChanged(
            final OffsetDateTime timestamp,
            final String customer) {

        return new WorkflowUpdatedEvent()
                .id("event-changed-" + customer)
                .workflowId("workflow-1")
                .timestamp(timestamp)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .title(Map.of("en", "A ride"))
                .uiUriPath("/ui")
                .uiUriType(UiUriType.WEBPACK_MF_REACT)
                .details(new LinkedHashMap<>(Map.of("customer", customer)))
                .detailsFulltextSearch(customer);

    }

    private static WorkflowCompletedEvent workflowCompleted() {

        return new WorkflowCompletedEvent()
                .id("event-completed")
                .workflowId("workflow-1")
                .timestamp(ENDED_AT)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .title(Map.of("en", "A ride"))
                .uiUriPath("/ui")
                .uiUriType(UiUriType.WEBPACK_MF_REACT)
                .details(new LinkedHashMap<>(Map.of("customer", "Anna")))
                .detailsFulltextSearch("Anna");

    }

    /**
     * What a stored user task is compared by. Left out is what the cockpit measures by its own clock
     * ({@code reportedAt}) and what a save recomputes ({@code version}, {@code updatedAt}): those say
     * when the two runs happened, not what they stored.
     */
    private static Map<String, Object> comparable(
            final UserTask task) {

        final var described = new LinkedHashMap<String, Object>();
        described.put("id", task.getId());
        described.put("createdAt", task.getCreatedAt());
        described.put("latestEventAt", task.getLatestEventAt());
        described.put("endedAt", task.getEndedAt());
        described.put("endReason", task.getEndReason());
        described.put("initiator", task.getInitiator());
        described.put("workflowModuleId", task.getWorkflowModuleId());
        described.put("bpmnProcessId", task.getBpmnProcessId());
        described.put("taskDefinition", task.getTaskDefinition());
        described.put("title", task.getTitle());
        described.put("dueDate", task.getDueDate());
        described.put("details", task.getDetails());
        described.put("detailsFulltextSearch", task.getDetailsFulltextSearch());
        return described;

    }

    /** @see #comparable(UserTask) */
    private static Map<String, Object> comparable(
            final Workflow workflow) {

        final var described = new LinkedHashMap<String, Object>();
        described.put("id", workflow.getId());
        described.put("createdAt", workflow.getCreatedAt());
        described.put("latestEventAt", workflow.getLatestEventAt());
        described.put("endedAt", workflow.getEndedAt());
        described.put("workflowModuleId", workflow.getWorkflowModuleId());
        described.put("bpmnProcessId", workflow.getBpmnProcessId());
        described.put("title", workflow.getTitle());
        described.put("details", workflow.getDetails());
        described.put("detailsFulltextSearch", workflow.getDetailsFulltextSearch());
        return described;

    }

    // --- a completion arriving before the creation it completes -------------------------------

    @Test
    void aCompletedTaskTheCockpitNeverSawIsStoredAsEnded() {

        final var cockpit = cockpitWhichWasSent(
                api -> api.userTaskCompletedEvent("task-1", taskCompleted()));

        final var stored = cockpit.storedTask();
        assertNotNull(stored, "the completion of an unknown task has to create it");
        assertEquals(ENDED_AT, stored.getEndedAt());
        assertEquals(UserTaskEndReason.COMPLETED, stored.getEndReason());
        // an end does not say when the task began, and the creation which will say it is the report
        // this task is still waiting for
        assertNull(stored.getCreatedAt());

    }

    @Test
    void aCreationArrivingAfterTheCompletionDoesNotReopenTheTask() {

        final var cockpit = cockpitWhichWasSent(api -> {
            api.userTaskCompletedEvent("task-1", taskCompleted());
            api.userTaskCreatedEvent(taskCreated());
        });

        final var stored = cockpit.storedTask();
        assertEquals(ENDED_AT, stored.getEndedAt());
        assertEquals(UserTaskEndReason.COMPLETED, stored.getEndReason());
        // and it did fill in what the end could not report
        assertEquals(CREATED_AT, stored.getCreatedAt());

    }

    @Test
    void bothOrdersOfACreationAndACompletionStoreTheSameTask() {

        final var inOrder = cockpitWhichWasSent(api -> {
            api.userTaskCreatedEvent(taskCreated());
            api.userTaskCompletedEvent("task-1", taskCompleted());
        });
        final var swapped = cockpitWhichWasSent(api -> {
            api.userTaskCompletedEvent("task-1", taskCompleted());
            api.userTaskCreatedEvent(taskCreated());
        });

        assertEquals(comparable(inOrder.storedTask()), comparable(swapped.storedTask()));

    }

    @Test
    void aCompletedCaseTheCockpitNeverSawIsStoredAsEnded() {

        final var cockpit = cockpitWhichWasSent(
                api -> api.workflowCompletedEvent("workflow-1", workflowCompleted()));

        final var stored = cockpit.storedWorkflow();
        assertNotNull(stored, "the completion of an unknown case has to create it");
        assertEquals(ENDED_AT, stored.getEndedAt());
        assertNull(stored.getCreatedAt());

    }

    @Test
    void bothOrdersOfACreationAndACompletionStoreTheSameCase() {

        final var inOrder = cockpitWhichWasSent(api -> {
            api.workflowCreatedEvent(workflowCreated());
            api.workflowCompletedEvent("workflow-1", workflowCompleted());
        });
        final var swapped = cockpitWhichWasSent(api -> {
            api.workflowCompletedEvent("workflow-1", workflowCompleted());
            api.workflowCreatedEvent(workflowCreated());
        });

        assertEquals(comparable(inOrder.storedWorkflow()), comparable(swapped.storedWorkflow()));

    }

    // --- a change arriving after the change which followed it ---------------------------------

    @Test
    void anOlderChangeDoesNotOverwriteAYoungerOneOfATask() {

        final var cockpit = cockpitWhichWasSent(api -> {
            api.userTaskCreatedEvent(taskCreated());
            api.userTaskUpdatedEvent("task-1", taskChanged(ENDED_AT, "Berta"));
            api.userTaskUpdatedEvent("task-1", taskChanged(CHANGED_AT, "Anna"));
        });

        final var stored = cockpit.storedTask();
        assertEquals(Map.of("customer", "Berta"), stored.getDetails());
        assertEquals(ENDED_AT, stored.getLatestEventAt());

    }

    @Test
    void anOlderChangeDoesNotOverwriteAYoungerOneOfACase() {

        final var cockpit = cockpitWhichWasSent(api -> {
            api.workflowCreatedEvent(workflowCreated());
            api.workflowUpdatedEvent("workflow-1", workflowChanged(ENDED_AT, "Berta"));
            api.workflowUpdatedEvent("workflow-1", workflowChanged(CHANGED_AT, "Anna"));
        });

        final var stored = cockpit.storedWorkflow();
        assertEquals(Map.of("customer", "Berta"), stored.getDetails());
        assertEquals(ENDED_AT, stored.getLatestEventAt());

    }

    @Test
    void bothOrdersOfTwoChangesStoreTheSameTask() {

        final var inOrder = cockpitWhichWasSent(api -> {
            api.userTaskCreatedEvent(taskCreated());
            api.userTaskUpdatedEvent("task-1", taskChanged(CHANGED_AT, "Anna"));
            api.userTaskUpdatedEvent("task-1", taskChanged(ENDED_AT, "Berta"));
        });
        final var swapped = cockpitWhichWasSent(api -> {
            api.userTaskCreatedEvent(taskCreated());
            api.userTaskUpdatedEvent("task-1", taskChanged(ENDED_AT, "Berta"));
            api.userTaskUpdatedEvent("task-1", taskChanged(CHANGED_AT, "Anna"));
        });

        assertEquals(comparable(inOrder.storedTask()), comparable(swapped.storedTask()));

    }

    // --- a change arriving after the end ------------------------------------------------------

    @Test
    void aChangeYoungerThanTheEndIsStoredWithoutReopeningTheTask() {

        final var cockpit = cockpitWhichWasSent(api -> {
            api.userTaskCreatedEvent(taskCreated());
            api.userTaskCompletedEvent("task-1", taskCompleted());
            api.userTaskUpdatedEvent("task-1", taskChanged(CHANGED_AFTER_THE_END, "Berta"));
        });

        final var stored = cockpit.storedTask();
        assertEquals(ENDED_AT, stored.getEndedAt());
        assertEquals(UserTaskEndReason.COMPLETED, stored.getEndReason());
        // the change is the younger report and says what the case is about now
        assertEquals(Map.of("customer", "Berta"), stored.getDetails());

    }

    @Test
    void aChangeYoungerThanTheEndIsStoredWithoutReopeningTheCase() {

        final var cockpit = cockpitWhichWasSent(api -> {
            api.workflowCreatedEvent(workflowCreated());
            api.workflowCompletedEvent("workflow-1", workflowCompleted());
            api.workflowUpdatedEvent("workflow-1", workflowChanged(CHANGED_AFTER_THE_END, "Berta"));
        });

        final var stored = cockpit.storedWorkflow();
        assertEquals(ENDED_AT, stored.getEndedAt());
        assertEquals(Map.of("customer", "Berta"), stored.getDetails());

    }

    @Test
    void aSecondEndOfATaskChangesNothing() {

        final var cockpit = cockpitWhichWasSent(api -> {
            api.userTaskCreatedEvent(taskCreated());
            api.userTaskCompletedEvent("task-1", taskCompleted());
            api.userTaskCancelledEvent("task-1", new UserTaskCancelledEvent()
                    .id("event-cancelled")
                    .userTaskId("task-1")
                    .timestamp(CHANGED_AFTER_THE_END)
                    .comment("the ride was called off")
                    .workflowModuleId("taxi-ride")
                    .bpmnProcessId("ride")
                    .taskDefinition("assign-driver")
                    .title(Map.of("en", "Assign a driver"))
                    .uiUriPath("/ui")
                    .uiUriType(UiUriType.WEBPACK_MF_REACT));
        });

        final var stored = cockpit.storedTask();
        assertEquals(ENDED_AT, stored.getEndedAt());
        assertEquals(UserTaskEndReason.COMPLETED, stored.getEndReason());

    }

    // --- what the log says about a report which changed nothing -------------------------------

    @Test
    void aReportWhichChangedNothingIsSaidOutLoud() {

        final var cockpit = cockpitWhichWasSent(api -> {
            api.userTaskCreatedEvent(taskCreated());
            api.userTaskUpdatedEvent("task-1", taskChanged(ENDED_AT, "Berta"));
            api.userTaskUpdatedEvent("task-1", taskChanged(CHANGED_AT, "Anna"));
        });

        // a change a workflow module sent and nobody finds in the cockpit has to be findable in the
        // log, with the task and both timestamps in it
        verify(cockpit.logger, atLeastOnce()).info(
                contains("Keeping user task"),
                eq("task-1"),
                eq("change"),
                eq(CHANGED_AT),
                eq(ENDED_AT));

    }

}
