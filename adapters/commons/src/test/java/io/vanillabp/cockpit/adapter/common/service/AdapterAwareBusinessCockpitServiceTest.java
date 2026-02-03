package io.vanillabp.cockpit.adapter.common.service;

import io.vanillabp.spi.cockpit.usertask.UserTask;
import io.vanillabp.springboot.adapter.VanillaBpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdapterAwareBusinessCockpitServiceTest {

    @Mock
    private VanillaBpProperties properties;

    @Mock
    private BusinessCockpitServiceImplementation<TestAggregate> camunda7Service;

    @Mock
    private BusinessCockpitServiceImplementation<TestAggregate> camunda8Service;

    @Mock
    private CrudRepository<TestAggregate, String> repository;

    @Mock
    private UserTask userTask;

    private AdapterAwareBusinessCockpitService<TestAggregate> service;

    @BeforeEach
    void setUp() {
        Map<String, BusinessCockpitServiceImplementation<TestAggregate>> servicesByAdapter = new HashMap<>();
        servicesByAdapter.put("camunda7", camunda7Service);
        servicesByAdapter.put("camunda8", camunda8Service);

        service = new AdapterAwareBusinessCockpitService<>(
                properties,
                servicesByAdapter,
                String.class,
                TestAggregate.class);
    }

    @Test
    void constructor_setsParentOnAllAdapters() {
        verify(camunda7Service).setParent(service);
        verify(camunda8Service).setParent(service);
    }

    @Test
    void getWorkflowAggregateIdClass_returnsClass() {
        assertThat(service.getWorkflowAggregateIdClass()).isEqualTo(String.class);
    }

    @Test
    void getWorkflowAggregateClass_returnsClass() {
        assertThat(service.getWorkflowAggregateClass()).isEqualTo(TestAggregate.class);
    }

    @Test
    void getWorkflowModuleId_initiallyNull() {
        assertThat(service.getWorkflowModuleId()).isNull();
    }

    @Test
    void getPrimaryBpmnProcessId_initiallyNull() {
        assertThat(service.getPrimaryBpmnProcessId()).isNull();
    }

    @Test
    void getBpmnProcessIds_initiallyEmpty() {
        assertThat(service.getBpmnProcessIds()).isEmpty();
    }

    @Test
    void wire_setsWorkflowModuleId() {
        service.wire("camunda7", "test-module", "process-1", true);

        assertThat(service.getWorkflowModuleId()).isEqualTo("test-module");
    }

    @Test
    void wire_withPrimary_setsPrimaryBpmnProcessId() {
        service.wire("camunda7", "test-module", "process-1", true);

        assertThat(service.getPrimaryBpmnProcessId()).isEqualTo("process-1");
    }

    @Test
    void wire_addsBpmnProcessId() {
        service.wire("camunda7", "test-module", "process-1", false);

        assertThat(service.getBpmnProcessIds()).contains("process-1");
    }

    @Test
    void wire_withDifferentWorkflowModuleId_throwsException() {
        service.wire("camunda7", "module-1", "process-1", true);

        assertThatThrownBy(() -> service.wire("camunda8", "module-2", "process-1", false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Wiring the workflowModuleId");
    }

    @Test
    void wire_withSameWorkflowModuleId_succeeds() {
        service.wire("camunda7", "test-module", "process-1", true);
        service.wire("camunda8", "test-module", "process-2", false);

        assertThat(service.getWorkflowModuleId()).isEqualTo("test-module");
        assertThat(service.getBpmnProcessIds()).containsExactlyInAnyOrder("process-1", "process-2");
    }

    @Test
    void getWorkflowAggregateRepository_returnsNullWhenNoAdaptersHaveRepository() {
        // The service iterates through adapters but since we haven't set up stubs,
        // the first adapter returns null
        CrudRepository<TestAggregate, ?> result = service.getWorkflowAggregateRepository();

        assertThat(result).isNull();
    }

    @Test
    void aggregateChanged_delegatesToPrimaryAdapter() {
        when(properties.getDefaultAdapterFor("test-module", "process-1")).thenReturn(Arrays.asList("camunda7"));

        service.wire("camunda7", "test-module", "process-1", true);
        service.wire("camunda8", "test-module", "process-1", false);

        TestAggregate aggregate = new TestAggregate();
        service.aggregateChanged(aggregate);

        verify(camunda7Service).aggregateChanged(aggregate);
    }

    @Test
    void aggregateChangedWithUserTaskIds_delegatesToPrimaryAdapter() {
        when(properties.getDefaultAdapterFor("test-module", "process-1")).thenReturn(Arrays.asList("camunda7"));

        service.wire("camunda7", "test-module", "process-1", true);
        service.wire("camunda8", "test-module", "process-1", false);

        TestAggregate aggregate = new TestAggregate();
        service.aggregateChanged(aggregate, "task1", "task2");

        verify(camunda7Service).aggregateChanged(aggregate, "task1", "task2");
    }

    @Test
    void getUserTask_delegatesToPrimaryAdapter() {
        when(properties.getDefaultAdapterFor("test-module", "process-1")).thenReturn(Arrays.asList("camunda7"));
        when(camunda7Service.getUserTask(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("task-1")))
                .thenReturn(Optional.of(userTask));

        service.wire("camunda7", "test-module", "process-1", true);
        service.wire("camunda8", "test-module", "process-1", false);

        TestAggregate aggregate = new TestAggregate();
        Optional<UserTask> result = service.getUserTask(aggregate, "task-1");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(userTask);
    }

    @Test
    void wire_validatesPropertiesWhenAllAdaptersWired() {
        service.wire("camunda7", "test-module", "process-1", true);
        service.wire("camunda8", "test-module", "process-1", false);

        verify(properties).validatePropertiesFor(
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.eq("test-module"),
                org.mockito.ArgumentMatchers.eq("process-1"));
    }

    private static class TestAggregate {
    }
}
