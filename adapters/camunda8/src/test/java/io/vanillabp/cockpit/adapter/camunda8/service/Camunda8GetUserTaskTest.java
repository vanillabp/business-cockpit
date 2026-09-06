package io.vanillabp.cockpit.adapter.camunda8.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.ProblemDetail;
import io.camunda.client.api.command.ProblemException;
import io.camunda.client.api.fetch.UserTaskGetRequest;
import io.camunda.client.api.search.response.UserTask;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskWiring;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskEventImpl;
import io.vanillabp.spi.cockpit.details.DetailsEvent;
import java.net.HttpURLConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

/**
 * Asking Camunda 8 for one user task reads the same secondary storage as the searches of
 * {@link Camunda8AggregateChangedTest}, so a task created moments ago may not be there yet. The
 * client reports such a task as "not found" instead of answering with nothing, and to the caller
 * of the cockpit service that is an answer, not a failure: it asked whether there is a user task
 * and the honest reply is that there is none to be had. Anything else the cluster says is a
 * failure and stays one.
 *
 * <p>These tests drive the client's get request through mocks. A cluster cannot be ordered to
 * forget a user task, whereas the request answering "not found" is exactly what that looks like
 * from the adapter's side.
 */
class Camunda8GetUserTaskTest {

    private static final String BPMN_PROCESS_ID = "TestWorkflow";

    private static final String AGGREGATE_ID_NAME = "id";

    private static final String AGGREGATE_ID = "4711";

    private static final String USER_TASK_ID = "815";

    private final CamundaClient client = mock(CamundaClient.class);

    private final UserTaskGetRequest userTaskGetRequest = mock(UserTaskGetRequest.class);

    private final Camunda8UserTaskEventHandler userTaskEventHandler =
            mock(Camunda8UserTaskEventHandler.class);

    private Camunda8BusinessCockpitService<Object> service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void wireAServiceForAnAggregateWhoseIdIsFourSevenOneOne() {

        when(client.newUserTaskGetRequest(Long.parseLong(USER_TASK_ID)))
                .thenReturn(userTaskGetRequest);

        service = new Camunda8BusinessCockpitService<>(
                mock(CrudRepository.class),
                Object.class,
                aggregate -> AGGREGATE_ID,
                businessKey -> businessKey,
                AGGREGATE_ID_NAME,
                mock(ApplicationEventPublisher.class),
                mock(Camunda8WorkflowEventHandler.class),
                userTaskEventHandler);
        service.setParent(mock(AdapterAwareBusinessCockpitService.class));
        service.wire(client, "TestModule", BPMN_PROCESS_ID, true);
        service.setBpmnProcessId(BPMN_PROCESS_ID);

    }

    @Test
    @DisplayName("A user task the cluster does not know about is answered as no user task")
    void anUnknownUserTaskIsAnsweredEmpty() {

        letTheGetRequestFailWith(HttpURLConnection.HTTP_NOT_FOUND, "Not Found");

        assertThat(service.getUserTask(new Object(), USER_TASK_ID)).isEmpty();

    }

    @Test
    @DisplayName("Anything else the cluster reports reaches the caller instead of hiding as an empty answer")
    void anErrorOtherThanAnUnknownUserTaskIsPassedOn() {

        letTheGetRequestFailWith(HttpURLConnection.HTTP_UNAVAILABLE, "Service Unavailable");

        assertThatThrownBy(() -> service.getUserTask(new Object(), USER_TASK_ID))
                .isInstanceOf(ProblemException.class)
                .satisfies(thrown ->
                        assertThat(((ProblemException) thrown).code())
                                .isEqualTo(HttpURLConnection.HTTP_UNAVAILABLE));

    }

    @Test
    @DisplayName("A user task the cluster knows is mapped and handed to the caller")
    void aUserTaskWhichIsThereIsMapped() {

        letTheGetRequestAnswerWithAUserTask();
        letTheEventHandlerMapItTo(aMappedUserTaskEvent());

        final var userTask = service.getUserTask(new Object(), USER_TASK_ID);

        assertThat(userTask)
                .isPresent()
                .get()
                .satisfies(found -> assertThat(found.getId()).isEqualTo(USER_TASK_ID));

        final var event = theEventTheHandlerWasGiven();
        assertThat(event.getUserTaskKey()).isEqualTo(Long.parseLong(USER_TASK_ID));
        assertThat(event.getBpmnProcessId()).isEqualTo(BPMN_PROCESS_ID);
        assertThat(event.getProcessInstanceKey()).isEqualTo(4242L);
        assertThat(event.getEvent()).isEqualTo(DetailsEvent.Event.UPDATED);
        assertThat(event.getVariables()).containsEntry(AGGREGATE_ID_NAME, AGGREGATE_ID);
        // the form reference carries the job type of the details provider, the cockpit wants the
        // task definition alone
        assertThat(event.getTaskDefinition()).isEqualTo("TheUserTask");

    }

    private void letTheGetRequestFailWith(
            final int code,
            final String reason) {

        when(userTaskGetRequest.execute())
                .thenThrow(new ProblemException(
                        code, reason, new ProblemDetail().setStatus(code).setTitle(reason)));

    }

    private void letTheGetRequestAnswerWithAUserTask() {

        final var userTask = mock(UserTask.class);
        when(userTask.getUserTaskKey()).thenReturn(Long.parseLong(USER_TASK_ID));
        when(userTask.getBpmnProcessId()).thenReturn(BPMN_PROCESS_ID);
        when(userTask.getProcessDefinitionKey()).thenReturn(1L);
        when(userTask.getProcessInstanceKey()).thenReturn(4242L);
        when(userTask.getProcessDefinitionVersion()).thenReturn(1);
        when(userTask.getElementId()).thenReturn("TheUserTask");
        when(userTask.getExternalFormReference())
                .thenReturn(Camunda8UserTaskWiring.JOBTYPE_DETAILSPROVIDER + "TheUserTask");
        when(userTaskGetRequest.execute()).thenReturn(userTask);

    }

    private UserTaskEventImpl aMappedUserTaskEvent() {

        final var mapped = mock(UserTaskEventImpl.class);
        when(mapped.getUserTaskId()).thenReturn(USER_TASK_ID);
        return mapped;

    }

    private void letTheEventHandlerMapItTo(
            final UserTaskEventImpl mapped) {

        when(userTaskEventHandler.getUserTaskEvent(any(), any())).thenReturn(mapped);

    }

    private Camunda8UserTaskEvent theEventTheHandlerWasGiven() {

        final var eventMapped = ArgumentCaptor.forClass(Camunda8UserTaskEvent.class);
        verify(userTaskEventHandler).getUserTaskEvent(eventMapped.capture(), any());
        return eventMapped.getValue();

    }

}
