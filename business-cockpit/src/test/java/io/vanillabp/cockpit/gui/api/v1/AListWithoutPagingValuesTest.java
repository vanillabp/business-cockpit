package io.vanillabp.cockpit.gui.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import io.vanillabp.cockpit.commons.exceptions.RestfulExceptionHandler;
import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.api.v1.AbstractUserTaskListGuiApiController;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.api.v1.AbstractWorkflowListGuiApiController;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * What the lists of the GUI API do with a request which leaves out a paging value. The OpenAPI
 * document calls {@code pageNumber}, {@code pageSize} and {@code sortAscending} optional, so a client
 * which keeps to it may leave each of them out. Each of these requests used to end in a
 * {@code NullPointerException} and HTTP 500.
 * <p>
 * Now a missing page number is the first page and a missing direction is ascending. A missing page
 * size is a mistake of the client and is answered with 400, naming the field. The requests go
 * through Spring MVC, so the bean validation and the cockpit's exception handler are the real
 * ones. The two services are mocks, because what they are called with is the point here.
 */
@ExtendWith(SuppressOutputExtension.class)
class AListWithoutPagingValuesTest {

    /** The interfaces map the paths below this prefix, and an application adds the prefix. */
    private static final String GUI_API = "";

    private UserTaskService userTaskService;

    private WorkflowlistService workflowlistService;

    private MockMvc client;

    static class UserTasks extends AbstractUserTaskListGuiApiController {

        @Override
        protected UserTaskVisibility userTasksVisibleTo(
                final UserDetails currentUser) {
            return UserTaskVisibility.everyUserTask();
        }

    }

    static class Workflows extends AbstractWorkflowListGuiApiController {

        @Override
        protected WorkflowVisibility workflowsVisibleTo(
                final UserDetails currentUser) {
            return WorkflowVisibility.everyWorkflow();
        }

    }

