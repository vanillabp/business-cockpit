package io.vanillabp.cockpit.adapter.camunda8.service;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.CamundaFuture;
import io.camunda.client.api.search.enums.ProcessInstanceState;
import io.camunda.client.api.search.filter.ProcessInstanceFilter;
import io.camunda.client.api.search.filter.UserTaskFilter;
import io.camunda.client.api.search.request.ProcessInstanceSearchRequest;
import io.camunda.client.api.search.request.UserTaskSearchRequest;
import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.client.api.search.response.SearchResponse;
import io.camunda.client.api.search.response.UserTask;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Camunda8BusinessCockpitServiceTest {

    @Mock
    private CamundaClient camundaClient;

    @Mock
    private Camunda8WorkflowEventHandler workflowEventHandler;

    @Mock
    private Camunda8UserTaskEventHandler userTaskEventHandler;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private Camunda8BusinessCockpitService<String> service;

    @BeforeEach
    void setup() {
        service = new Camunda8BusinessCockpitService<String>(
                mock(CrudRepository.class),
                String.class,
                businessKey -> businessKey,
                businessKey -> businessKey,
                "id",
                applicationEventPublisher,
                workflowEventHandler,
                userTaskEventHandler,
                new Camunda8BusinessCockpitSupportService(workflowEventHandler, userTaskEventHandler));
        service.setBpmnProcessId("test-process");

        service.setParent(mock(AdapterAwareBusinessCockpitService.class));
        service.wire(camundaClient, "test-module", "test-process", true);
    }

    @SuppressWarnings("unchecked")
    @Test
    void userTaskSearchShouldPublishAllUserTasksOfProcessInstanceWithoutPagination() {
        // process instance identified by business key and a call-activity child inheriting it
        final var processInstance = mock(ProcessInstance.class);
        when(processInstance.getProcessInstanceKey()).thenReturn(4711L);
        // the client reports a missing parent as null (a mock would default to 0)
        when(processInstance.getParentProcessInstanceKey()).thenReturn(null);
        final var childInstance = mock(ProcessInstance.class);
        when(childInstance.getParentProcessInstanceKey()).thenReturn(4711L);
        final var processInstanceResponse = (SearchResponse<ProcessInstance>) mock(SearchResponse.class);
        when(processInstanceResponse.items()).thenReturn(List.of(processInstance, childInstance));
        final var processInstanceFuture = (CamundaFuture<SearchResponse<ProcessInstance>>) mock(CamundaFuture.class);
        when(processInstanceFuture.join(anyLong(), any(TimeUnit.class))).thenReturn(processInstanceResponse);
        final var processInstanceRequest = mock(ProcessInstanceSearchRequest.class, RETURNS_SELF);
        when(processInstanceRequest.send()).thenReturn(processInstanceFuture);
        when(camundaClient.newProcessInstanceSearchRequest()).thenReturn(processInstanceRequest);

        // user tasks of that process instance, all returned by one single search request
        final var firstTask = mock(UserTask.class);
        when(firstTask.getUserTaskKey()).thenReturn(111L);
        when(firstTask.getExternalFormReference()).thenReturn("first-form");
        final var secondTask = mock(UserTask.class);
        when(secondTask.getUserTaskKey()).thenReturn(222L);
        when(secondTask.getExternalFormReference()).thenReturn("second-form");
        final var userTaskResponse = (SearchResponse<UserTask>) mock(SearchResponse.class);
        when(userTaskResponse.items()).thenReturn(List.of(firstTask, secondTask));
        final var userTaskFuture = (CamundaFuture<SearchResponse<UserTask>>) mock(CamundaFuture.class);
        when(userTaskFuture.join(anyLong(), any(TimeUnit.class))).thenReturn(userTaskResponse);
        final var userTaskRequest = mock(UserTaskSearchRequest.class, RETURNS_SELF);
        when(userTaskRequest.send()).thenReturn(userTaskFuture);
        when(camundaClient.newUserTaskSearchRequest()).thenReturn(userTaskRequest);

        service.aggregateChanged("business-key-1", "111", "222");

        // only active process instances are searched
        final var processInstanceFilterCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(processInstanceRequest).filter(processInstanceFilterCaptor.capture());
        final var processInstanceFilter = mock(ProcessInstanceFilter.class);
        processInstanceFilterCaptor.getValue().accept(processInstanceFilter);
        verify(processInstanceFilter).state(ProcessInstanceState.ACTIVE);

        // user task search is filtered by the root process instance only, not the child
        final var filterCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(userTaskRequest).filter(filterCaptor.capture());
        final var userTaskFilter = mock(UserTaskFilter.class);
        filterCaptor.getValue().accept(userTaskFilter);
        verify(userTaskFilter).processInstanceKey(4711L);

        // exactly one search request, no pagination
        verify(userTaskRequest, times(1)).send();
        verify(userTaskRequest, never()).page(any(Consumer.class));

        // one event per user task found
        final var eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher, times(2)).publishEvent(eventCaptor.capture());
        final var events = eventCaptor
                .getAllValues()
                .stream()
                .map(Camunda8UserTaskEvent.class::cast)
                .toList();
        assertEquals(List.of(111L, 222L), events.stream().map(Camunda8UserTaskEvent::getUserTaskKey).toList());
        assertEquals(List.of("first-form", "second-form"),
                events.stream().map(Camunda8UserTaskEvent::getTaskDefinition).toList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void workflowSearchShouldIgnoreCallActivityChildren() {
        // root process instance and a call-activity child inheriting the business key
        final var rootInstance = mock(ProcessInstance.class);
        when(rootInstance.getProcessInstanceKey()).thenReturn(4711L);
        // the client reports a missing parent as null (a mock would default to 0)
        when(rootInstance.getParentProcessInstanceKey()).thenReturn(null);
        final var childInstance = mock(ProcessInstance.class);
        when(childInstance.getParentProcessInstanceKey()).thenReturn(4711L);
        final var processInstanceResponse = (SearchResponse<ProcessInstance>) mock(SearchResponse.class);
        when(processInstanceResponse.items()).thenReturn(List.of(rootInstance, childInstance));
        final var processInstanceFuture = (CamundaFuture<SearchResponse<ProcessInstance>>) mock(CamundaFuture.class);
        when(processInstanceFuture.join()).thenReturn(processInstanceResponse);
        final var processInstanceRequest = mock(ProcessInstanceSearchRequest.class, RETURNS_SELF);
        when(processInstanceRequest.send()).thenReturn(processInstanceFuture);
        when(camundaClient.newProcessInstanceSearchRequest()).thenReturn(processInstanceRequest);

        service.aggregateChanged("business-key-1");

        // one event for the root process instance only
        final var eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(4711L, ((Camunda8WorkflowEvent) eventCaptor.getValue()).getProcessInstanceKey());
    }

}
