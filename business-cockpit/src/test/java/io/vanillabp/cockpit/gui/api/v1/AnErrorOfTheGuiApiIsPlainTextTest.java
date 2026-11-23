package io.vanillabp.cockpit.gui.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;

import io.vanillabp.cockpit.config.web.GuiApiContentSecurityPolicy;
import io.vanillabp.cockpit.exceptions.RestfulExceptionHandler;
import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.api.v1.AbstractUserTaskListGuiApiController;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the GUI API answers when a query parameter cannot be turned into the type the controller
 * takes. The answer is {@code 400 Bad Request} in plain text, it names the parameter, and it never
 * repeats the value which was sent.
 * <p>
 * The cockpit maps the task list three times, below {@code /gui/api/v1} and below its two views
 * {@code current-user} and {@code user-roles}. All three are covered, because each one is a link a
 * browser can open. Every answer below {@code /gui/api/} also carries a Content Security Policy
 * which allows nothing to run.
 */
@ExtendWith(SuppressOutputExtension.class)
class AnErrorOfTheGuiApiIsPlainTextTest {

  private static final String VALUE_WITH_MARKUP = "<b>yes</b>";

  @RequestMapping(path = "/gui/api/v1")
  static class AllTasks extends AbstractUserTaskListGuiApiController {

    @Override
    protected UserTaskVisibility userTasksVisibleTo(
        final UserDetails currentUser) {
      return UserTaskVisibility.everyUserTask();
    }

  }

  @RequestMapping(path = "/gui/api/v1/current-user")
  static class TasksOfTheCurrentUser extends AllTasks {
  }

  @RequestMapping(path = "/gui/api/v1/user-roles")
  static class TasksOfTheRolesOfTheUser extends AllTasks {
  }

  private MockMvc client;

  @BeforeEach
  void setUp() {

    final var controllers = new AllTasks[]{
        new AllTasks(), new TasksOfTheCurrentUser(), new TasksOfTheRolesOfTheUser()
    };
    for (final var controller : controllers) {
      ReflectionTestUtils.setField(controller, "userContext", mock(UserContext.class));
      ReflectionTestUtils.setField(controller, "userTaskService", mock(UserTaskService.class));
    }

    client = MockMvcBuilders
        .standaloneSetup((Object[]) controllers)
        .setControllerAdvice(new RestfulExceptionHandler())
        .addFilter(new GuiApiContentSecurityPolicy.PolicyFilter(), GuiApiContentSecurityPolicy.GUI_API_PATHS)
        .build();

  }

  @ParameterizedTest
  @ValueSource(strings = {
      "/gui/api/v1", "/gui/api/v1/current-user", "/gui/api/v1/user-roles"
  })
  void aParameterOfTheWrongTypeIsAnsweredAsPlainTextWithoutItsValue(
      final String prefix) throws Exception {

    final var answer = client
        .perform(get(prefix
            + "/usertask/x")
            .queryParam("markAsRead", VALUE_WITH_MARKUP)
            .header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"))
        .andReturn()
        .getResponse();

    assertThat(answer.getStatus()).isEqualTo(400);
    assertThat(MediaType.parseMediaType(answer.getContentType()).isCompatibleWith(MediaType.TEXT_PLAIN)).isTrue();
    assertThat(answer.getContentAsString())
        .isEqualTo("The request is not valid: 'markAsRead' cannot be read.")
        .doesNotContain("<");
    assertThat(answer.getHeader("Content-Security-Policy")).isEqualTo("default-src 'none'; frame-ancestors 'none'");

  }

  @Test
  void thePolicyIsRegisteredForTheGuiApiAheadOfTheSecurityChain() {

    final var registration = new GuiApiContentSecurityPolicy().guiApiContentSecurityPolicyFilter();

    assertThat(registration.getUrlPatterns()).containsExactly("/gui/api/*");
    assertThat(registration.getOrder()).isLessThan(SecurityFilterProperties.DEFAULT_FILTER_ORDER);

  }

}