    @BeforeEach
    void setUp() throws Exception {

        final var userContext = mock(UserContext.class);
        when(userContext.getUserLoggedInDetails()).thenReturn(mock(UserDetails.class));
        userTaskService = mock(UserTaskService.class);
        workflowlistService = mock(WorkflowlistService.class);
        when(userTaskService.getUserTasks(any(), anyInt(), anyInt(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(Page.empty());
        when(userTaskService.getUserTasksUpdated(any(), anyInt(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(Page.empty());
        when(workflowlistService.getWorkflows(anyInt(), anyInt(), any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(Page.empty());
        when(workflowlistService.getWorkflowsUpdated(any(), anyInt(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(Page.empty());

        final var userTasks = new UserTasks();
        set(AbstractUserTaskListGuiApiController.class, userTasks, "userContext", userContext);
        set(AbstractUserTaskListGuiApiController.class, userTasks, "userTaskService", userTaskService);
        set(AbstractUserTaskListGuiApiController.class, userTasks, "updateStreams", mock(UpdateStreams.class));
        set(AbstractUserTaskListGuiApiController.class, userTasks, "mapper",
                mock(io.vanillabp.cockpit.tasklist.api.v1.GuiApiMapper.class));

        final var workflows = new Workflows();
        set(AbstractWorkflowListGuiApiController.class, workflows, "userContext", userContext);
        set(AbstractWorkflowListGuiApiController.class, workflows, "workflowlistService", workflowlistService);
        set(AbstractWorkflowListGuiApiController.class, workflows, "updateStreams", mock(UpdateStreams.class));
        set(AbstractWorkflowListGuiApiController.class, workflows, "mapper",
                mock(io.vanillabp.cockpit.workflowlist.api.v1.GuiApiMapper.class));

        final var exceptionHandler = new RestfulExceptionHandler();
        set(RestfulExceptionHandler.class, exceptionHandler, "logger",
                LoggerFactory.getLogger(AListWithoutPagingValuesTest.class));

        client = MockMvcBuilders
                .standaloneSetup(userTasks, workflows)
                .setControllerAdvice(exceptionHandler)
                .build();

    }

    private static void set(
            final Class<?> declaringClass,
            final Object target,
            final String field,
            final Object value) throws Exception {

        final var declaredField = declaringClass.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(target, value);

    }

    private MvcResult send(
            final MockHttpServletRequestBuilder request,
            final String body) throws Exception {

        return client
                .perform(request
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

    }

    @Test
    void aTaskListWithOnlyAPageSizeIsTheFirstPageSortedAscending() throws Exception {

        final var answer = send(post(GUI_API + "/usertask"), "{ \"pageSize\": 10 }");

        assertThat(answer.getResponse().getStatus()).isEqualTo(200);
        verify(userTaskService).getUserTasks(any(), eq(0), eq(10), any(), any(), any(), eq(true), any());

    }

    @Test
    void aTaskListWhichNamesItsValuesGetsThem() throws Exception {

        final var answer = send(
                post(GUI_API + "/usertask"),
                "{ \"pageNumber\": 2, \"pageSize\": 10, \"sortAscending\": false }");

        assertThat(answer.getResponse().getStatus()).isEqualTo(200);
        verify(userTaskService).getUserTasks(any(), eq(2), eq(10), any(), any(), any(), eq(false), any());

    }

    @Test
    void aTaskListWithoutAPageSizeIsRefusedAndTheFieldIsNamed() throws Exception {

        final var answer = send(post(GUI_API + "/usertask"), "{}");

        assertThat(answer.getResponse().getStatus()).isEqualTo(400);
        assertThat(answer.getResponse().getContentAsString())
                .isEqualTo("The request is not valid: 'pageSize' is missing.");
        verifyNoInteractions(userTaskService);

    }

    @Test
    void aPageSizeOfZeroAndANegativePageAreRefusedByTheSchema() throws Exception {

        final var answer = send(post(GUI_API + "/usertask"), "{ \"pageNumber\": -1, \"pageSize\": 0 }");

        assertThat(answer.getResponse().getStatus()).isEqualTo(400);
        assertThat(answer.getResponse().getContentAsString()).isEqualTo(
                "The request is not valid: 'pageNumber' breaks the rule @Min, 'pageSize' breaks the rule @Min.");
        verifyNoInteractions(userTaskService);

    }

    @Test
    void aTaskListUpdateWithoutADirectionSortsAscending() throws Exception {

        final var answer = send(put(GUI_API + "/usertask"), "{ \"size\": 10, \"knownUserTasksIds\": [] }");

        assertThat(answer.getResponse().getStatus()).isEqualTo(200);
        verify(userTaskService).getUserTasksUpdated(any(), eq(10), eq(List.of()), any(), any(), any(), eq(true), any());

    }

    @Test
    void aWorkflowListWithOnlyAPageSizeIsTheFirstPageSortedAscending() throws Exception {

        final var answer = send(post(GUI_API + "/workflow"), "{ \"pageSize\": 10 }");

        assertThat(answer.getResponse().getStatus()).isEqualTo(200);
        verify(workflowlistService).getWorkflows(eq(0), eq(10), any(), any(), any(), any(), any(), eq(true), any());

    }

    @Test
    void aWorkflowListWithoutAPageSizeIsRefusedAndTheFieldIsNamed() throws Exception {

        final var answer = send(post(GUI_API + "/workflow"), "{ \"sortAscending\": false }");

        assertThat(answer.getResponse().getStatus()).isEqualTo(400);
        assertThat(answer.getResponse().getContentAsString())
                .isEqualTo("The request is not valid: 'pageSize' is missing.");
        verifyNoInteractions(workflowlistService);

    }

    @Test
    void aWorkflowListUpdateWithoutADirectionSortsAscending() throws Exception {

        final var answer = send(put(GUI_API + "/workflow"), "{ \"size\": 10, \"knownWorkflowsIds\": [] }");

        assertThat(answer.getResponse().getStatus()).isEqualTo(200);
        verify(workflowlistService).getWorkflowsUpdated(any(), eq(10), eq(List.of()), any(), any(), any(), eq(true), any());

    }

}
