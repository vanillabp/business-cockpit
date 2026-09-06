package io.vanillabp.cockpit.adapter.camunda8.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.CamundaFuture;
import io.camunda.client.api.search.enums.ProcessInstanceState;
import io.camunda.client.api.search.filter.ProcessInstanceFilter;
import io.camunda.client.api.search.request.ProcessInstanceSearchRequest;
import io.camunda.client.api.search.request.UserTaskSearchRequest;
import io.camunda.client.api.search.response.ProcessInstance;
import io.camunda.client.api.search.response.SearchResponse;
import io.camunda.client.api.search.response.UserTask;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8AggregateChangedEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8UserTaskEvent;
import io.vanillabp.cockpit.adapter.camunda8.receiver.events.Camunda8WorkflowEvent;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

/**
 * Camunda 8 answers "which workflow carries this aggregate id" from its secondary storage, which
 * learns about a workflow only once the exporter has written it there. An application reports a
 * changed aggregate most often right after starting its workflow, which is precisely the moment
 * that storage knows the least, so an empty answer has to be read as "not yet" for as long as the
 * cluster is allowed to need.
 *
 * <p>These tests drive the client's search API through mocks rather than a cluster. An exporter's
 * delay is nothing a test can order a real cluster to produce, while a search answering "nothing"
 * a few times and then reporting the workflow is exactly what such a delay looks like from the
 * adapter's side.
 */
class Camunda8AggregateChangedTest {

    private static final String BPMN_PROCESS_ID = "TestWorkflow";

    private static final String AGGREGATE_ID_NAME = "id";

    private static final String AGGREGATE_ID = "4711";

    private final CamundaClient client = mock(CamundaClient.class);

