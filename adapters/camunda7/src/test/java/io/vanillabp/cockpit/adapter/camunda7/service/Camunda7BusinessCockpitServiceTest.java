package io.vanillabp.cockpit.adapter.camunda7.service;

import io.vanillabp.cockpit.adapter.camunda7.usertask.Camunda7UserTaskEventHandler;
import io.vanillabp.cockpit.adapter.camunda7.workflow.Camunda7WorkflowEventHandler;
import io.vanillabp.cockpit.adapter.common.service.AdapterAwareBusinessCockpitService;
import io.vanillabp.spi.cockpit.usertask.UserTask;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.task.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Camunda7BusinessCockpitService}.
 */
@ExtendWith(MockitoExtension.class)
class Camunda7BusinessCockpitServiceTest {

    @Mock
    private ProcessEngine processEngine;

    @Mock
    private TaskService taskService;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private CrudRepository<TestAggregate, Object> repository;

    @Mock
    private Camunda7UserTaskEventHandler userTaskEventHandler;

    @Mock
    private Camunda7WorkflowEventHandler workflowEventHandler;

    @Mock
    private AdapterAwareBusinessCockpitService<TestAggregate> parent;

    @Mock
    private ProcessEngineConfigurationImpl processEngineConfiguration;

    @Mock
    private CommandExecutor commandExecutor;

    private Camunda7BusinessCockpitService<TestAggregate> service;

    private final Function<TestAggregate, ?> getWorkflowAggregateId = TestAggregate::getId;
    private final Function<String, Object> parseBusinessKey = Long::parseLong;

    @BeforeEach
    void setUp() {
        service = new Camunda7BusinessCockpitService<>(
                processEngine,
                taskService,
                runtimeService,
                getWorkflowAggregateId,
                repository,
                TestAggregate.class,
                parseBusinessKey,
                userTaskEventHandler,
                workflowEventHandler
        );
    }

    @Test
    void getWorkflowAggregateClass_returnsCorrectClass() {
        // Act
        Class<TestAggregate> result = service.getWorkflowAggregateClass();

        // Assert
        assertThat(result).isEqualTo(TestAggregate.class);
    }

    @Test
    void getWorkflowAggregateRepository_returnsRepository() {
        // Act
        CrudRepository<TestAggregate, Object> result = service.getWorkflowAggregateRepository();

        // Assert
        assertThat(result).isSameAs(repository);
    }

    @Test
    void getWorkflowAggregateIdFromBusinessKey_parsesBusinessKey() {
        // Act
        Object result = service.getWorkflowAggregateIdFromBusinessKey("12345");

        // Assert
        assertThat(result).isEqualTo(12345L);
    }

    @Test
    void setParent_setsParent() {
        // Act
        service.setParent(parent);

        // Assert - no exception means success
        assertThat(service.getWorkflowAggregateClass()).isEqualTo(TestAggregate.class);
    }

    @Test
    void wire_throwsExceptionIfParentNotSet() {
        // Act & Assert
        assertThatThrownBy(() -> service.wire("module1", "process1", true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not yet wired");
    }

    @Test
    void wire_delegatesToParent() {
        // Arrange
        service.setParent(parent);

        // Act
        service.wire("module1", "process1", true);

        // Assert
        verify(parent).wire("camunda7", "module1", "process1", true);
    }

    @Test
    void aggregateChanged_triggersWorkflowEventForRootProcessInstances() {
        // Arrange
        TestAggregate aggregate = new TestAggregate(123L);
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        ProcessInstance processInstance = mock(ProcessInstance.class);

        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceBusinessKey("123")).thenReturn(query);
        when(query.list()).thenReturn(List.of(processInstance));
        when(processInstance.getRootProcessInstanceId()).thenReturn(null);
        when(processInstance.getId()).thenReturn("PI-456");

        // Act
        service.aggregateChanged(aggregate);

        // Assert
        verify(workflowEventHandler).triggerEventAfterTransaction("PI-456");
    }

    @Test
    void aggregateChanged_filtersNonRootProcessInstances() {
        // Arrange
        TestAggregate aggregate = new TestAggregate(123L);
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        ProcessInstance rootInstance = mock(ProcessInstance.class);
        ProcessInstance subProcessInstance = mock(ProcessInstance.class);

        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceBusinessKey("123")).thenReturn(query);
        when(query.list()).thenReturn(List.of(rootInstance, subProcessInstance));

        // Root process
        when(rootInstance.getRootProcessInstanceId()).thenReturn("PI-100");
        when(rootInstance.getId()).thenReturn("PI-100");

        // Sub-process
        when(subProcessInstance.getRootProcessInstanceId()).thenReturn("PI-100");
        when(subProcessInstance.getId()).thenReturn("PI-200");

        // Act
        service.aggregateChanged(aggregate);

        // Assert
        verify(workflowEventHandler).triggerEventAfterTransaction("PI-100");
        verify(workflowEventHandler, never()).triggerEventAfterTransaction("PI-200");
    }

