package io.vanillabp.cockpit.bpms.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What an end reported through version 1 of the API does to what the cockpit stored.
 * <p>
 * Version 1 has no mapping of an end and needs none: its end is a lifecycle event, saying who ended
 * the task or the case and why, and the controller writes those two fields by hand. So an end of
 * this version cannot take a due date, a title or the business data away however little the
 * reporting side knows about the case by then, and these tests hold the controller to writing
 * nothing else.
 */
class WhatAnEndMayNotOverwriteTest {

    private static final OffsetDateTime DUE_AT = OffsetDateTime.parse("2026-09-12T08:00:00Z");

    private static final OffsetDateTime ENDED_AT = OffsetDateTime.parse("2026-09-13T10:00:00Z");

    private BpmsApiController bpmsApi;

    private UserTask endedTask;

    private Workflow endedWorkflow;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {

        endedTask = storedTask();
        endedWorkflow = storedWorkflow();

        final var userTaskService = mock(UserTaskService.class);
        when(userTaskService.reportEndedUserTask(anyString(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    ((Consumer<UserTask>) invocation.getArgument(3)).accept(endedTask);
                    return true;
                });

        final var workflowlistService = mock(WorkflowlistService.class);
        when(workflowlistService.reportEndedWorkflow(anyString(), any(), any()))
                .thenAnswer(invocation -> {
                    ((Consumer<Workflow>) invocation.getArgument(2)).accept(endedWorkflow);
                    return true;
                });

        bpmsApi = new BpmsApiController();
        set(bpmsApi, "userTaskService", userTaskService);
        set(bpmsApi, "workflowlistService", workflowlistService);

    }

    private static void set(
            final Object target,
            final String field,
            final Object value) throws Exception {

        final var declaredField = BpmsApiController.class.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(target, value);

    }

    /** A task the cockpit knows everything about, so that every field has something to lose. */
    private static UserTask storedTask() {

        final var task = new UserTask();
        task.setId("task-1");
        task.setBusinessId("4711");
        task.setTitle(new HashMap<>(Map.of("en", "Assign a driver for ride 4711")));
        task.setDueDate(DUE_AT);
        task.setComment("the driver called in sick");
        task.setDetails(new HashMap<>(Map.of("customer", "Anna")));
        task.setDetailsFulltextSearch("Anna");
        final var assignee = new Person();
        assignee.setId("anna");
        task.setAssignee(assignee);
        return task;

    }

    /** A case the cockpit knows everything about. */
    private static Workflow storedWorkflow() {

        final var workflow = new Workflow();
        workflow.setId("workflow-1");
        workflow.setBusinessId("4711");
        workflow.setTitle(new HashMap<>(Map.of("en", "Ride 4711")));
        workflow.setComment("the customer complained");
        workflow.setDetails(new HashMap<>(Map.of("customer", "Anna")));
        workflow.setDetailsFulltextSearch("Anna");
        return workflow;

    }

    @Test
    @DisplayName("A completion of version 1 writes who completed the task and nothing else")
    void aCompletionKeepsEverythingButWhoEndedTheTask() {

        bpmsApi.userTaskCompletedEvent(
                "task-1",
                new UserTaskCompletedEvent()
                        .id("event-completed")
                        .userTaskId("task-1")
                        .timestamp(ENDED_AT)
                        .initiator("bert"));

        assertEquals("bert", endedTask.getInitiator());
        assertEquals(DUE_AT, endedTask.getDueDate());
        assertEquals(Map.of("en", "Assign a driver for ride 4711"), endedTask.getTitle());
        assertEquals("4711", endedTask.getBusinessId());
        assertEquals("the driver called in sick", endedTask.getComment());
        assertEquals(Map.of("customer", "Anna"), endedTask.getDetails());
        assertEquals("Anna", endedTask.getDetailsFulltextSearch());
        assertEquals("anna", endedTask.getAssignee().getId());

    }

    @Test
    @DisplayName("A cancellation of version 1 writes who cancelled the task and why, and nothing else")
    void aCancellationKeepsEverythingButWhoEndedTheTaskAndTheReason() {

        bpmsApi.userTaskCancelledEvent(
                "task-1",
                new UserTaskCancelledEvent()
                        .id("event-cancelled")
                        .userTaskId("task-1")
                        .timestamp(ENDED_AT)
                        .initiator("bert")
                        .comment("the ride was called off"));

        assertEquals("bert", endedTask.getInitiator());
        assertEquals("the ride was called off", endedTask.getComment());
        assertEquals(DUE_AT, endedTask.getDueDate());
        assertEquals(Map.of("en", "Assign a driver for ride 4711"), endedTask.getTitle());
        assertEquals(Map.of("customer", "Anna"), endedTask.getDetails());

    }

    @Test
    @DisplayName("A completed case of version 1 keeps everything the cockpit stored")
    void aCompletedCaseKeepsEverything() {

        bpmsApi.workflowCompletedEvent(
                "workflow-1",
                new WorkflowCompletedEvent()
                        .id("event-completed")
                        .workflowId("workflow-1")
                        .timestamp(ENDED_AT));

        assertEquals(Map.of("en", "Ride 4711"), endedWorkflow.getTitle());
        assertEquals("4711", endedWorkflow.getBusinessId());
        assertEquals("the customer complained", endedWorkflow.getComment());
        assertEquals(Map.of("customer", "Anna"), endedWorkflow.getDetails());
        assertEquals("Anna", endedWorkflow.getDetailsFulltextSearch());

    }

    @Test
    @DisplayName("A cancelled case of version 1 writes why it was cancelled and nothing else")
    void aCancelledCaseKeepsEverythingButTheReason() {

        bpmsApi.workflowCancelledEvent(
                "workflow-1",
                new WorkflowCancelledEvent()
                        .id("event-cancelled")
                        .workflowId("workflow-1")
                        .timestamp(ENDED_AT)
                        .comment("the customer called it off"));

        assertEquals("the customer called it off", endedWorkflow.getComment());
        assertEquals(Map.of("en", "Ride 4711"), endedWorkflow.getTitle());
        assertEquals("4711", endedWorkflow.getBusinessId());
        assertEquals(Map.of("customer", "Anna"), endedWorkflow.getDetails());

    }

}
