package io.vanillabp.cockpit.adapter.camunda8.service;

import io.camunda.client.CamundaClient;
import io.vanillabp.cockpit.adapter.camunda8.usertask.Camunda8UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda8.workflow.Camunda8WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.repository.CrudRepository;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class Camunda8BusinessCockpitServiceTest {

    @Mock
    private CrudRepository<TestWorkflowAggregate, Object> workflowAggregateRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private Camunda8WorkflowEventHandler workflowEventHandler;

    @Mock
    private Camunda8UserTaskEventHandler userTaskEventHandler;

    @Mock
    private CamundaClient client;

    @Mock
    private AdapterAwareBusinessCockpitService<TestWorkflowAggregate> parentService;

    private Camunda8BusinessCockpitService<TestWorkflowAggregate> service;

    // Test class for workflow aggregate
    public static class TestWorkflowAggregate {
        private String id;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    @BeforeEach
    void setUp() {
        // Function to get aggregate ID
        final Function<TestWorkflowAggregate, String> getWorkflowAggregateId = TestWorkflowAggregate::getId;

        // Function to parse business key
        final Function<String, Object> parseWorkflowAggregateIdFromBusinessKey = s -> s;

        service = new Camunda8BusinessCockpitService<>(
                workflowAggregateRepository,
                TestWorkflowAggregate.class,
                getWorkflowAggregateId,
                parseWorkflowAggregateIdFromBusinessKey,
                "businessKey",
                applicationEventPublisher,
                workflowEventHandler,
                userTaskEventHandler);
    }

    @Test
    void getWorkflowAggregateClass_returnsCorrectClass() {
        // Get workflow aggregate class
        final var result = service.getWorkflowAggregateClass();

        // Verify
        assertThat(result).isEqualTo(TestWorkflowAggregate.class);
    }

    @Test
    void getWorkflowAggregateRepository_returnsCorrectRepository() {
        // Get workflow aggregate repository
        final var result = service.getWorkflowAggregateRepository();

        // Verify
        assertThat(result).isSameAs(workflowAggregateRepository);
    }

    @Test
    void getWorkflowAggregateIdName_returnsCorrectName() {
        // Get workflow aggregate ID name
        final var result = service.getWorkflowAggregateIdName();

        // Verify
        assertThat(result).isEqualTo("businessKey");
    }

    @Test
    void setParent_setsParentSuccessfully() {
        // Set parent
        service.setParent(parentService);

        // No exception means success
    }

    @Test
    void setBpmnProcessId_setsBpmnProcessId() {
        // Set BPMN process ID
        service.setBpmnProcessId("order-process");

        // No direct getter, so we verify no exception is thrown
    }

    @Test
    void setTenantId_setsTenantId() {
        // Set tenant ID
        service.setTenantId("tenant-1");

        // No direct getter, so we verify no exception is thrown
    }

    @Test
    void wire_withoutParent_throwsException() {
        // Don't set parent

        // Verify that wire throws exception when parent is not set
        assertThatThrownBy(() -> service.wire(client, "test-module", "order-process", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not yet wired!");
    }

    @Test
    void wire_withParent_delegatesToParent() {
        // Set parent first
        service.setParent(parentService);

        // Wire
        service.wire(client, "test-module", "order-process", true);

        // Verify parent is called
        verify(parentService).wire("camunda8", "test-module", "order-process", true);
    }

}
