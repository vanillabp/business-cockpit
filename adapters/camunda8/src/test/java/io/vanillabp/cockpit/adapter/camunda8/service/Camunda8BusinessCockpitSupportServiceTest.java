package io.vanillabp.cockpit.adapter.camunda8.service;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.CamundaFuture;
import io.camunda.client.api.search.request.ProcessInstanceSearchRequest;
import io.camunda.client.api.search.request.UserTaskSearchRequest;
import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.client.api.search.response.SearchResponse;
import io.camunda.client.api.search.response.UserTask;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.spi.process.WorkflowNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.retry.annotation.RecoverAnnotationRecoveryHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Camunda8BusinessCockpitSupportServiceTest {

    private final Camunda8BusinessCockpitSupportService service = new Camunda8BusinessCockpitSupportService(
            mock(Camunda8WorkflowEventHandler.class),
            mock(Camunda8UserTaskEventHandler.class));

    @Test
    void userTaskSearchShouldRecoverWithEmptyListIfRetriesAreExhausted() throws Exception {
        final var retryableMethod = Camunda8BusinessCockpitSupportService.class.getMethod(
                "getUserTasks",
                CamundaClient.class,
                String.class,
                String.class,
                String.class,
                String.class,
                List.class);
        // the same handler the retry proxy uses once all attempts failed
        final var recoveryHandler = new RecoverAnnotationRecoveryHandler<>(service, retryableMethod);

        final var result = recoveryHandler.recover(
                new Object[] { mock(CamundaClient.class), null, "test-process", "business-key-1", "id", List.of(222L) },
                new WorkflowNotFoundException("not found"));

        assertEquals(List.of(), result);
    }

    @Test
    void userTaskSearchShouldReturnOnlyRequestedUserTasks() {
        final var client = clientWithUserTasks(111L, 222L, 333L);

        final var result = service.getUserTasks(
                client, null, "test-process", "business-key-1", "id", List.of(111L, 333L));

        assertEquals(List.of(111L, 333L), result.stream().map(UserTask::getUserTaskKey).toList());
    }

    @Test
    void userTaskSearchShouldReturnAllUserTasksIfNoUserTaskIdsAreGiven() {
        final var client = clientWithUserTasks(111L, 222L);

        final var resultForNull = service.getUserTasks(
                client, null, "test-process", "business-key-1", "id", null);
        final var resultForEmpty = service.getUserTasks(
                client, null, "test-process", "business-key-1", "id", List.of());

        assertEquals(List.of(111L, 222L), resultForNull.stream().map(UserTask::getUserTaskKey).toList());
        assertEquals(List.of(111L, 222L), resultForEmpty.stream().map(UserTask::getUserTaskKey).toList());
    }

    @SuppressWarnings("unchecked")
    private static CamundaClient clientWithUserTasks(
            final long... userTaskKeys) {

        final var client = mock(CamundaClient.class);

        // one root process instance identified by business key
        final var processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessInstanceKey()).thenReturn(4711L);
        // the client reports a missing parent as null (a mock would default to 0)
        when(processInstance.getParentProcessInstanceKey()).thenReturn(null);
        // both stubbed: the commits in between use singleItem() or items(), and
        // singleItem() is not a default method in every client version
        final var processInstanceResponse = (SearchResponse<ProcessInstance>) mock(SearchResponse.class);
        when(processInstanceResponse.singleItem()).thenReturn(processInstance);
        when(processInstanceResponse.items()).thenReturn(List.of(processInstance));
        final var processInstanceFuture = (CamundaFuture<SearchResponse<ProcessInstance>>) mock(CamundaFuture.class);
        when(processInstanceFuture.join(anyLong(), any(TimeUnit.class))).thenReturn(processInstanceResponse);
        final var processInstanceRequest = mock(ProcessInstanceSearchRequest.class, RETURNS_SELF);
        when(processInstanceRequest.send()).thenReturn(processInstanceFuture);
        when(client.newProcessInstanceSearchRequest()).thenReturn(processInstanceRequest);

        // user tasks of that process instance
        final var userTasks = new ArrayList<UserTask>();
        for (final var userTaskKey : userTaskKeys) {
            final var userTask = mock(UserTask.class);
            when(userTask.getUserTaskKey()).thenReturn(userTaskKey);
            userTasks.add(userTask);
        }
        final var userTaskResponse = (SearchResponse<UserTask>) mock(SearchResponse.class);
        when(userTaskResponse.items()).thenReturn(List.copyOf(userTasks));
        final var userTaskFuture = (CamundaFuture<SearchResponse<UserTask>>) mock(CamundaFuture.class);
        when(userTaskFuture.join(anyLong(), any(TimeUnit.class))).thenReturn(userTaskResponse);
        final var userTaskRequest = mock(UserTaskSearchRequest.class, RETURNS_SELF);
        when(userTaskRequest.send()).thenReturn(userTaskFuture);
        when(client.newUserTaskSearchRequest()).thenReturn(userTaskRequest);

        return client;

    }

}
