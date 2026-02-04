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
        // Event und gemapptes Modul vorbereiten
        final var event = new RegisterWorkflowModuleEvent();
        final var mappedModule = new WorkflowModule();
        when(mapper.toModel(event, "mod-1")).thenReturn(mappedModule);

        // Controller-Aufruf durchfuehren
        final var response = controller.registerWorkflowModule("mod-1", event);

        // Modul muss ueber den Mapper konvertiert und registriert werden
        verify(mapper).toModel(event, "mod-1");
        verify(workflowModuleService).registerWorkflowModule("mod-1", mappedModule);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- userTaskCreatedEvent ---

    @Test
    void userTaskCreatedEvent_mapsEventAndCreatesTask() {
        // Event mit Task-ID vorbereiten
        final var event = new UserTaskCreatedOrUpdatedEvent();
        event.setUserTaskId("task-1");
        final var mappedTask = new UserTask();
        when(mapper.toModel(event)).thenReturn(mappedTask);

        // Controller-Aufruf durchfuehren
        final var response = controller.userTaskCreatedEvent(event);

        // Task muss gemappt und angelegt werden
        verify(mapper).toModel(event);
        verify(taskService).createTask("task-1", mappedTask);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- userTaskUpdatedEvent ---

    @Test
    void userTaskUpdatedEvent_delegatesToUpdateTask() {
        // Event vorbereiten
        final var event = new UserTaskCreatedOrUpdatedEvent();

        // Controller-Aufruf durchfuehren
        final var response = controller.userTaskUpdatedEvent("task-1", event);

        // updateTask muss mit der korrekten Task-ID aufgerufen werden
        verify(taskService).updateTask(eq("task-1"), any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void userTaskUpdatedEvent_withNullEvent_throwsIllegalArgumentException() {
        // Null-Event muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> controller.userTaskUpdatedEvent("task-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    // --- userTaskCompletedEvent ---

    @Test
    void userTaskCompletedEvent_delegatesToCompleteTask() {
        // Event vorbereiten
        final var event = new UserTaskCompletedEvent();

        // Controller-Aufruf durchfuehren
        final var response = controller.userTaskCompletedEvent("task-1", event);

        // completeTask muss aufgerufen werden
        verify(taskService).completeTask(eq("task-1"), any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void userTaskCompletedEvent_withNullEvent_throwsIllegalArgumentException() {
        // Null-Event muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> controller.userTaskCompletedEvent("task-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    // --- userTaskCancelledEvent ---

    @Test
    void userTaskCancelledEvent_delegatesToCancelTask() {
        // Event vorbereiten
        final var event = new UserTaskCancelledEvent();

        // Controller-Aufruf durchfuehren
        final var response = controller.userTaskCancelledEvent("task-1", event);

        // cancelTask muss aufgerufen werden
        verify(taskService).cancelTask(eq("task-1"), any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void userTaskCancelledEvent_withNullEvent_throwsIllegalArgumentException() {
        // Null-Event muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> controller.userTaskCancelledEvent("task-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    // --- workflowCreatedEvent ---

    @Test
    void workflowCreatedEvent_mapsEventAndCreatesWorkflow() {
        // Event mit Workflow-ID vorbereiten
        final var event = new WorkflowCreatedOrUpdatedEvent();
        event.setWorkflowId("wf-1");
        final var mappedWorkflow = new Workflow();
        when(mapper.toModel(event)).thenReturn(mappedWorkflow);

        // Controller-Aufruf durchfuehren
        final var response = controller.workflowCreatedEvent(event);

        // Workflow muss gemappt und angelegt werden
        verify(mapper).toModel(event);
        verify(workflowService).createWorkflow("wf-1", mappedWorkflow);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void workflowCreatedEvent_withNullEvent_throwsIllegalArgumentException() {
        // Null-Event muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> controller.workflowCreatedEvent(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    // --- workflowUpdatedEvent ---

    @Test
    void workflowUpdatedEvent_delegatesToUpdateWorkflow() {
        // Event vorbereiten
        final var event = new WorkflowCreatedOrUpdatedEvent();

        // Controller-Aufruf durchfuehren
        final var response = controller.workflowUpdatedEvent("wf-1", event);

        // updateWorkflow muss aufgerufen werden
        verify(workflowService).updateWorkflow(eq("wf-1"), any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void workflowUpdatedEvent_withNullEvent_throwsIllegalArgumentException() {
        // Null-Event muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> controller.workflowUpdatedEvent("wf-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    // --- workflowCompletedEvent ---

    @Test
    void workflowCompletedEvent_delegatesToCompleteWorkflow() {
        // Event vorbereiten
        final var event = new WorkflowCompletedEvent();

        // Controller-Aufruf durchfuehren
        final var response = controller.workflowCompletedEvent("wf-1", event);

        // completeWorkflow muss aufgerufen werden
        verify(workflowService).completeWorkflow(eq("wf-1"), any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- workflowCancelledEvent ---

    @Test
    void workflowCancelledEvent_delegatesToCancelWorkflow() {
        // Event vorbereiten
        final var event = new WorkflowCancelledEvent();

        // Controller-Aufruf durchfuehren
        final var response = controller.workflowCancelledEvent("wf-1", event);

        // cancelWorkflow muss aufgerufen werden
        verify(workflowService).cancelWorkflow(eq("wf-1"), any());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void workflowCancelledEvent_withNullEvent_throwsIllegalArgumentException() {
        // Null-Event muss zu einer IllegalArgumentException fuehren
        assertThatThrownBy(() -> controller.workflowCancelledEvent("wf-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

}
