package io.vanillabp.cockpit.bpms.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.vanillabp.cockpit.commons.exceptions.RestfulExceptionHandler;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleService;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

/**
 * What version 1 of the API answers to a report which leaves out a field the schema requires.
 * <p>
 * Such a report can never go through, so the answer is {@code 400 Bad Request}, which the sender
 * gives up on. A {@code 500} made the sender repeat it until its attempts were used up. The body
 * names the field, so the log of the sender says what to fix. The tests send JSON through Spring MVC,
 * so the bean validation and the cockpit's exception handler are the real ones.
 */
@ExtendWith(SuppressOutputExtension.class)
class AMissingRequiredFieldTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Map<String, UserTask> userTasks = new HashMap<>();

    private final Map<String, Workflow> workflows = new HashMap<>();

    private MockMvc client;

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

        final var bpmsApi = new BpmsApiController();
        set(BpmsApiController.class, bpmsApi, "userTaskMapper", userTaskMapper);
        set(BpmsApiController.class, bpmsApi, "workflowMapper", workflowMapper);
        set(BpmsApiController.class, bpmsApi, "userTaskService", userTaskService);
        set(BpmsApiController.class, bpmsApi, "workflowlistService", workflowlistService);
        set(BpmsApiController.class, bpmsApi, "workflowModuleService", mock(WorkflowModuleService.class));

        final var exceptionHandler = new RestfulExceptionHandler();
        set(
                RestfulExceptionHandler.class,
                exceptionHandler,
                "logger",
                LoggerFactory.getLogger(AMissingRequiredFieldTest.class));

        client = MockMvcBuilders
                .standaloneSetup(bpmsApi)
                .setControllerAdvice(exceptionHandler)
                .build();

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

    private static Map<String, Object> aTask() {

        final var task = new LinkedHashMap<String, Object>();
        task.put("id", "event-1");
        task.put("userTaskId", "task-1");
        task.put("timestamp", "2026-09-11T09:00:00Z");
        task.put("workflowModuleId", "taxi-ride");
        task.put("bpmnProcessId", "ride");
        task.put("workflowId", "workflow-1");
        task.put("taskDefinition", "assign-driver");
        task.put("title", Map.of("en", "Assign a driver"));
        task.put("uiUriPath", "/ui");
        task.put("uiUriType", "WEBPACK_MF_REACT");
        return task;

    }

    private static Map<String, Object> aWorkflow() {

        final var workflow = new LinkedHashMap<String, Object>();
        workflow.put("id", "event-1");
        workflow.put("workflowId", "workflow-1");
        workflow.put("timestamp", "2026-09-11T09:00:00Z");
        workflow.put("workflowModuleId", "taxi-ride");
        workflow.put("bpmnProcessId", "ride");
        workflow.put("title", Map.of("en", "A ride"));
        workflow.put("uiUriPath", "/ui");
        workflow.put("uiUriType", "WEBPACK_MF_REACT");
        return workflow;

    }

    private static Map<String, Object> without(
            final Map<String, Object> report,
            final String... fields) {

        for (final var field : fields) {
            report.remove(field);
        }
        return report;

    }

    private MvcResult send(
            final String path,
            final Map<String, Object> report) throws Exception {

        return client
                .perform(post(BpmsApiController.BPMS_API_URL_PREFIX + path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(report)))
                .andReturn();

    }

    @Test
    void aTaskWithoutATimestampIsRefusedAndTheFieldIsNamed() throws Exception {

        final var answer = send("/usertask/created", without(aTask(), "timestamp"));

        assertEquals(400, answer.getResponse().getStatus());
        assertEquals(
                "The request is not valid: 'timestamp' is missing.",
                answer.getResponse().getContentAsString());
        assertEquals(Map.of(), userTasks, "a refused report stores nothing");

    }

    @Test
    void everyMissingFieldIsNamedInTheOrderOfTheirNames() throws Exception {

        final var answer = send("/usertask/task-1/updated", without(aTask(), "title", "bpmnProcessId"));

        assertEquals(400, answer.getResponse().getStatus());
        assertEquals(
                "The request is not valid: 'bpmnProcessId' is missing, 'title' is missing.",
                answer.getResponse().getContentAsString());

    }

    /** An end of this version carries the id, the event and the time and nothing else. */
    private static Map<String, Object> anEnd(
            final String idField,
            final String id) {

        final var end = new LinkedHashMap<String, Object>();
        end.put("id", "event-2");
        end.put(idField, id);
        end.put("timestamp", "2026-09-11T10:00:00Z");
        return end;

    }

    @Test
    void anEndWithoutItsIdIsRefused() throws Exception {

        assertEquals(
                400,
                send("/usertask/task-1/completed", without(anEnd("userTaskId", "task-1"), "userTaskId"))
                        .getResponse().getStatus());
        assertEquals(
                400,
                send("/usertask/task-1/cancelled", without(anEnd("userTaskId", "task-1"), "userTaskId"))
                        .getResponse().getStatus());
        assertEquals(
                400,
                send("/workflow/workflow-1/completed", without(anEnd("workflowId", "workflow-1"), "workflowId"))
                        .getResponse().getStatus());
        assertEquals(
                400,
                send("/workflow/workflow-1/cancelled", without(anEnd("workflowId", "workflow-1"), "workflowId"))
                        .getResponse().getStatus());

    }

    @Test
    void anEndWithEveryFieldIsStored() throws Exception {

        assertEquals(
                200,
                send("/usertask/task-1/completed", anEnd("userTaskId", "task-1")).getResponse().getStatus());
        assertEquals(
                200,
                send("/workflow/workflow-1/cancelled", anEnd("workflowId", "workflow-1")).getResponse().getStatus());

    }

    @Test
    void aWorkflowWithoutItsIdIsRefusedAndTheFieldIsNamed() throws Exception {

        final var created = send("/workflow/created", without(aWorkflow(), "workflowId"));
        final var updated = send("/workflow/workflow-1/updated", without(aWorkflow(), "workflowId"));

        assertEquals(400, created.getResponse().getStatus());
        assertEquals(400, updated.getResponse().getStatus());
        assertTrue(
                created.getResponse().getContentAsString().contains("'workflowId' is missing"),
                created.getResponse().getContentAsString());
        assertEquals(Map.of(), workflows);

    }

    /** A value the sender put into the report is not sent back, however large it is. */
    @Test
    void theAnswerDoesNotRepeatWhatTheReportSaid() throws Exception {

        final var report = without(aWorkflow(), "timestamp");
        final var large = "x".repeat(10_000);
        report.put("details", Map.of("customer", large));

        final var answer = send("/workflow/workflow-1/updated", report);

        assertEquals(400, answer.getResponse().getStatus());
        assertFalse(answer.getResponse().getContentAsString().contains("xxxx"));
        assertTrue(answer.getResponse().getContentAsString().length() < 200);

    }

    /** The positive twin: the same reports with every field go through. */
    @Test
    void aCompleteReportIsStored() throws Exception {

        assertEquals(200, send("/usertask/created", aTask()).getResponse().getStatus());
        assertEquals(200, send("/workflow/created", aWorkflow()).getResponse().getStatus());
        assertEquals(1, userTasks.size());
        assertEquals(1, workflows.size());

    }

}
