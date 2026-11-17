package io.vanillabp.cockpit.gui.api.v1.client.quarkus;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.vanillabp.cockpit.gui.api.v1.client.OfficialCurrentUserApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialNotificationConfigApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialWorkflowModulesApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialWorkflowlistApi;
import jakarta.ws.rs.OPTIONS;

/**
 * Quarkus' REST client warns while the application is built about a method with OPTIONS which
 * sends a body. The two such operations of the OpenAPI file, the suggestions of words for the
 * lists, are left out of this client, so a workflow module builds without the warning.
 */
public class NoMethodSendsABodyWithOptionsTest {

  private static final List<Class<?>> APIS = List.of(
      OfficialTasklistApi.class,
      OfficialWorkflowlistApi.class,
      OfficialWorkflowModulesApi.class,
      OfficialNotificationConfigApi.class,
      OfficialCurrentUserApi.class);

  @Test
  public void no_interface_has_a_method_with_options() {

    final var withOptions = APIS
        .stream()
        .flatMap(api -> Arrays.stream(api.getMethods()))
        .filter(method -> method.isAnnotationPresent(OPTIONS.class))
        .map(Method::getName)
        .toList();

    assertThat(withOptions).isEmpty();

  }

  @Test
  public void the_other_methods_of_the_lists_are_still_there() {

    assertThat(Arrays.stream(OfficialTasklistApi.class.getMethods()).map(Method::getName))
        .contains("getUserTask", "getUserTasks")
        .doesNotContain("getUserTaskKwicResults");
    assertThat(Arrays.stream(OfficialWorkflowlistApi.class.getMethods()).map(Method::getName))
        .contains("getWorkflows")
        .doesNotContain("getKwicResults");

  }

}
