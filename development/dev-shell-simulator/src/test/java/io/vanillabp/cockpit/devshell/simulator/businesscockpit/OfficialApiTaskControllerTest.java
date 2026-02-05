package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Page;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
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
class OfficialApiTaskControllerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private OfficialApiMapper mapper;

    private OfficialApiTaskController controller;

    @BeforeEach
    void setUp() {
        // Controller uses constructor injection via @RequiredArgsConstructor
        controller = new OfficialApiTaskController(taskService, mapper);
    }

    // --- getUserTask ---

    @Test
    void getUserTask_returnsApiMappedTask() {
        // Prepare domain task and corresponding API object
        final var domainTask = new UserTask();
        domainTask.setId("task-1");
        final var apiTask = new io.vanillabp.cockpit.gui.api.v1.UserTask();
        apiTask.setId("task-1");

        when(taskService.getUserTask("task-1")).thenReturn(domainTask);
        when(mapper.toApi(domainTask)).thenReturn(apiTask);

        // Execute controller call
        final var response = controller.getUserTask("task-1", false);

        // Response must be HTTP 200 with the mapped API task
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(apiTask);
    }

    @Test
    void getUserTask_delegatesIdToTaskService() {
        // Prepare domain task
        final var domainTask = new UserTask();
        when(taskService.getUserTask("task-42")).thenReturn(domainTask);
        when(mapper.toApi(domainTask)).thenReturn(new io.vanillabp.cockpit.gui.api.v1.UserTask());

        // Execute controller call with specific ID
        controller.getUserTask("task-42", null);

        // TaskService must be called with the correct ID
        verify(taskService).getUserTask("task-42");
    }

    // --- getUserTasks ---

    @Test
    void getUserTasks_returnsPaginatedTasks() {
        // Prepare request with pagination parameters
        final var request = new UserTasksRequest();
        request.setMode(UserTaskRetrieveMode.ALL);
        request.setPageNumber(0);
        request.setPageSize(10);

        // Prepare domain page and API response
        final var domainPage = Page.<UserTask>builder()
                .number(0)
                .size(10)
                .totalPages(1)
                .pageObjects(List.of())
                .build();
        final var apiUserTasks = new UserTasks();

        when(mapper.toModel(UserTaskRetrieveMode.ALL)).thenReturn(TaskService.RetrieveMode.ALL);
        when(taskService.getUserTasks(TaskService.RetrieveMode.ALL, 0, 10)).thenReturn(domainPage);
        when(mapper.toUserTasksApi(domainPage)).thenReturn(apiUserTasks);

        // Execute controller call
        final var response = controller.getUserTasks(request, null);

        // Response must be HTTP 200 with the mapped API pagination
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(apiUserTasks);
    }

    @Test
    void getUserTasks_mapsRetrieveModeToDomainModel() {
        // Prepare request with specific retrieve mode
        final var request = new UserTasksRequest();
        request.setMode(UserTaskRetrieveMode.OPENTASKS);
        request.setPageNumber(0);
        request.setPageSize(20);

        final var domainPage = Page.<UserTask>builder()
                .number(0).size(20).totalPages(0).pageObjects(List.of()).build();

        when(mapper.toModel(UserTaskRetrieveMode.OPENTASKS)).thenReturn(TaskService.RetrieveMode.OPENTASKS);
        when(taskService.getUserTasks(TaskService.RetrieveMode.OPENTASKS, 0, 20)).thenReturn(domainPage);
        when(mapper.toUserTasksApi(domainPage)).thenReturn(new UserTasks());

        // Execute controller call
        controller.getUserTasks(request, null);

        // Retrieve mode must be correctly mapped and passed
        verify(mapper).toModel(UserTaskRetrieveMode.OPENTASKS);
        verify(taskService).getUserTasks(TaskService.RetrieveMode.OPENTASKS, 0, 20);
    }

}