    private final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);

    private final List<Object> eventsPublished = new ArrayList<>();

    private final ListAppender<ILoggingEvent> logRecorded = new ListAppender<>();

    private Camunda8BusinessCockpitService<Object> service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void wireAServiceForAnAggregateWhoseIdIsFourSevenOneOne() {

        logRecorded.start();
        serviceLogger().addAppender(logRecorded);

        doAnswer(invocation -> eventsPublished.add(invocation.getArgument(0)))
                .when(applicationEventPublisher)
                .publishEvent(any(Object.class));

        service = new Camunda8BusinessCockpitService<>(
                mock(CrudRepository.class),
                Object.class,
                aggregate -> AGGREGATE_ID,
                businessKey -> businessKey,
                AGGREGATE_ID_NAME,
                applicationEventPublisher,
                mock(Camunda8WorkflowEventHandler.class),
                mock(Camunda8UserTaskEventHandler.class));
        service.setParent(mock(AdapterAwareBusinessCockpitService.class));
        service.wire(client, "TestModule", BPMN_PROCESS_ID, true);
        service.setBpmnProcessId(BPMN_PROCESS_ID);
        service.setWorkflowVisibilityTimeout(Duration.ofSeconds(2));

    }

    @AfterEach
    void stopRecordingTheLog() {

        serviceLogger().detachAppender(logRecorded);
        logRecorded.stop();

    }

    @Test
    @DisplayName("Nothing is asked of the cluster while the caller's transaction is still open")
    void theLookupIsDeferredPastTheCallersTransaction() {

        service.aggregateChanged(new Object());

        // the workflow may be started by the very transaction which is still running: VanillaBP's
        // Camunda 8 adapter sends the create-instance command only after the commit
        verify(client, never()).newProcessInstanceSearchRequest();
        assertThat(eventsPublished)
                .singleElement()
                .isInstanceOf(Camunda8AggregateChangedEvent.class);

    }

    @Test
    @DisplayName("A workflow the read model reports late still reaches the cockpit")
    void theWorkflowIsAwaitedWhileTheReadModelDoesNotKnowItYet() {

        letTheProcessInstanceSearchAnswer(
                noProcessInstance(),
                noProcessInstance(),
                theProcessInstance(4242L));

        service.aggregateChanged(new Object());
        theCommitHappens();

        assertThat(workflowEventsPublished())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getProcessInstanceKey()).isEqualTo(4242L);
                    assertThat(event.getBpmnProcessId()).isEqualTo(BPMN_PROCESS_ID);
                    assertThat(event.getVariables()).containsEntry(AGGREGATE_ID_NAME, AGGREGATE_ID);
                });
        assertThat(warningsRecorded()).isEmpty();

    }

    @Test
    @DisplayName("The search asks for running workflows carrying the aggregate id as a JSON value")
    void theSearchIsBuiltTheWayCamundaStoresVariables() {

        letTheProcessInstanceSearchAnswer(theProcessInstance(4242L));
        service.setTenantId("TestModule");

        service.aggregateChanged(new Object());
        theCommitHappens();

        final var filter = mock(ProcessInstanceFilter.class);
        filterBuilt().accept(filter);
        verify(filter).processDefinitionId(BPMN_PROCESS_ID);
        // completed and terminated workflows stay in secondary storage and share the aggregate id
        verify(filter).state(ProcessInstanceState.ACTIVE);
        // Camunda 8 stores variables as JSON, so a string is stored and searched with quotes
        verify(filter).variables(Map.of(AGGREGATE_ID_NAME, "\"" + AGGREGATE_ID + "\""));
        verify(filter).tenantId("TestModule");

    }

    @Test
    @DisplayName("Without the waiting the late workflow is lost, which is the defect behind this fix")
    void theWorkflowIsLostWhenTheAdapterIsNotAllowedToWait() {

        service.setWorkflowVisibilityTimeout(Duration.ZERO);
        letTheProcessInstanceSearchAnswer(
                noProcessInstance(),
                theProcessInstance(4242L));

        service.aggregateChanged(new Object());
        theCommitHappens();

        // one look, nothing there, and the change the cockpit was to receive is gone
        assertThat(workflowEventsPublished()).isEmpty();
        assertThat(warningsRecorded()).hasSize(1);

    }

    @Test
    @DisplayName("A workflow which really is not there ends in a message naming what the cockpit misses")
    void aWorkflowWhichStaysUnknownIsReportedAsTheCockpitLaggingBehind() {

        service.setWorkflowVisibilityTimeout(Duration.ZERO);
        letTheProcessInstanceSearchAnswer(noProcessInstance());

        service.aggregateChanged(new Object());
        theCommitHappens();

        assertThat(workflowEventsPublished()).isEmpty();
        assertThat(warningsRecorded())
                .singleElement()
                .satisfies(warning -> assertThat(warning)
                        .contains("lagging behind")
                        .contains(AGGREGATE_ID)
                        .contains(BPMN_PROCESS_ID)
                        .contains("workflow-visibility-timeout"));

    }

    @Test
    @DisplayName("A user task the read model reports late still reaches the cockpit")
    void aUserTaskIsAwaitedWhileTheReadModelDoesNotKnowItYet() {

        letTheUserTaskSearchAnswer(
                noUserTask(),
                theUserTask(815L));

        service.aggregateChanged(new Object(), "815");
        theCommitHappens();

        assertThat(userTaskEventsPublished())
                .singleElement()
                .satisfies(event -> assertThat(event.getUserTaskKey()).isEqualTo(815L));
        assertThat(warningsRecorded()).isEmpty();

    }

    @Test
    @DisplayName("Of several user tasks the ones found are sent and the ones missing are named")
    void theUserTasksWhichStayUnknownAreNamedWithoutHoldingBackTheOthers() {

        service.setWorkflowVisibilityTimeout(Duration.ZERO);
        // one request per user task the application named, in the order it named them
        letTheUserTaskSearchAnswer(
                theUserTask(815L),
                noUserTask());

        service.aggregateChanged(new Object(), "815", "816");
        theCommitHappens();

        assertThat(userTaskEventsPublished())
                .singleElement()
                .satisfies(event -> assertThat(event.getUserTaskKey()).isEqualTo(815L));
        assertThat(warningsRecorded())
                .singleElement()
                .satisfies(warning -> assertThat(warning)
                        .contains("lagging behind")
                        .contains("816")
                        .contains(AGGREGATE_ID));

    }

    /**
     * What the cockpit's support service does once the caller's transaction committed.
     */
    private void theCommitHappens() {

        eventsPublished
                .stream()
                .filter(Camunda8AggregateChangedEvent.class::isInstance)
                .map(Camunda8AggregateChangedEvent.class::cast)
                .toList()
                .forEach(Camunda8AggregateChangedEvent::findWorkflowsAndNotifyTheCockpit);

    }

    private ch.qos.logback.classic.Logger serviceLogger() {

        return (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Camunda8BusinessCockpitService.class);

    }

    private List<Camunda8WorkflowEvent> workflowEventsPublished() {

        return eventsPublished
                .stream()
                .filter(Camunda8WorkflowEvent.class::isInstance)
                .map(Camunda8WorkflowEvent.class::cast)
                .toList();

    }

    private List<Camunda8UserTaskEvent> userTaskEventsPublished() {

        return eventsPublished
                .stream()
                .filter(Camunda8UserTaskEvent.class::isInstance)
                .map(Camunda8UserTaskEvent.class::cast)
                .toList();

    }

    private List<String> warningsRecorded() {

        return logRecorded
                .list
                .stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();

    }

    private ProcessInstanceSearchRequest processInstanceSearchRequest;

    /**
     * Each answer stands for one attempt of the adapter; the last one is repeated should it ask
     * more often than answers were given.
     */
    @SafeVarargs
    private void letTheProcessInstanceSearchAnswer(
            final List<ProcessInstance>... answers) {

        // the answers are built before the stubbing starts: creating a mock halfway through a
        // when(..) chain leaves Mockito with an unfinished stubbing
        final var responses = searchAnswering(answers);

        processInstanceSearchRequest = mock(ProcessInstanceSearchRequest.class);
        when(client.newProcessInstanceSearchRequest()).thenReturn(processInstanceSearchRequest);
        when(processInstanceSearchRequest.filter(any(Consumer.class)))
                .thenReturn(processInstanceSearchRequest);
        when(processInstanceSearchRequest.send()).thenReturn(responses);

    }

    @SuppressWarnings("unchecked")
    private Consumer<ProcessInstanceFilter> filterBuilt() {

        final var filterBuilt = ArgumentCaptor.forClass(Consumer.class);
        verify(processInstanceSearchRequest).filter(filterBuilt.capture());
        return filterBuilt.getValue();

    }

    @SafeVarargs
    private void letTheUserTaskSearchAnswer(
            final List<UserTask>... answers) {

        final var responses = searchAnswering(answers);

        final var request = mock(UserTaskSearchRequest.class);
        when(client.newUserTaskSearchRequest()).thenReturn(request);
        when(request.filter(any(Consumer.class))).thenReturn(request);
        when(request.send()).thenReturn(responses);

    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    private <T> CamundaFuture<SearchResponse<T>> searchAnswering(
            final List<T>... answers) {

        // every response is built before the answer runs: a mock stubbed inside another mock's
        // answer leaves Mockito with an unfinished stubbing
        final Queue<SearchResponse<T>> remaining = new LinkedList<>();
        for (final var items : answers) {
            final SearchResponse<T> response = mock(SearchResponse.class);
            when(response.items()).thenReturn(items);
            remaining.add(response);
        }

        final CamundaFuture<SearchResponse<T>> future = mock(CamundaFuture.class);
        when(future.join()).thenAnswer(
                invocation -> remaining.size() > 1 ? remaining.poll() : remaining.peek());
        return future;

    }

    private List<ProcessInstance> noProcessInstance() {

        return List.of();

    }

    private List<ProcessInstance> theProcessInstance(
            final long processInstanceKey) {

        final var processInstance = mock(ProcessInstance.class);
        // a root instance has no parent, and Mockito answers a Long-returning method with zero
        // rather than with null, which the adapter would read as a call-activity child
        when(processInstance.getParentProcessInstanceKey()).thenReturn(null);
        when(processInstance.getProcessInstanceKey()).thenReturn(processInstanceKey);
        when(processInstance.getProcessDefinitionId()).thenReturn(BPMN_PROCESS_ID);
        when(processInstance.getProcessDefinitionKey()).thenReturn(1L);
        when(processInstance.getProcessDefinitionVersion()).thenReturn(1);
        return List.of(processInstance);

    }

    private List<UserTask> noUserTask() {

        return List.of();

    }

    private List<UserTask> theUserTask(
            final long userTaskKey) {

        final var userTask = mock(UserTask.class);
        when(userTask.getUserTaskKey()).thenReturn(userTaskKey);
        when(userTask.getBpmnProcessId()).thenReturn(BPMN_PROCESS_ID);
        when(userTask.getProcessDefinitionKey()).thenReturn(1L);
        when(userTask.getProcessInstanceKey()).thenReturn(4242L);
        when(userTask.getProcessDefinitionVersion()).thenReturn(1);
        when(userTask.getElementId()).thenReturn("TheUserTask");
        when(userTask.getExternalFormReference()).thenReturn("TheUserTask");
        return List.of(userTask);

    }

}
