package io.vanillabp.cockpit.bpms.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import io.vanillabp.integration.test.utils.CapturedOutput;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;

/**
 * What version 1 of the API does with a report whose path names one user task or case and whose
 * body names another. It refuses it, like the current version does, and for the same reason: the
 * service looks the record up by the path, and a mapper which creates the record takes the id from
 * the body.
 */
@ExtendWith(SuppressOutputExtension.class)
class APathWhichDisagreesWithTheBodyTest {

    private static final OffsetDateTime CHANGED_AT = OffsetDateTime.parse("2026-09-11T09:00:00Z");

    private static final OffsetDateTime CHANGED_LATER = OffsetDateTime.parse("2026-09-11T10:00:00Z");

    private static final String REFUSAL = "the path names '%s' and the body names '%s'";

    private final Map<String, UserTask> userTasks = new HashMap<>();

    private final Map<String, Workflow> workflows = new HashMap<>();

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

    private static void set(
            final Class<?> declaringClass,
            final Object target,
            final String field,
            final Object value) throws Exception {

        final var declaredField = declaringClass.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(target, value);

    }

    private static WorkflowCreatedOrUpdatedEvent workflowChanged(
            final String workflowId,
            final OffsetDateTime timestamp,
            final String customer) {

        return new WorkflowCreatedOrUpdatedEvent()
                .id("event-" + customer)
                .workflowId(workflowId)
                .timestamp(timestamp)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .title(Map.of("en", "A ride"))
                .uiUriPath("/ui")
                .uiUriType("WEBPACK_MF_REACT")
                .details(new HashMap<>(Map.of("customer", customer)));

    }

    private static UserTaskCreatedOrUpdatedEvent taskChanged(
            final String userTaskId) {

        return new UserTaskCreatedOrUpdatedEvent()
                .id("event-changed")
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

    @Test
    void aChangeWhoseBodyNamesAnotherCaseIsRefused(
            final CapturedOutput output) {

        final var answer = bpmsApi.workflowUpdatedEvent(
                "workflow-1", workflowChanged("workflow-2", CHANGED_AT, "Anna"));

        assertEquals(HttpStatus.BAD_REQUEST, answer.getStatusCode());
        assertEquals(Map.of(), workflows, "a refused report stores nothing");
        assertTrue(output.getAllOfThisTest().contains(REFUSAL.formatted("workflow-1", "workflow-2")));

    }

    /** The positive twin of the refusal above. */
    @Test
    void theNextChangeFindsTheCaseAChangeCreated(
            final CapturedOutput output) {

        bpmsApi.workflowUpdatedEvent("workflow-1", workflowChanged("workflow-1", CHANGED_AT, "Anna"));
        final var answer = bpmsApi.workflowUpdatedEvent(
                "workflow-1", workflowChanged("workflow-1", CHANGED_LATER, "Berta"));

        assertEquals(HttpStatus.OK, answer.getStatusCode());
        assertEquals(1, workflows.size());
        assertEquals(Map.of("customer", "Berta"), workflows.get("workflow-1").getDetails());
        assertTrue(!output.getAllOfThisTest().contains(REFUSAL.formatted("workflow-1", "workflow-1")));

    }

    @Test
    void anEndWhoseBodyNamesAnotherCaseIsRefused() {

        final var completed = new WorkflowCompletedEvent()
                .id("event-completed")
                .workflowId("workflow-2")
                .timestamp(CHANGED_LATER);
        final var cancelled = new WorkflowCancelledEvent()
                .id("event-cancelled")
                .workflowId("workflow-2")
                .timestamp(CHANGED_LATER);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                bpmsApi.workflowCompletedEvent("workflow-1", completed).getStatusCode());
        assertEquals(
                HttpStatus.BAD_REQUEST,
                bpmsApi.workflowCancelledEvent("workflow-1", cancelled).getStatusCode());
        assertEquals(Map.of(), workflows);

    }

    @Test
    void aReportWhoseBodyNamesAnotherTaskIsRefused(
            final CapturedOutput output) {

        final var completed = new UserTaskCompletedEvent()
                .id("event-completed")
                .userTaskId("task-2")
                .timestamp(CHANGED_LATER);
        final var cancelled = new UserTaskCancelledEvent()
                .id("event-cancelled")
                .userTaskId("task-2")
                .timestamp(CHANGED_LATER);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                bpmsApi.userTaskUpdatedEvent("task-1", taskChanged("task-2")).getStatusCode());
        assertEquals(
                HttpStatus.BAD_REQUEST,
                bpmsApi.userTaskCompletedEvent("task-1", completed).getStatusCode());
        assertEquals(
                HttpStatus.BAD_REQUEST,
                bpmsApi.userTaskCancelledEvent("task-1", cancelled).getStatusCode());
        assertEquals(Map.of(), userTasks);
        assertTrue(output.getAllOfThisTest().contains(REFUSAL.formatted("task-1", "task-2")));

    }

    @Test
    void aChangeOfAnUnknownTaskIsStoredUnderThePath() {

        assertEquals(
                HttpStatus.OK,
                bpmsApi.userTaskUpdatedEvent("task-1", taskChanged("task-1")).getStatusCode());

        assertEquals(1, userTasks.size());
        assertEquals("task-1", userTasks.get("task-1").getId());

    }

}
