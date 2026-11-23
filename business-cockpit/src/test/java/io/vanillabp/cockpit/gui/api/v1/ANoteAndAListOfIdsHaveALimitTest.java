package io.vanillabp.cockpit.gui.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.vanillabp.cockpit.exceptions.RestfulExceptionHandler;
import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.api.v1.AbstractUserTaskListGuiApiController;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.api.v1.AbstractWorkflowListGuiApiController;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The note of a user task or a case holds at most 10,000 characters, and a request which changes
 * several entries at once names at most 1,000 of them. The OpenAPI document of the GUI API says
 * so, and the cockpit answers a request beyond it with {@code 400 Bad Request} before anything is
 * stored. The services are mocks, because whether they are called at all is the point here.
 */
@ExtendWith(SuppressOutputExtension.class)
class ANoteAndAListOfIdsHaveALimitTest {

  private static final int LONGEST_NOTE = 10_000;

  private static final int MOST_IDS = 1_000;

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
  void setUp() {

    final var userContext = mock(UserContext.class);
    when(userContext.getUserLoggedInDetails()).thenReturn(mock(UserDetails.class));
    userTaskService = mock(UserTaskService.class);
    workflowlistService = mock(WorkflowlistService.class);

    final var userTasks = new UserTasks();
    ReflectionTestUtils.setField(userTasks, "userContext", userContext);
    ReflectionTestUtils.setField(userTasks, "userTaskService", userTaskService);
    ReflectionTestUtils.setField(userTasks, "personAndGroupModelMapper", mock(PersonAndGroupMapper.class));

    final var workflows = new Workflows();
    ReflectionTestUtils.setField(workflows, "userContext", userContext);
    ReflectionTestUtils.setField(workflows, "workflowlistService", workflowlistService);

    client = MockMvcBuilders
        .standaloneSetup(userTasks, workflows)
        .setControllerAdvice(new RestfulExceptionHandler())
        .build();

  }

  private MockHttpServletResponse send(
      final String path,
      final String body) throws Exception {

    return client
        .perform(patch(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andReturn()
        .getResponse();

  }

  private static String text(
      final int length) {

    return "x".repeat(length);

  }

  private static String ids(
      final int count) {

    return IntStream
        .range(0, count)
        .mapToObj(id -> "\"id-%d\"".formatted(id))
        .collect(Collectors.joining(",", "[", "]"));

  }

  @Test
  void aNoteOfSeveralTasksWhichIsTooLongIsRefused() throws Exception {

    final var answer = send("/usertask/note", "{ \"userTaskIds\": [\"t1\"], \"text\": \"%s\" }"
        .formatted(text(LONGEST_NOTE + 1)));

    assertThat(answer.getStatus()).isEqualTo(400);
    assertThat(answer.getContentAsString()).isEqualTo("The request is not valid: 'text' breaks the rule @Size.");
    verifyNoInteractions(userTaskService);

  }

  @Test
  void aNoteOfSeveralCasesWhichIsTooLongIsRefused() throws Exception {

    final var answer = send("/workflow/note", "{ \"workflowIds\": [\"w1\"], \"text\": \"%s\" }"
        .formatted(text(LONGEST_NOTE + 1)));

    assertThat(answer.getStatus()).isEqualTo(400);
    verifyNoInteractions(workflowlistService);

  }

  @Test
  void aNoteOfOneTaskOrCaseWhichIsTooLongIsRefused() throws Exception {

    final var body = "{ \"text\": \"%s\" }".formatted(text(LONGEST_NOTE + 1));

    assertThat(send("/usertask/t1/note", body).getStatus()).isEqualTo(400);
    assertThat(send("/workflow/w1/note", body).getStatus()).isEqualTo(400);
    verifyNoInteractions(userTaskService, workflowlistService);

  }

  @Test
  void aNoteForTooManyEntriesIsRefused() throws Exception {

    assertThat(send("/usertask/note", "{ \"userTaskIds\": %s, \"text\": \"a\" }".formatted(ids(MOST_IDS + 1)))
        .getStatus())
        .isEqualTo(400);
    assertThat(send("/workflow/note", "{ \"workflowIds\": %s, \"text\": \"a\" }".formatted(ids(MOST_IDS + 1)))
        .getStatus())
        .isEqualTo(400);
    verifyNoInteractions(userTaskService, workflowlistService);

  }

  @Test
  void tooManyTasksInOneRequestAreRefused() throws Exception {

    final var body = "{ \"userTaskIds\": %s }".formatted(ids(MOST_IDS + 1));

    assertThat(send("/usertask/mark-as-read", body).getStatus()).isEqualTo(400);
    assertThat(send("/usertask/claim", body).getStatus()).isEqualTo(400);
    assertThat(send("/usertask/assign?unassign=true&userId=u1", body).getStatus()).isEqualTo(400);
    verifyNoInteractions(userTaskService);

  }

  /** The positive twin: a request right at both limits goes through. */
  @Test
  void aNoteAtTheLimitForTheMostEntriesIsStored() throws Exception {

    assertThat(send("/usertask/note", "{ \"userTaskIds\": %s, \"text\": \"%s\" }"
        .formatted(ids(MOST_IDS), text(LONGEST_NOTE))).getStatus())
        .isEqualTo(200);
    assertThat(send("/workflow/note", "{ \"workflowIds\": %s, \"text\": \"%s\" }"
        .formatted(ids(MOST_IDS), text(LONGEST_NOTE))).getStatus())
        .isEqualTo(200);

  }

}