    @Test
    void aggregateChangedWithUserTasks_notifiesUserTaskHandler() {
        // Arrange
        TestAggregate aggregate = new TestAggregate(123L);
        TaskQuery taskQuery = mock(TaskQuery.class);
        TaskEntity task = mock(TaskEntity.class);

        when(processEngine.getProcessEngineConfiguration()).thenReturn(processEngineConfiguration);
        when(processEngineConfiguration.getCommandExecutorTxRequired()).thenReturn(commandExecutor);
        when(commandExecutor.execute(any(Command.class))).thenAnswer(invocation -> {
            Command<?> command = invocation.getArgument(0);
            return command.execute(null);
        });
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskIdIn("task1", "task2")).thenReturn(taskQuery);
        when(taskQuery.list()).thenReturn(List.of(task));

        // Act
        service.aggregateChanged(aggregate, "task1", "task2");

        // Assert
        verify(userTaskEventHandler).notify(eq(task), eq("update"));
    }

    @Test
    void getUserTask_returnsUserTaskWhenFound() {
        // Arrange
        TestAggregate aggregate = new TestAggregate(123L);
        TaskQuery taskQuery = mock(TaskQuery.class);
        TaskEntity task = mock(TaskEntity.class);
        UserTask mockUserTask = mock(UserTask.class);

        when(processEngine.getProcessEngineConfiguration()).thenReturn(processEngineConfiguration);
        when(processEngineConfiguration.getCommandExecutorTxRequired()).thenReturn(commandExecutor);
        when(commandExecutor.execute(any(Command.class))).thenAnswer(invocation -> {
            Command<?> command = invocation.getArgument(0);
            return command.execute(null);
        });
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId("task-123")).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);
        when(userTaskEventHandler.getUserTask(task)).thenReturn(mockUserTask);

        // Act
        Optional<UserTask> result = service.getUserTask(aggregate, "task-123");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(mockUserTask);
    }

    @Test
    void getUserTask_returnsEmptyWhenNotFound() {
        // Arrange
        TestAggregate aggregate = new TestAggregate(123L);
        TaskQuery taskQuery = mock(TaskQuery.class);

        when(processEngine.getProcessEngineConfiguration()).thenReturn(processEngineConfiguration);
        when(processEngineConfiguration.getCommandExecutorTxRequired()).thenReturn(commandExecutor);
        when(commandExecutor.execute(any(Command.class))).thenAnswer(invocation -> {
            Command<?> command = invocation.getArgument(0);
            return command.execute(null);
        });
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId("nonexistent")).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(null);

        // Act
        Optional<UserTask> result = service.getUserTask(aggregate, "nonexistent");

        // Assert
        assertThat(result).isEmpty();
    }

    /**
     * Test aggregate class.
     */
    static class TestAggregate {
        private final Long id;

        TestAggregate(Long id) {
            this.id = id;
        }

        Long getId() {
            return id;
        }
    }
}
