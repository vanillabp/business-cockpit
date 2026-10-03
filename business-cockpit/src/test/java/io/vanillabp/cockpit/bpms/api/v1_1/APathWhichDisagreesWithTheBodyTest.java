package io.vanillabp.cockpit.bpms.api.v1_1;

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
 * What the current API version does with a report whose path names one user task or case and whose
 * body names another.
 * <p>
 * The service looks the record up by the id in the path, and a mapper which creates the record
 * takes the id from the body. If the two differed, a change of an unknown case was stored under the
 * id of the body, and the next change with the same path did not find it. So the cockpit refuses
 * such a report. The tests drive the REST ingress with the generated mappers and the real services,
 * and keep the records in memory instead of in MongoDB.
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
        final var userTaskMapper = new UserTaskMapperV1_1Impl();
        set(UserTaskMapper.class, userTaskMapper, "personAndGroupMapper", personAndGroupMapper);
        final var workflowMapper = new WorkflowMapperV1_1Impl();
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

    private static WorkflowUpdatedEvent workflowChanged(
            final String workflowId,
            final OffsetDateTime timestamp,
            final String customer) {

        return new WorkflowUpdatedEvent()
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

    private static WorkflowCompletedEvent workflowCompleted(
            final String workflowId) {

        return new WorkflowCompletedEvent()
                .id("event-completed")
                .workflowId(workflowId)
                .timestamp(CHANGED_LATER)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride");

    }

    private static WorkflowCancelledEvent workflowCancelled(
            final String workflowId) {

        return new WorkflowCancelledEvent()
                .id("event-cancelled")
                .workflowId(workflowId)
                .timestamp(CHANGED_LATER)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride");

    }

    private static UserTaskUpdatedEvent taskChanged(
            final String userTaskId) {

        return new UserTaskUpdatedEvent()
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

    private static UserTaskCompletedEvent taskCompleted(
            final String userTaskId) {

        return new UserTaskCompletedEvent()
                .id("event-completed")
                .userTaskId(userTaskId)
                .timestamp(CHANGED_LATER)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .workflowId("workflow-1")
                .taskDefinition("assign-driver")
                .title(Map.of("en", "Assign a driver"))
                .uiUriPath("/ui")
                .uiUriType("WEBPACK_MF_REACT");

    }

    private static UserTaskCancelledEvent taskCancelled(
            final String userTaskId) {

        return new UserTaskCancelledEvent()
                .id("event-cancelled")
                .userTaskId(userTaskId)
                .timestamp(CHANGED_LATER)
                .workflowModuleId("taxi-ride")
                .bpmnProcessId("ride")
                .workflowId("workflow-1")
                .taskDefinition("assign-driver")
                .title(Map.of("en", "Assign a driver"))
                .uiUriPath("/ui")
                .uiUriType("WEBPACK_MF_REACT");

    }

    /**
     * The case of the story: a change creates a case the cockpit never saw, and the next change of
     * the same path has to find it. With a body which names another case, it would have been stored
     * under that other id.
     */
    @Test
    void aChangeWhoseBodyNamesAnotherCaseIsRefused(
            final CapturedOutput output) {

        final var answer = bpmsApi.workflowUpdatedEvent(
                "workflow-1", workflowChanged("workflow-2", CHANGED_AT, "Anna"));

        assertEquals(HttpStatus.BAD_REQUEST, answer.getStatusCode());
        assertEquals(Map.of(), workflows, "a refused report stores nothing");
        assertTrue(
                output.getAllOfThisTest().contains(REFUSAL.formatted("workflow-1", "workflow-2")),
                "the log has to say why the report was refused");

    }

    /** The positive twin: what the refusal above protects, for a body which names the path. */
    @Test
    void theNextChangeFindsTheCaseAChangeCreated(
            final CapturedOutput output) {

        assertEquals(
                HttpStatus.OK,
                bpmsApi.workflowUpdatedEvent("workflow-1", workflowChanged("workflow-1", CHANGED_AT, "Anna"))
                        .getStatusCode());
        assertEquals(
                HttpStatus.OK,
                bpmsApi.workflowUpdatedEvent("workflow-1", workflowChanged("workflow-1", CHANGED_LATER, "Berta"))
                        .getStatusCode());

        assertEquals(1, workflows.size());
        assertEquals(Map.of("customer", "Berta"), workflows.get("workflow-1").getDetails());
        assertTrue(!output.getAllOfThisTest().contains(REFUSAL.formatted("workflow-1", "workflow-1")));

    }

    @Test
    void anEndWhoseBodyNamesAnotherCaseIsRefused() {

        assertEquals(
                HttpStatus.BAD_REQUEST,
                bpmsApi.workflowCompletedEvent("workflow-1", workflowCompleted("workflow-2")).getStatusCode());
        assertEquals(
                HttpStatus.BAD_REQUEST,
                bpmsApi.workflowCancelledEvent("workflow-1", workflowCancelled("workflow-2")).getStatusCode());
        assertEquals(Map.of(), workflows);

    }

    @Test
    void aReportWhoseBodyNamesAnotherTaskIsRefused(
            final CapturedOutput output) {

        assertEquals(
                HttpStatus.BAD_REQUEST,
                bpmsApi.userTaskUpdatedEvent("task-1", taskChanged("task-2")).getStatusCode());
        assertEquals(
                HttpStatus.BAD_REQUEST,
                bpmsApi.userTaskCompletedEvent("task-1", taskCompleted("task-2")).getStatusCode());
        assertEquals(
                HttpStatus.BAD_REQUEST,
                bpmsApi.userTaskCancelledEvent("task-1", taskCancelled("task-2")).getStatusCode());
        assertEquals(Map.of(), userTasks);
        assertTrue(output.getAllOfThisTest().contains(REFUSAL.formatted("task-1", "task-2")));

    }

    @Test
    void aChangeOfAnUnknownTaskIsStoredUnderThePath() {

        assertEquals(
                HttpStatus.OK,
                bpmsApi.userTaskUpdatedEvent("task-1", taskChanged("task-1")).getStatusCode());
        assertEquals(
                HttpStatus.OK,
                bpmsApi.userTaskCompletedEvent("task-1", taskCompleted("task-1")).getStatusCode());

        assertEquals(1, userTasks.size());
        assertEquals(CHANGED_LATER, userTasks.get("task-1").getEndedAt());

    }

}
