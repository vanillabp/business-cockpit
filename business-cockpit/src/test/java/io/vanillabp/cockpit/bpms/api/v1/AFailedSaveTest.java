package io.vanillabp.cockpit.bpms.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;

/**
 * What version 1 of the API answers when it cannot store a report.
 * <p>
 * The sender treats {@code 400 Bad Request} as a report which can never go through and gives it up.
 * A save which failed because MongoDB was gone for a moment is no such report. Sent again, it goes
 * through. So the cockpit answers {@code 503 Service Unavailable}, which the sender repeats. The
 * tests drive the REST ingress with the generated mappers and the real services, and keep the
 * records in memory. A switch lets every save fail, the way MongoDB does when it cannot be reached.
 */
@ExtendWith(SuppressOutputExtension.class)
class AFailedSaveTest {

    private static final OffsetDateTime CHANGED_AT = OffsetDateTime.parse("2026-09-11T09:00:00Z");

    private static final OffsetDateTime CHANGED_LATER = OffsetDateTime.parse("2026-09-11T10:00:00Z");

    private final Map<String, UserTask> userTasks = new HashMap<>();

    private final Map<String, Workflow> workflows = new HashMap<>();

    private boolean saveFails;

    private BpmsApiController bpmsApi;

    @BeforeEach
    void setUp() throws Exception {

        final var personAndGroupMapper = mock(PersonAndGroupMapper.class);
        final var userTaskMapper = new UserTaskMapperV1Impl();
        set(UserTaskMapper.class, userTaskMapper, "personAndGroupMapper", personAndGroupMapper);
        final var workflowMapper = new WorkflowMapperV1Impl();
        set(WorkflowMapper.class, workflowMapper, "personAndGroupMapper", personAndGroupMapper);

        final var userTaskRepository = mock(UserTaskRepository.class);
        when(userTaskRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(userTasks.get(invocation.getArgument(0))));
        when(userTaskRepository.save(any(UserTask.class))).thenAnswer(invocation -> {
            failIfSwitchedOn();
            final UserTask task = invocation.getArgument(0);
            userTasks.put(task.getId(), task);
            return task;
        });
        final var userTaskService = new UserTaskService();
        set(UserTaskService.class, userTaskService, "userTasks", userTaskRepository);
        set(UserTaskService.class, userTaskService, "logger", mock(Logger.class));
        set(UserTaskService.class, userTaskService, "mongoTemplate", mock(MongoTemplate.class));

        final var workflowRepository = mock(WorkflowRepository.class);
        when(workflowRepository.findById(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(workflows.get(invocation.getArgument(0))));
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> {
            failIfSwitchedOn();
            final Workflow workflow = invocation.getArgument(0);
            workflows.put(workflow.getId(), workflow);
            return workflow;
        });
        final var workflowlistService = new WorkflowlistService();
        set(WorkflowlistService.class, workflowlistService, "workflowRepository", workflowRepository);
        set(WorkflowlistService.class, workflowlistService, "logger", mock(Logger.class));
        set(WorkflowlistService.class, workflowlistService, "mongoTemplate", mock(MongoTemplate.class));

        bpmsApi = new BpmsApiController();
        set(BpmsApiController.class, bpmsApi, "userTaskMapper", userTaskMapper);
        set(BpmsApiController.class, bpmsApi, "workflowMapper", workflowMapper);
        set(BpmsApiController.class, bpmsApi, "userTaskService", userTaskService);
        set(BpmsApiController.class, bpmsApi, "workflowlistService", workflowlistService);

    }

