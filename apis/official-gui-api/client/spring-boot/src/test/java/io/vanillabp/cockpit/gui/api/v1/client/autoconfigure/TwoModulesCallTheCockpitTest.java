package io.vanillabp.cockpit.gui.api.v1.client.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
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
import io.vanillabp.cockpit.gui.api.v1.client.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceGroupPrefix;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockWebServer;

/**
 * Two workflow modules of one application call the cockpit, each with keys of its own. Each
 * imports the interface into a group of its own. The group of the auto-configuration steps back,
 * and both groups forward the user's cookie.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = {
        TestApplication.class, TwoModulesCallTheCockpitTest.LoanApproval.class, TwoModulesCallTheCockpitTest.Onboarding.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "loan-approval.cockpit.default-header.X-Workflow-Module=loan-approval", "onboarding.cockpit.default-header.X-Workflow-Module=onboarding",
    })
@DirtiesContext
public class TwoModulesCallTheCockpitTest {

  static final MockWebServer cockpit = new MockWebServer();

  @DynamicPropertySource
  static void urls(
      final DynamicPropertyRegistry registry) {

    registry.add("loan-approval.cockpit.base-url", () -> Cockpit.guiApiUrl(cockpit));
    registry.add("onboarding.cockpit.base-url", () -> Cockpit.guiApiUrl(cockpit));

  }

  @AfterAll
  static void stop() {
    cockpit.close();
  }

  @AfterEach
  void leaveTheRequest() {
    RequestContextHolder.resetRequestAttributes();
  }

  interface LoanApprovalTasklist extends OfficialTasklistApi {
  }

  interface OnboardingTasklist extends OfficialTasklistApi {
  }

  @Configuration
  @ImportHttpServices(group = "loan-approval-cockpit", types = LoanApprovalTasklist.class)
  static class LoanApproval {

    @Bean
    HttpServiceGroupPrefix loanApprovalCockpitKeys() {
      return HttpServiceGroupPrefix.of("loan-approval-cockpit", "loan-approval.cockpit");
    }

  }

  @Configuration
  @ImportHttpServices(group = "onboarding-cockpit", types = OnboardingTasklist.class)
  static class Onboarding {

    @Bean
    HttpServiceGroupPrefix onboardingCockpitKeys() {
      return HttpServiceGroupPrefix.of("onboarding-cockpit", "onboarding.cockpit");
    }

  }

  @Autowired
  LoanApprovalTasklist loanApproval;

  @Autowired
  OnboardingTasklist onboarding;

  @Autowired
  ApplicationContext context;

  @Test
  public void each_module_calls_with_its_own_keys_and_the_users_cookie() {

    final var usersRequest = new MockHttpServletRequest();
    usersRequest.addHeader("Cookie", "bc-token=signed-login");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(usersRequest));
    cockpit.enqueue(Cockpit.userTask("task-1", "jane"));
    cockpit.enqueue(Cockpit.userTask("task-2", "joe"));

    loanApproval.getUserTask("task-1", null);
    onboarding.getUserTask("task-2", null);

    final var first = Cockpit.next(cockpit);
    final var second = Cockpit.next(cockpit);
    assertThat(first.getHeaders().get("X-Workflow-Module")).isEqualTo("loan-approval");
    assertThat(second.getHeaders().get("X-Workflow-Module")).isEqualTo("onboarding");
    assertThat(first.getHeaders().get("Cookie")).isEqualTo("bc-token=signed-login");
    assertThat(second.getHeaders().get("Cookie")).isEqualTo("bc-token=signed-login");

  }

  @Test
  public void the_group_of_the_auto_configuration_steps_back() {

    assertThat(context.getBeansOfType(OfficialWorkflowlistApi.class)).isEmpty();
    assertThat(context.getBeansOfType(OfficialTasklistApi.class)).hasSize(2);

  }

}
