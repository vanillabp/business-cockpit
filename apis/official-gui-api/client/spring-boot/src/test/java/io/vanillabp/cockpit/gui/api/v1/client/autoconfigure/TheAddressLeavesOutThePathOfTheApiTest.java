package io.vanillabp.cockpit.gui.api.v1.client.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceClientsMisconfigured;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceGroupPrefix;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * 'base-url' is the address of the cockpit only. The client adds the path of the API, the server
 * path of the OpenAPI file, itself, so a client of a new version of the API brings its
 * new path along. An address which ends with that path would send each call to
 * '/gui/api/v1/gui/api/v1/...', so it stops the start and the message shows the address to set.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TheAddressLeavesOutThePathOfTheApiTest {

  interface LoanApprovalTasklist extends OfficialTasklistApi {
  }

  /** A workflow module with a group of its own, at a prefix of its own. */
  @Configuration
  @ImportHttpServices(group = "loan-approval-cockpit", types = LoanApprovalTasklist.class)
  static class LoanApproval {

    @Bean
    HttpServiceGroupPrefix loanApprovalCockpitKeys() {
      return HttpServiceGroupPrefix.of("loan-approval-cockpit", "loan-approval.cockpit");
    }

  }

  @Test
  public void the_path_of_the_api_in_the_address_of_the_group_of_the_auto_configuration_stops_the_start() {

    final var problems = problemsOf(new Class<?>[]{
        TestApplication.class
    }, OfficialGuiApiClientAutoConfiguration.ADDRESS
        + "=https://cockpit.example.com/gui/api/v1");

    assertThat(problems).singleElement().asString()
        .contains("'https://cockpit.example.com/gui/api/v1' in '"
            + OfficialGuiApiClientAutoConfiguration.ADDRESS
            + "' ends with '/gui/api/v1'")
        .contains("the path of the API which the HTTP service group 'official-gui-api' adds to the address itself")
        .endsWith("to the address without it: 'https://cockpit.example.com'.");

  }

  @Test
  public void the_path_of_the_api_in_the_address_of_a_group_of_a_workflow_module_stops_the_start() {

    final var problems = problemsOf(new Class<?>[]{
        TestApplication.class, LoanApproval.class
    }, "loan-approval.cockpit.base-url=https://example.com/cockpit/gui/api/v1/");

    assertThat(problems).containsExactly("""
        The address 'https://example.com/cockpit/gui/api/v1/' in 'loan-approval.cockpit.base-url' \
        ends with '/gui/api/v1', the path of the API which the HTTP service group \
        'loan-approval-cockpit' adds to the address itself. Set 'loan-approval.cockpit.base-url' to \
        the address without it: 'https://example.com/cockpit'.""");

  }

  /** The problems the support module lists, wherever they are in the chain of causes. */
  private static List<String> problemsOf(
      final Class<?>[] configurations,
      final String... properties) {

    try (var context = new SpringApplicationBuilder(configurations)
        .web(WebApplicationType.NONE)
        .run(Arrays.stream(properties).map(property -> "--"
            + property).toArray(String[]::new))) {
      throw new AssertionError("The application started, but it should have stopped");
    } catch (RuntimeException failure) {
      for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
        if (cause instanceof HttpServiceClientsMisconfigured misconfigured) {
          return misconfigured.getProblems();
        }
      }
      throw new AssertionError("The application stopped for another reason", failure);
    }

  }

}
