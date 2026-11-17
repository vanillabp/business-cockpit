package io.vanillabp.cockpit.gui.api.v1.client.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockWebServer;

/**
 * An application which adds the dependency, sets the address of the cockpit in Spring Boot's place
 * for the group and injects an interface. Inside a request the user's cookie goes along.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
public class TheClientsAreBuiltFromKeysTest {

  static final MockWebServer cockpit = new MockWebServer();

  @DynamicPropertySource
  static void urls(
      final DynamicPropertyRegistry registry) {
    registry.add("spring.http.serviceclient.official-gui-api.base-url", () -> Cockpit.guiApiUrl(cockpit));
  }

  @AfterAll
  static void stop() {
    cockpit.close();
  }

  @AfterEach
  void leaveTheRequest() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Autowired
  OfficialTasklistApi tasklist;

  @Test
  public void a_user_task_is_read_from_the_cockpit() {

    cockpit.enqueue(Cockpit.userTask("task-1", "jane"));

    final var userTask = tasklist.getUserTask("task-1", null).getBody();

    assertThat(userTask.getAssignee().getId()).isEqualTo("jane");
    final var request = Cockpit.next(cockpit);
    assertThat(request.getTarget()).isEqualTo("/gui/api/v1/usertask/task-1");
    assertThat(request.getHeaders().get("Cookie")).isNull();

  }

  @Test
  public void inside_a_request_the_users_cookie_goes_along() {

    final var usersRequest = new MockHttpServletRequest();
    usersRequest.addHeader("Cookie", "bc-token=signed-login");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(usersRequest));
    cockpit.enqueue(Cockpit.userTask("task-1", "jane"));

    tasklist.getUserTask("task-1", null);

    assertThat(Cockpit.next(cockpit).getHeaders().get("Cookie")).isEqualTo("bc-token=signed-login");

  }

}
