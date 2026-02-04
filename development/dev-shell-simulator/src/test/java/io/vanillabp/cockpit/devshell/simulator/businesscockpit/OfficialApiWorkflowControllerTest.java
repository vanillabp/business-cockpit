package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Page;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import io.vanillabp.cockpit.gui.api.v1.Workflows;
import io.vanillabp.cockpit.gui.api.v1.WorkflowsRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficialApiWorkflowControllerTest {

    @Mock
    private WorkflowService workflowService;

    @Mock
    private TaskService taskService;

    @Mock
    private OfficialApiMapper mapper;

    private OfficialApiWorkflowController controller;

    @BeforeEach
    void setUp() {
        // Controller nutzt Constructor-Injection via @RequiredArgsConstructor
        controller = new OfficialApiWorkflowController(workflowService, taskService, mapper);
    }

    // --- getWorkflow ---

    @Test
    void getWorkflow_returnsApiMappedWorkflow() {
        // Domain-Workflow und API-Workflow vorbereiten
        final var domainWorkflow = new io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow();
        domainWorkflow.setId("wf-1");
        final var apiWorkflow = new io.vanillabp.cockpit.gui.api.v1.Workflow();
        apiWorkflow.setId("wf-1");

        when(workflowService.getWorkflow("wf-1")).thenReturn(domainWorkflow);
        when(mapper.toApi(domainWorkflow)).thenReturn(apiWorkflow);

        // Controller-Aufruf durchfuehren
        final var response = controller.getWorkflow("wf-1");

        // Antwort muss HTTP 200 mit dem gemappten API-Workflow sein
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(apiWorkflow);
    }

    @Test
    void getWorkflow_delegatesIdToWorkflowService() {
        // Domain-Workflow vorbereiten
        final var domainWorkflow = new io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow();
        when(workflowService.getWorkflow("wf-42")).thenReturn(domainWorkflow);
        when(mapper.toApi(domainWorkflow)).thenReturn(new io.vanillabp.cockpit.gui.api.v1.Workflow());

        // Controller-Aufruf mit spezifischer ID
        controller.getWorkflow("wf-42");

        // WorkflowService muss mit der korrekten ID aufgerufen werden
        verify(workflowService).getWorkflow("wf-42");
    }

    // --- getWorkflows ---

    @Test
    void getWorkflows_returnsPaginatedWorkflows() {
        // Request mit Paginierungsparametern vorbereiten
        final var request = new WorkflowsRequest();
        request.setPageNumber(0);
        request.setPageSize(10);

        // Domain-Page und API-Response vorbereiten
        final var domainPage = Page.<io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow>builder()
                .number(0).size(10).totalPages(1).pageObjects(List.of()).build();
        final var apiWorkflows = new Workflows();

        when(workflowService.getWorkflows(0, 10)).thenReturn(domainPage);
        when(mapper.toWorkflowsApi(domainPage)).thenReturn(apiWorkflows);

        // Controller-Aufruf durchfuehren
        final var response = controller.getWorkflows(request, null, null);

        // Antwort muss HTTP 200 mit der gemappten API-Paginierung sein
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(apiWorkflows);
    }

    // --- getUserTasksOfWorkflow ---

    @Test
    void getUserTasksOfWorkflow_returnsFilteredTasks() {
        // Request mit Retrieve-Mode vorbereiten
        final var request = new UserTasksRequest();
        request.setMode(UserTaskRetrieveMode.ALL);

        // Domain-Page und API-UserTasks vorbereiten
        final var taskPage = Page.<UserTask>builder()
                .number(0).size(20).totalPages(1).pageObjects(List.of()).build();
        final var apiUserTasks = new UserTasks();
        apiUserTasks.setUserTasks(List.of());

        when(mapper.toModel(UserTaskRetrieveMode.ALL)).thenReturn(TaskService.RetrieveMode.ALL);
        when(taskService.getUserTasksOfWorkflow("wf-1", TaskService.RetrieveMode.ALL, null, null))
                .thenReturn(taskPage);
        when(mapper.toUserTasksApi(taskPage)).thenReturn(apiUserTasks);

        // Controller-Aufruf durchfuehren
        final var response = controller.getUserTasksOfWorkflow("wf-1", true, request);

        // Antwort muss HTTP 200 mit der UserTask-Liste sein
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(List.of());
    }

    @Test
    void getUserTasksOfWorkflow_passesWorkflowIdToTaskService() {
        // Request vorbereiten
        final var request = new UserTasksRequest();
        request.setMode(UserTaskRetrieveMode.OPENTASKS);

        final var taskPage = Page.<UserTask>builder()
                .number(0).size(20).totalPages(0).pageObjects(List.of()).build();
        final var apiUserTasks = new UserTasks();
        apiUserTasks.setUserTasks(List.of());

        when(mapper.toModel(UserTaskRetrieveMode.OPENTASKS)).thenReturn(TaskService.RetrieveMode.OPENTASKS);
        when(taskService.getUserTasksOfWorkflow("wf-99", TaskService.RetrieveMode.OPENTASKS, null, null))
                .thenReturn(taskPage);
        when(mapper.toUserTasksApi(taskPage)).thenReturn(apiUserTasks);

        // Controller-Aufruf mit spezifischer Workflow-ID
        controller.getUserTasksOfWorkflow("wf-99", false, request);

        // TaskService muss mit der korrekten Workflow-ID aufgerufen werden
        verify(taskService).getUserTasksOfWorkflow("wf-99", TaskService.RetrieveMode.OPENTASKS, null, null);
    }

}
