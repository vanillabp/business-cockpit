package io.vanillabp.cockpit.gui.api.v1.client.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import io.vanillabp.cockpit.gui.api.v1.client.OfficialCurrentUserApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceClientsMisconfigured;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceGroupPrefix;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The dependency alone asks for no key. A workflow module may have it on its class path, in a
 * test or because it calls the cockpit only if the application wants it, and the application
 * still starts. The clients are there as soon as the address of the cockpit is set.
 */
@ExtendWith(SuppressOutputExtension.class)
public class WithoutAnAddressThereIsNoClientTest {

  private static final String ADDRESS_KEY = "spring.http.serviceclient.official-gui-api.base-url";

  /** Calls the cockpit only if the application set its address. */
  static class AskTheCockpitIfConfigured {

    private final ObjectProvider<OfficialTasklistApi> tasklist;

    AskTheCockpitIfConfigured(
        final ObjectProvider<OfficialTasklistApi> tasklist) {
      this.tasklist = tasklist;
    }

    boolean asksTheCockpit() {
      return tasklist.getIfAvailable() != null;
    }

  }

  @Configuration
  static class OptionalCockpit {

    @Bean
    AskTheCockpitIfConfigured askTheCockpitIfConfigured(
        final ObjectProvider<OfficialTasklistApi> tasklist) {
      return new AskTheCockpitIfConfigured(tasklist);
    }

  }

  /** Needs the cockpit, so it cannot start without its address. */
  @Configuration
  static class RequiredCockpit {

    @Bean
    String assigneeLookup(
        final OfficialTasklistApi tasklist) {
      return "uses "
          + tasklist;
    }

  }

  /** Gives the group of the auto-configuration a prefix, which it does not read. */
  @Configuration
  static class PrefixOfTheGroupOfTheAutoConfiguration {

    @Bean
    HttpServiceGroupPrefix cockpitKeys() {
      return HttpServiceGroupPrefix.of(OfficialGuiApiClientAutoConfiguration.GROUP, "loan-approval.cockpit");
    }

  }

  @Test
  public void without_the_address_the_application_starts_and_has_no_client() {

    try (var context = start(new Class<?>[]{
        TestApplication.class, OptionalCockpit.class
    })) {

      assertThat(context.getBeansOfType(OfficialTasklistApi.class)).isEmpty();
      assertThat(context.getBeansOfType(OfficialCurrentUserApi.class)).isEmpty();
      assertThat(context.getBean(AskTheCockpitIfConfigured.class).asksTheCockpit()).isFalse();

    }

  }

  @Test
  public void with_the_address_the_clients_are_there() {

    try (var context = start(new Class<?>[]{
        TestApplication.class, OptionalCockpit.class
    }, ADDRESS_KEY
        + "=http://localhost:1")) {

      assertThat(context.getBeansOfType(OfficialTasklistApi.class)).hasSize(1);
      assertThat(context.getBeansOfType(OfficialCurrentUserApi.class)).hasSize(1);
      assertThat(context.getBean(AskTheCockpitIfConfigured.class).asksTheCockpit()).isTrue();

    }

  }

  @Test
  public void an_address_with_a_placeholder_nothing_resolves_still_names_the_placeholder() {

    final var problems = problemsOf(catchStartFailure(new Class<?>[]{
        TestApplication.class
    }, ADDRESS_KEY
        + "=${COCKPIT_URL}"));

    assertThat(problems).singleElement().asString()
        .contains("'COCKPIT_URL'")
        .contains("'"
            + ADDRESS_KEY
            + "'");

  }

  @Test
  public void a_prefix_for_the_group_of_the_auto_configuration_says_how_to_choose_a_prefix() {

    final var problems = problemsOf(catchStartFailure(new Class<?>[]{
        TestApplication.class, PrefixOfTheGroupOfTheAutoConfiguration.class
    }, "loan-approval.cockpit.base-url=http://localhost:1"));

    assertThat(problems).singleElement().asString()
        .contains("'"
            + ADDRESS_KEY
            + "'")
        .contains("'loan-approval.cockpit'")
        .contains("group of its own");

  }

  @Test
  public void a_class_which_needs_a_client_learns_which_key_is_missing() {

    final var failure = catchStartFailure(new Class<?>[]{
        TestApplication.class, RequiredCockpit.class
    });

    final FailureAnalysis analysis = new TheOfficialGuiApiHasNoAddress(new StandardEnvironment()).analyze(failure);

    assertThat(analysis).isNotNull();
    assertThat(analysis.getDescription())
        .contains("OfficialTasklistApi")
        .contains("'"
            + ADDRESS_KEY
            + "'");
    assertThat(analysis.getAction())
        .contains("'"
            + ADDRESS_KEY
            + "'")
        .contains("ObjectProvider")
        .contains("'https://cockpit.example.com'")
        .contains("'/gui/api/v1'");

  }

  @Test
  public void a_missing_bean_of_another_type_is_left_to_spring_boot() {

    final var analyzer = new TheOfficialGuiApiHasNoAddress(new StandardEnvironment());

    assertThat(analyzer.analyze(new NoSuchBeanDefinitionException(String.class))).isNull();

  }

  @Test
  public void a_missing_client_while_the_address_is_set_is_left_to_spring_boot() {

    final var analyzer = new TheOfficialGuiApiHasNoAddress(new MockEnvironment()
        .withProperty(ADDRESS_KEY, "http://localhost:1"));

    assertThat(analyzer.analyze(new NoSuchBeanDefinitionException(OfficialTasklistApi.class))).isNull();

  }

  private static ConfigurableApplicationContext start(
      final Class<?>[] configurations,
      final String... properties) {

    return new SpringApplicationBuilder(configurations)
        .web(WebApplicationType.NONE)
        .run(Arrays.stream(properties).map(property -> "--"
            + property).toArray(String[]::new));

  }

  private static Throwable catchStartFailure(
      final Class<?>[] configurations,
      final String... properties) {

    try (var context = start(configurations, properties)) {
      throw new AssertionError("The application started, but it should have stopped");
    } catch (RuntimeException e) {
      return e;
    }

  }

  /** The problems the support module lists, wherever they are in the chain of causes. */
  private static List<String> problemsOf(
      final Throwable failure) {

    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof HttpServiceClientsMisconfigured misconfigured) {
        return misconfigured.getProblems();
      }
    }
    throw new AssertionError("The application stopped for another reason", failure);

  }

}
