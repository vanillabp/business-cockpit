package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.bpms.api.v1.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCancelledEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCompletedEvent;
import io.vanillabp.cockpit.bpms.api.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BpmsApiV1ControllerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowModuleService workflowModuleService;

    @Mock
    private OfficialApiMapper mapper;

    @InjectMocks
    private BpmsApiV1Controller controller;

    // --- registerWorkflowModule ---

    @Test
    void registerWorkflowModule_delegatesToServiceViaMapper() {
        // Prepare event and mapped module
        final var event = new RegisterWorkflowModuleEvent();
        final var mappedModule = new WorkflowModule();
        when(mapper.toModel(event, "mod-1")).thenReturn(mappedModule);

        // Execute controller call
        final var response = controller.registerWorkflowModule("mod-1", event);

        // Module must be converted via mapper and registered
        verify(mapper).toModel(event, "mod-1");
        verify(workflowModuleService).registerWorkflowModule("mod-1", mappedModule);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- userTaskCreatedEvent ---

    @Test
    void userTaskCreatedEvent_mapsEventAndCreatesTask() {
        // Prepare event with task ID
        final var event = new UserTaskCreatedOrUpdatedEvent();
        event.setUserTaskId("task-1");
        final var mappedTask = new UserTask();
        when(mapper.toModel(event)).thenReturn(mappedTask);

        // Execute controller call
        final var response = controller.userTaskCreatedEvent(event);

        // Task must be mapped and created
        verify(mapper).toModel(event);
        verify(taskService).createTask("task-1", mappedTask);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- userTaskUpdatedEvent ---

    @Test
    void userTaskUpdatedEvent_delegatesToUpdateTask() {
        // Prepare event
        final var event = new UserTaskCreatedOrUpdatedEvent();

        // Execute controller call
        final var response = controller.userTaskUpdatedEvent("task-1", event);

        // updateTask must be called with correct task ID
        verify(taskService).updateTask(eq("task-1"), any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void userTaskUpdatedEvent_withNullEvent_throwsIllegalArgumentException() {
        // Null event must throw IllegalArgumentException
        assertThatThrownBy(() -> controller.userTaskUpdatedEvent("task-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    // --- userTaskCompletedEvent ---

    @Test
    void userTaskCompletedEvent_delegatesToCompleteTask() {
        // Prepare event
        final var event = new UserTaskCompletedEvent();

        // Execute controller call
        final var response = controller.userTaskCompletedEvent("task-1", event);

        // completeTask must be called
        verify(taskService).completeTask(eq("task-1"), any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void userTaskCompletedEvent_withNullEvent_throwsIllegalArgumentException() {
        // Null event must throw IllegalArgumentException
        assertThatThrownBy(() -> controller.userTaskCompletedEvent("task-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    // --- userTaskCancelledEvent ---

    @Test
    void userTaskCancelledEvent_delegatesToCancelTask() {
        // Prepare event
        final var event = new UserTaskCancelledEvent();

        // Execute controller call
        final var response = controller.userTaskCancelledEvent("task-1", event);

        // cancelTask must be called
        verify(taskService).cancelTask(eq("task-1"), any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void userTaskCancelledEvent_withNullEvent_throwsIllegalArgumentException() {
        // Null event must throw IllegalArgumentException
        assertThatThrownBy(() -> controller.userTaskCancelledEvent("task-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    // --- workflowCreatedEvent ---

    @Test
    void workflowCreatedEvent_mapsEventAndCreatesWorkflow() {
        // Prepare event with workflow ID
        final var event = new WorkflowCreatedOrUpdatedEvent();
        event.setWorkflowId("wf-1");
        final var mappedWorkflow = new Workflow();
        when(mapper.toModel(event)).thenReturn(mappedWorkflow);

        // Execute controller call
        final var response = controller.workflowCreatedEvent(event);

        // Workflow must be mapped and created
        verify(mapper).toModel(event);
        verify(workflowService).createWorkflow("wf-1", mappedWorkflow);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void workflowCreatedEvent_withNullEvent_throwsIllegalArgumentException() {
        // Null event must throw IllegalArgumentException
        assertThatThrownBy(() -> controller.workflowCreatedEvent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    // --- workflowUpdatedEvent ---

    @Test
    void workflowUpdatedEvent_delegatesToUpdateWorkflow() {
        // Prepare event
        final var event = new WorkflowCreatedOrUpdatedEvent();

        // Execute controller call
        final var response = controller.workflowUpdatedEvent("wf-1", event);

        // updateWorkflow must be called
        verify(workflowService).updateWorkflow(eq("wf-1"), any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void workflowUpdatedEvent_withNullEvent_throwsIllegalArgumentException() {
        // Null event must throw IllegalArgumentException
        assertThatThrownBy(() -> controller.workflowUpdatedEvent("wf-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    // --- workflowCompletedEvent ---

    @Test
    void workflowCompletedEvent_delegatesToCompleteWorkflow() {
        // Prepare event
        final var event = new WorkflowCompletedEvent();

        // Execute controller call
        final var response = controller.workflowCompletedEvent("wf-1", event);

        // completeWorkflow must be called
        verify(workflowService).completeWorkflow(eq("wf-1"), any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- workflowCancelledEvent ---

    @Test
    void workflowCancelledEvent_delegatesToCancelWorkflow() {
        // Prepare event
        final var event = new WorkflowCancelledEvent();

        // Execute controller call
        final var response = controller.workflowCancelledEvent("wf-1", event);

        // cancelWorkflow must be called
        verify(workflowService).cancelWorkflow(eq("wf-1"), any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void workflowCancelledEvent_withNullEvent_throwsIllegalArgumentException() {
        // Null event must throw IllegalArgumentException
        assertThatThrownBy(() -> controller.workflowCancelledEvent("wf-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

}
