package io.vanillabp.cockpit.gui.api.v1.client.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceGroupPrefix;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockWebServer;

/**
 * The application reads the keys of the cockpit's client below a prefix of its choice, here
 * {@code loan-approval.cockpit}. It imports the interface it needs into a group of its own and
 * gives that group the prefix. A cookie set by a key wins over the one of the user's request.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = {
        TestApplication.class, TheApplicationChoosesThePrefixTest.LoanApproval.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "loan-approval.cockpit.default-header.X-Workflow-Module=loan-approval", "loan-approval.cockpit.default-header.Cookie=bc-token=of-the-module",
    })
@DirtiesContext
public class TheApplicationChoosesThePrefixTest {

  static final MockWebServer cockpit = new MockWebServer();

  @DynamicPropertySource
  static void urls(
      final DynamicPropertyRegistry registry) {
    registry.add("loan-approval.cockpit.base-url", () -> Cockpit.guiApiUrl(cockpit));
  }

  @AfterAll
  static void stop() {
    cockpit.close();
  }

  @Configuration
  @ImportHttpServices(group = "loan-approval-cockpit", types = OfficialTasklistApi.class)
  static class LoanApproval {

    @Bean
    HttpServiceGroupPrefix cockpitKeys() {
      return HttpServiceGroupPrefix.of("loan-approval-cockpit", "loan-approval.cockpit");
    }

  }

  @Autowired
  OfficialTasklistApi tasklist;

  @Test
  public void the_keys_below_the_chosen_prefix_apply() {

    cockpit.enqueue(Cockpit.userTask("task-1", "jane"));

    tasklist.getUserTask("task-1", null);

    assertThat(Cockpit.next(cockpit).getHeaders().get("X-Workflow-Module")).isEqualTo("loan-approval");

  }

  @Test
  public void a_cookie_of_the_keys_wins_over_the_one_of_the_users_request() {

    final var usersRequest = new MockHttpServletRequest();
    usersRequest.addHeader("Cookie", "bc-token=signed-login");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(usersRequest));
    try {
      cockpit.enqueue(Cockpit.userTask("task-1", "jane"));

      tasklist.getUserTask("task-1", null);

      assertThat(Cockpit.next(cockpit).getHeaders().get("Cookie")).isEqualTo("bc-token=of-the-module");
    } finally {
      RequestContextHolder.resetRequestAttributes();
    }

  }

}