    private void failIfSwitchedOn() {

        if (saveFails) {
            throw new DataAccessResourceFailureException("MongoDB cannot be reached");
        }

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

    private static WorkflowCreatedOrUpdatedEvent workflowCreated(
            final String workflowId) {

        return new WorkflowCreatedOrUpdatedEvent()
                .id("event-created")
                .workflowId(workflowId)
                .timestamp(CHANGED_AT)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .title(Map.of("en", "A ride"))
                .uiUriPath("/ui")
                .uiUriType("WEBPACK_MF_REACT");

    }

    private static WorkflowCreatedOrUpdatedEvent workflowChanged(
            final String workflowId,
            final OffsetDateTime timestamp) {

        return new WorkflowCreatedOrUpdatedEvent()
                .id("event-changed")
                .workflowId(workflowId)
                .timestamp(timestamp)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .title(Map.of("en", "A ride"))
                .uiUriPath("/ui")
                .uiUriType("WEBPACK_MF_REACT");

    }

    private static WorkflowCompletedEvent workflowCompleted(
            final String workflowId) {

        return new WorkflowCompletedEvent()
                .id("event-completed")
                .workflowId(workflowId)
                .timestamp(CHANGED_LATER);

    }

    private static WorkflowCancelledEvent workflowCancelled(
            final String workflowId) {

        return new WorkflowCancelledEvent()
                .id("event-cancelled")
                .workflowId(workflowId)
                .timestamp(CHANGED_LATER);

    }

    private static UserTaskCreatedOrUpdatedEvent taskCreated(
            final String userTaskId) {

        return new UserTaskCreatedOrUpdatedEvent()
                .id("event-created")
                .userTaskId(userTaskId)
                .timestamp(CHANGED_AT)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .workflowId("workflow-1")
                .taskDefinition("assign-driver")
                .title(Map.of("en", "Assign a driver"))
                .uiUriPath("/ui")
                .uiUriType("WEBPACK_MF_REACT");

    }

    private static UserTaskCreatedOrUpdatedEvent taskChanged(
            final String userTaskId,
            final OffsetDateTime timestamp) {

        return new UserTaskCreatedOrUpdatedEvent()
                .id("event-changed")
                .userTaskId(userTaskId)
                .timestamp(timestamp)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .workflowId("workflow-1")
                .taskDefinition("assign-driver")
                .title(Map.of("en", "Assign a driver"))
                .uiUriPath("/ui")
                .uiUriType("WEBPACK_MF_REACT");

    }

    private static UserTaskCompletedEvent taskCompleted(
            final String userTaskId) {

        return new UserTaskCompletedEvent()
                .id("event-completed")
                .userTaskId(userTaskId)
                .timestamp(CHANGED_LATER);

    }

    private static UserTaskCancelledEvent taskCancelled(
            final String userTaskId) {

        return new UserTaskCancelledEvent()
                .id("event-cancelled")
                .userTaskId(userTaskId)
                .timestamp(CHANGED_LATER);

    }

    @Test
    void aUserTaskReportWhichCannotBeStoredIsToBeSentAgain() {

        saveFails = true;

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                bpmsApi.userTaskCreatedEvent(taskCreated("task-1")).getStatusCode());
        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                bpmsApi.userTaskUpdatedEvent("task-1", taskChanged("task-1", CHANGED_AT)).getStatusCode());
        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                bpmsApi.userTaskCompletedEvent("task-1", taskCompleted("task-1")).getStatusCode());
        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                bpmsApi.userTaskCancelledEvent("task-1", taskCancelled("task-1")).getStatusCode());
        assertEquals(Map.of(), userTasks);

    }

    @Test
    void aWorkflowReportWhichCannotBeStoredIsToBeSentAgain() {

        saveFails = true;

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                bpmsApi.workflowCreatedEvent(workflowCreated("workflow-1")).getStatusCode());
        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                bpmsApi.workflowUpdatedEvent("workflow-1", workflowChanged("workflow-1", CHANGED_AT)).getStatusCode());
        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                bpmsApi.workflowCompletedEvent("workflow-1", workflowCompleted("workflow-1")).getStatusCode());
        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                bpmsApi.workflowCancelledEvent("workflow-1", workflowCancelled("workflow-1")).getStatusCode());
        assertEquals(Map.of(), workflows);

    }

    /** The positive twin: the same report sent again, once MongoDB is back, goes through. */
    @Test
    void theSameReportSentAgainGoesThrough() {

        saveFails = true;
        bpmsApi.userTaskCreatedEvent(taskCreated("task-1"));
        bpmsApi.workflowCreatedEvent(workflowCreated("workflow-1"));

        saveFails = false;
        assertEquals(HttpStatus.OK, bpmsApi.userTaskCreatedEvent(taskCreated("task-1")).getStatusCode());
        assertEquals(HttpStatus.OK, bpmsApi.workflowCreatedEvent(workflowCreated("workflow-1")).getStatusCode());
        assertEquals(1, userTasks.size());
        assertEquals(1, workflows.size());

    }

    /**
     * A report older than what is stored is no failure. The cockpit keeps what it has and answers
     * {@code 200 OK}, because sending it again would change nothing. It does not even try to store,
     * so a failing MongoDB does not turn it into a {@code 503}.
     */
    @Test
    void aReportOlderThanWhatIsStoredIsTakenAndChangesNothing() {

        bpmsApi.userTaskUpdatedEvent("task-1", taskChanged("task-1", CHANGED_LATER));
        bpmsApi.workflowUpdatedEvent("workflow-1", workflowChanged("workflow-1", CHANGED_LATER));
        bpmsApi.userTaskCompletedEvent("task-1", taskCompleted("task-1"));
        bpmsApi.workflowCompletedEvent("workflow-1", workflowCompleted("workflow-1"));

        saveFails = true;
        assertEquals(
                HttpStatus.OK,
                bpmsApi.userTaskUpdatedEvent("task-1", taskChanged("task-1", CHANGED_AT)).getStatusCode());
        assertEquals(
                HttpStatus.OK,
                bpmsApi.workflowUpdatedEvent("workflow-1", workflowChanged("workflow-1", CHANGED_AT)).getStatusCode());
        // a second end, and a creation of what the cockpit knows already, are taken the same way
        assertEquals(
                HttpStatus.OK,
                bpmsApi.userTaskCancelledEvent("task-1", taskCancelled("task-1")).getStatusCode());
        assertEquals(
                HttpStatus.OK,
                bpmsApi.workflowCancelledEvent("workflow-1", workflowCancelled("workflow-1")).getStatusCode());
        assertEquals(HttpStatus.OK, bpmsApi.userTaskCreatedEvent(taskCreated("task-1")).getStatusCode());

    }

    /** A report the cockpit refuses for what it says stays refused, whatever MongoDB does. */
    @Test
    void aRefusedReportIsStillRefusedWhileSavingFails() {

        saveFails = true;

        assertEquals(
                HttpStatus.BAD_REQUEST,
                bpmsApi.userTaskUpdatedEvent("task-1", taskChanged("task-2", CHANGED_AT)).getStatusCode());
        assertEquals(
                HttpStatus.BAD_REQUEST,
                bpmsApi.workflowCompletedEvent("workflow-1", workflowCompleted("workflow-2")).getStatusCode());

    }

}
