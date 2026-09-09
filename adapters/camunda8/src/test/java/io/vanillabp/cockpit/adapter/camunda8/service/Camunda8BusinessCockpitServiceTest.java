package io.vanillabp.cockpit.adapter.camunda8.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.page.AnyPage;
import io.camunda.client.api.search.request.UserTaskSearchRequest;
import io.camunda.client.api.search.response.SearchResponse;
import io.camunda.client.api.search.response.SearchResponsePage;
import io.camunda.client.api.search.response.UserTask;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

@ExtendWith(MockitoExtension.class)
class Camunda8BusinessCockpitServiceTest {

    @Mock
    private CamundaClient camundaClient;

    private Camunda8BusinessCockpitService<String> service;

    @BeforeEach
    void setup() {
        service = new Camunda8BusinessCockpitService<String>(
                mock(CrudRepository.class),
                String.class,
                businessKey -> businessKey,
                businessKey -> businessKey,
                "id",
                mock(ApplicationEventPublisher.class),
                mock(Camunda8WorkflowEventHandler.class),
                mock(Camunda8UserTaskEventHandler.class));
        service.setBpmnProcessId("test-process");

        service.setParent(mock(AdapterAwareBusinessCockpitService.class));
        service.wire(camundaClient, "test-module", "test-process", true);
    }

    @SuppressWarnings("unchecked")
    @Test
    void userTaskSearchShouldConsiderAllPages() {
        final var request = mock(UserTaskSearchRequest.class, RETURNS_SELF);
        when(camundaClient.newUserTaskSearchRequest()).thenReturn(request);

        // page 1: does not contain the requested user task, but has a next page
        final var otherTask = mock(UserTask.class);
        when(otherTask.getUserTaskKey()).thenReturn(111L);
        final var page1 = mock(SearchResponsePage.class);
        when(page1.endCursor()).thenReturn("cursor-after-page-1");
        final var page1Response = (SearchResponse<UserTask>) mock(SearchResponse.class);
        when(page1Response.items()).thenReturn(List.of(otherTask));
        when(page1Response.page()).thenReturn(page1);

        // page 2: only reachable via after("cursor-after-page-1"), contains the requested task
        final var wantedTask = mock(UserTask.class);
        when(wantedTask.getUserTaskKey()).thenReturn(222L);
        when(wantedTask.getExternalFormReference()).thenReturn("some-form");
        final var page2 = mock(SearchResponsePage.class);
        final var page2Response = (SearchResponse<UserTask>) mock(SearchResponse.class);
        when(page2Response.items()).thenReturn(List.of(wantedTask));
        when(page2Response.page()).thenReturn(page2);

        // fake backend: records which cursor the production code asked for via page(...)
        final var requestedAfterCursor = new AtomicReference<String>();
        final var pageStep = mock(AnyPage.class);
        doAnswer(invocation -> {
            requestedAfterCursor.set(invocation.getArgument(0));
            return null;
        }).when(pageStep).after(anyString());

        when(request.page(any(Consumer.class))).thenAnswer(invocation -> {
            requestedAfterCursor.set(null);
            final Consumer<AnyPage> pageConsumer = invocation.getArgument(0);
            pageConsumer.accept(pageStep);
            return request;
        });

        // fake backend: returns page 2 only if the code progressed the cursor, page 1 otherwise
        final var executeCallCount = new AtomicInteger();
        when(request.execute()).thenAnswer(invocation -> {
            if (executeCallCount.incrementAndGet() > 5) {
                throw new AssertionError(
                        "aggregateChanged() called execute() more than 5 times: pagination is "
                                + "stuck on the same page instead of advancing via after()/from()");
            }
            return "cursor-after-page-1".equals(requestedAfterCursor.get())
                    ? page2Response
                    : page1Response;
        });

        service.aggregateChanged("business-key-1", "222");

        verify(pageStep).after("cursor-after-page-1");
        assertEquals(2, executeCallCount.get(),
                "expected exactly one search request per page (page 1, then page 2)");
    }

}
