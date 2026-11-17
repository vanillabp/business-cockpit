package io.vanillabp.cockpit.gui.api.v1.client.autoconfigure;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.gui.api.v1.client.OfficialNotificationConfigApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialWorkflowModulesApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceGroupPrefix;

/**
 * Builds the clients of the cockpit's official GUI API, so an application adds the dependency,
 * sets the keys and injects the interfaces. The clients form the HTTP service group
 * {@value #GROUP}. The support module configures it from the keys below the group's prefix:
 * {@code spring.http.serviceclient.official-gui-api.*} by default, or the prefix of an
 * {@link HttpServiceGroupPrefix} bean the application declares for {@value #GROUP}.
 * <p>
 * The group steps back if the application imports one of the interfaces into a group of its own,
 * for example because two workflow modules call the cockpit with different keys.
 * <p>
 * Every group with an interface of this API forwards the cookie of the user's request, see
 * {@link ForwardTheUsersCookie}.
 */
@AutoConfiguration(before = HttpServiceClientAutoConfiguration.class)
public class OfficialGuiApiClientAutoConfiguration {

  /** The name of the HTTP service group of the clients. */
  public static final String GROUP = "official-gui-api";

  /** The interfaces generated from the API, one per tag of the OpenAPI file. */
  public static final List<Class<?>> APIS = List.of(
      OfficialTasklistApi.class,
      OfficialWorkflowlistApi.class,
      OfficialWorkflowModulesApi.class,
      OfficialNotificationConfigApi.class);

  /**
   * Creates the configuration. Spring Boot creates it from the list of auto-configurations.
   */
  public OfficialGuiApiClientAutoConfiguration() {
  }

  /**
   * Forwards the cookie of the user's request to the cockpit.
   *
   * @return the configurer of every group with an interface of this API
   */
  @Bean
  ForwardTheUsersCookie officialGuiApiForwardTheUsersCookie() {
    return new ForwardTheUsersCookie(APIS);
  }

  /** The group of the clients, if the application has no clients of this API of its own. */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean({
      OfficialTasklistApi.class, OfficialWorkflowlistApi.class, OfficialWorkflowModulesApi.class, OfficialNotificationConfigApi.class
  })
  @ImportHttpServices(group = GROUP, types = {
      OfficialTasklistApi.class, OfficialWorkflowlistApi.class, OfficialWorkflowModulesApi.class, OfficialNotificationConfigApi.class
  })
  static class OfficialGuiApiGroup {
  }

}
