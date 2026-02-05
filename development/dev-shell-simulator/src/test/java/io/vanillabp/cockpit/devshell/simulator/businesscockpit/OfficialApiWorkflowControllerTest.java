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
        // Controller uses constructor injection via @RequiredArgsConstructor
        controller = new OfficialApiWorkflowController(workflowService, taskService, mapper);
    }

    // --- getWorkflow ---

    @Test
    void getWorkflow_returnsApiMappedWorkflow() {
        // Prepare domain workflow and API workflow
        final var domainWorkflow = new io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow();
        domainWorkflow.setId("wf-1");
        final var apiWorkflow = new io.vanillabp.cockpit.gui.api.v1.Workflow();
        apiWorkflow.setId("wf-1");

        when(workflowService.getWorkflow("wf-1")).thenReturn(domainWorkflow);
        when(mapper.toApi(domainWorkflow)).thenReturn(apiWorkflow);

        // Execute controller call
        final var response = controller.getWorkflow("wf-1");

        // Response must be HTTP 200 with the mapped API workflow
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(apiWorkflow);
    }

    @Test
    void getWorkflow_delegatesIdToWorkflowService() {
        // Prepare domain workflow
        final var domainWorkflow = new io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow();
        when(workflowService.getWorkflow("wf-42")).thenReturn(domainWorkflow);
        when(mapper.toApi(domainWorkflow)).thenReturn(new io.vanillabp.cockpit.gui.api.v1.Workflow());

        // Execute controller call with specific ID
        controller.getWorkflow("wf-42");

        // WorkflowService must be called with the correct ID
        verify(workflowService).getWorkflow("wf-42");
    }

    // --- getWorkflows ---

    @Test
    void getWorkflows_returnsPaginatedWorkflows() {
        // Prepare request with pagination parameters
        final var request = new WorkflowsRequest();
        request.setPageNumber(0);
        request.setPageSize(10);

        // Prepare domain page and API response
        final var domainPage = Page.<io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow>builder()
                .number(0).size(10).totalPages(1).pageObjects(List.of()).build();
        final var apiWorkflows = new Workflows();

        when(workflowService.getWorkflows(0, 10)).thenReturn(domainPage);
        when(mapper.toWorkflowsApi(domainPage)).thenReturn(apiWorkflows);

        // Execute controller call
        final var response = controller.getWorkflows(request, null, null);

        // Response must be HTTP 200 with the mapped API pagination
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(apiWorkflows);
    }

    // --- getUserTasksOfWorkflow ---

    @Test
    void getUserTasksOfWorkflow_returnsFilteredTasks() {
        // Prepare request with retrieve mode
        final var request = new UserTasksRequest();
        request.setMode(UserTaskRetrieveMode.ALL);

        // Prepare domain page and API UserTasks
        final var taskPage = Page.<UserTask>builder()
                .number(0).size(20).totalPages(1).pageObjects(List.of()).build();
        final var apiUserTasks = new UserTasks();
        apiUserTasks.setUserTasks(List.of());

        when(mapper.toModel(UserTaskRetrieveMode.ALL)).thenReturn(TaskService.RetrieveMode.ALL);
        when(taskService.getUserTasksOfWorkflow("wf-1", TaskService.RetrieveMode.ALL, null, null))
                .thenReturn(taskPage);
        when(mapper.toUserTasksApi(taskPage)).thenReturn(apiUserTasks);

        // Execute controller call
        final var response = controller.getUserTasksOfWorkflow("wf-1", true, request);

        // Response must be HTTP 200 with the UserTask list
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(List.of());
    }

    @Test
    void getUserTasksOfWorkflow_passesWorkflowIdToTaskService() {
        // Prepare request
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

        // Execute controller call with specific workflow ID
        controller.getUserTasksOfWorkflow("wf-99", false, request);

        // TaskService must be called with the correct workflow ID
        verify(taskService).getUserTasksOfWorkflow("wf-99", TaskService.RetrieveMode.OPENTASKS, null, null);
    }

}
