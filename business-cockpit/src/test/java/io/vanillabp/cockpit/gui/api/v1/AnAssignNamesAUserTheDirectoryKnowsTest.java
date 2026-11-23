package io.vanillabp.cockpit.gui.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import java.util.List;
import java.util.Optional;

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
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.api.v1.AbstractUserTaskListGuiApiController;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An assign through the GUI API makes a person a candidate of the task. The cockpit asks the user
 * directory whether it knows the person, and refuses an id it does not know with
 * {@code 400 Bad Request}. The mapper of the application is not asked, because
 * it may turn any id into a person.
 */
@ExtendWith(SuppressOutputExtension.class)
class AnAssignNamesAUserTheDirectoryKnowsTest {

  private UserTaskService userTaskService;

  private MockMvc client;

  static class UserTasks extends AbstractUserTaskListGuiApiController {

    @Override
    protected UserTaskVisibility userTasksVisibleTo(
        final UserDetails currentUser) {
      return UserTaskVisibility.everyUserTask();
    }

  }

  @BeforeEach
  void setUp() {

    final var userContext = mock(UserContext.class);
    when(userContext.getUserLoggedInDetails()).thenReturn(mock(UserDetails.class));
    userTaskService = mock(UserTaskService.class);
    final var directory = mock(UserDirectoryService.class);
    when(directory.getUser(any())).thenReturn(Optional.empty());
    when(directory.getUser("alice"))
        .thenReturn(Optional.of(new UserDetailsImpl("alice", null, null, null, List.of())));
    // like the mappers the cockpit ships: any id becomes a person
    final var mapper = mock(PersonAndGroupMapper.class);
    when(mapper.toModelPerson(any(String.class))).thenAnswer(invocation -> {
      final var person = new Person();
      person.setId(invocation.getArgument(0));
      return person;
    });
    when(userTaskService.assignTask(any(), eq("t1"), any(Person.class)))
        .thenReturn(new io.vanillabp.cockpit.tasklist.model.UserTask());

    final var userTasks = new UserTasks();
    ReflectionTestUtils.setField(userTasks, "userContext", userContext);
    ReflectionTestUtils.setField(userTasks, "userTaskService", userTaskService);
    ReflectionTestUtils.setField(userTasks, "personAndGroupModelMapper", mapper);
    ReflectionTestUtils.setField(userTasks, "userDirectoryService", directory);

    client = MockMvcBuilders
        .standaloneSetup(userTasks)
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

  @Test
  void anAssignOfOneTaskToAnIdTheDirectoryDoesNotKnowIsRefused() throws Exception {

    final var answer = send("/usertask/t1/assign?userId=nobody-123", "");

    assertThat(answer.getStatus()).isEqualTo(400);
    assertThat(answer.getContentAsString())
        .isEqualTo("The request is not valid: 'userId' names no user the user directory knows.");
    verifyNoInteractions(userTaskService);

  }

  @Test
  void anAssignOfSeveralTasksToAnIdTheDirectoryDoesNotKnowIsRefused() throws Exception {

    final var answer = send("/usertask/assign?userId=nobody-123", "{ \"userTaskIds\": [\"t1\", \"t2\"] }");

    assertThat(answer.getStatus()).isEqualTo(400);
    verifyNoInteractions(userTaskService);

  }

  @Test
  void anAssignWithoutAUserIsRefused() throws Exception {

    assertThat(send("/usertask/t1/assign", "").getStatus()).isEqualTo(400);
    assertThat(send("/usertask/assign", "{ \"userTaskIds\": [\"t1\"] }").getStatus()).isEqualTo(400);
    verifyNoInteractions(userTaskService);

  }

  /** The positive twin: a user the directory knows becomes a candidate. */
  @Test
  void anAssignToAUserTheDirectoryKnowsGoesThrough() throws Exception {

    assertThat(send("/usertask/t1/assign?userId=alice", "").getStatus()).isEqualTo(200);
    assertThat(send("/usertask/assign?userId=alice", "{ \"userTaskIds\": [\"t1\"] }").getStatus()).isEqualTo(200);
    verify(userTaskService).assignTask(any(), anyCollection(), any(Person.class));

  }

  /**
   * Taking a person away is not checked against the directory. A user who left the directory
   * has to be removable from the candidates as well.
   */
  @Test
  void anUnassignOfAnIdTheDirectoryDoesNotKnowGoesThrough() throws Exception {

    assertThat(send("/usertask/assign?unassign=true&userId=gone-456", "{ \"userTaskIds\": [\"t1\"] }").getStatus())
        .isEqualTo(200);
    verify(userTaskService).unassignTask(any(), anyCollection(), eq("gone-456"));

  }

}
