package io.vanillabp.cockpit.gui.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.vanillabp.cockpit.commons.security.usercontext.UserContext;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.api.v1.AbstractUserTaskListGuiApiController;
import io.vanillabp.cockpit.tasklist.api.v1.GuiApiMapper;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the list of user tasks and its update ask the service for when the request carries no
 * {@code initialTimestamp}. They ask for the tasks which have not ended, without a timestamp, as
 * the count does. "Now" would give the same tasks, but its query has an "or" more, and MongoDB then
 * reads the index entries of every task the user may see, the ended ones included. The answer still
 * carries the time of the server, so the next page can send it back. A request which sends a
 * timestamp gets it passed on unchanged.
 */
@ExtendWith(SuppressOutputExtension.class)
class AListWithoutATimestampTest {

  private static final OffsetDateTime TIMESTAMP = OffsetDateTime.of(2026, 11, 7, 9, 30, 0, 0, ZoneOffset.UTC);

  private UserTaskService userTaskService;

  private GuiApiMapper mapper;

  private MockMvc client;

  static class UserTasks extends AbstractUserTaskListGuiApiController {

    @Override
    protected UserTaskVisibility userTasksVisibleTo(
        final UserDetails currentUser) {
      return UserTaskVisibility.everyUserTask();
    }

  }

  @BeforeEach
  void setUp() throws Exception {

    final var userContext = mock(UserContext.class);
    when(userContext.getUserLoggedInDetails()).thenReturn(mock(UserDetails.class));
    userTaskService = mock(UserTaskService.class);
    when(userTaskService.getUserTasks(any(), anyInt(), anyInt(), any(), any(), any(), anyBoolean(), any()))
        .thenReturn(Page.empty());
    when(userTaskService.getUserTasksUpdated(any(), anyInt(), any(), any(), any(), any(), anyBoolean(), any()))
        .thenReturn(Page.empty());
    mapper = mock(GuiApiMapper.class);

    final var userTasks = new UserTasks();
    set(userTasks, "userContext", userContext);
    set(userTasks, "userTaskService", userTaskService);
    set(userTasks, "updateStreams", mock(UpdateStreams.class));
    set(userTasks, "mapper", mapper);

    client = MockMvcBuilders
        .standaloneSetup(userTasks)
        .build();

  }

  private static void set(
      final Object target,
      final String field,
      final Object value) throws Exception {

    final var declaredField = AbstractUserTaskListGuiApiController.class.getDeclaredField(field);
    declaredField.setAccessible(true);
    declaredField.set(target, value);

  }

  private int send(
      final MockHttpServletRequestBuilder request,
      final String body) throws Exception {

    return client
        .perform(request
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andReturn()
        .getResponse()
        .getStatus();

  }

  @Test
  void theFirstPageWithoutATimestampAsksForTheTasksWhichHaveNotEndedAndAnswersWithTheTime() throws Exception {

    assertThat(send(post("/usertask"), "{ \"pageSize\": 10 }")).isEqualTo(200);

    verify(userTaskService).getUserTasks(any(), eq(0), eq(10), isNull(), any(), any(), anyBoolean(), any());
    verify(mapper).toApi(any(Page.class), notNull(), any());

  }

  @Test
  void theUpdateWithoutATimestampAsksForTheTasksWhichHaveNotEndedAndAnswersWithTheTime() throws Exception {

    assertThat(send(put("/usertask"), "{ \"size\": 10, \"knownUserTasksIds\": [] }")).isEqualTo(200);

    verify(userTaskService).getUserTasksUpdated(any(), eq(10), any(), isNull(), any(), any(), anyBoolean(), any());
    verify(mapper).toApi(any(Page.class), notNull(), any());

  }

  @Test
  void aTimestampIsPassedOnToTheFirstPageAndToTheUpdate() throws Exception {

    assertThat(send(post("/usertask")
        .queryParam("initialTimestamp", TIMESTAMP.toString()), "{ \"pageSize\": 10 }")).isEqualTo(200);
    assertThat(send(put("/usertask")
        .queryParam("initialTimestamp", TIMESTAMP.toString()), "{ \"size\": 10, \"knownUserTasksIds\": [] }"))
        .isEqualTo(200);

    verify(userTaskService).getUserTasks(any(), eq(0), eq(10), eq(TIMESTAMP), any(), any(), anyBoolean(), any());
    verify(userTaskService).getUserTasksUpdated(any(), eq(10), any(), eq(TIMESTAMP), any(), any(), anyBoolean(),
        any());

  }

}
