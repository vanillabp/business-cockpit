package io.vanillabp.cockpit.gui.api.v1.client.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.gui.api.v1.client.OfficialCurrentUserApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialNotificationConfigApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialWorkflowModulesApi;
import io.vanillabp.cockpit.gui.api.v1.client.OfficialWorkflowlistApi;
import io.vanillabp.cockpit.support.rest.httpservice.ConditionalOnHttpServiceGroupAddress;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceClientsMisconfigured;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceGroupPrefix;

/**
 * Builds the clients of the cockpit's official GUI API, so an application adds the dependency,
 * sets the address of the cockpit and injects the interfaces. The clients form the HTTP service
 * group {@value #GROUP}, and the support module configures it from the keys below
 * {@value #KEYS}.
 * <p>
 * The group is there only if {@value #ADDRESS} is set. Without it the application starts and has
 * no client of this API, so the dependency on the class path, in a test for example, asks for no
 * key. A class which calls the cockpit only if the application wants it takes an
 * {@link ObjectProvider} of the interface. A class which needs the client fails to start, and
 * {@link TheOfficialGuiApiHasNoAddress} names the key.
 * <p>
 * The group steps back if the application imports one of the interfaces into a group of its own,
 * for example because two workflow modules call the cockpit with different keys. Such a group may
 * read its keys below a prefix of its choice, see {@link HttpServiceGroupPrefix}. The group of
 * this class always reads them in Spring Boot's place: whether it is there is decided before any
 * bean exists, so it cannot ask a bean for its prefix.
 * <p>
 * Every group with an interface of this API forwards the cookie of the user's request, see
 * {@link ForwardTheUsersCookie}.
 */
@AutoConfiguration(before = HttpServiceClientAutoConfiguration.class)
public class OfficialGuiApiClientAutoConfiguration {

  /** The name of the HTTP service group of the clients. */
  public static final String GROUP = "official-gui-api";

  /** Where the group reads its keys: Spring Boot's place for {@value #GROUP}. */
  public static final String KEYS = HttpServiceGroupPrefix.SPRING_BOOT_PREFIX
      + "."
      + GROUP;

  /**
   * The key of the address of the cockpit, with its context path if it has one. The path of the
   * API is not part of it: each interface carries the server path of the OpenAPI file in
   * {@code @HttpExchange} on the type, and an address which ends with that path stops the start.
   * Without the key, the group is not there.
   */
  public static final String ADDRESS = KEYS
      + ".base-url";

  /** The interfaces generated from the API, one per tag of the OpenAPI file. */
  public static final List<Class<?>> APIS = List.of(
      OfficialTasklistApi.class,
      OfficialWorkflowlistApi.class,
      OfficialWorkflowModulesApi.class,
      OfficialNotificationConfigApi.class,
      OfficialCurrentUserApi.class);

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

  /**
   * Stops the start if the application gives the group of this class a prefix. The prefix would
   * do nothing, and the developer would wonder why the clients are missing.
   *
   * @param prefixes the prefixes of the application's groups
   * @return the check, run once all beans exist
   */
  @Bean
  SmartInitializingSingleton officialGuiApiKeysStayInSpringBootsPlace(
      final ObjectProvider<HttpServiceGroupPrefix> prefixes) {

    return () -> prefixes
        .orderedStream()
        .filter(prefix -> prefix.group().equals(GROUP))
        .filter(prefix -> !prefix.prefix().equals(KEYS))
        .findFirst()
        .ifPresent(prefix -> {
          throw new HttpServiceClientsMisconfigured(List.of("""
              An HttpServiceGroupPrefix gives the HTTP service group '%s' the prefix '%s'. That \
              group reads its keys in Spring Boot's place only, and it is there only if '%s' is \
              set. To read the keys below '%s', import the interfaces you need into a group of its \
              own and give that group the prefix. The README of \
              official-gui-api-spring-boot-client shows how."""
              .formatted(GROUP, prefix.prefix(), ADDRESS, prefix.prefix())));
        });

  }

  /**
   * The group of the clients, if the application set the address of the cockpit and has no clients
   * of this API of its own.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnHttpServiceGroupAddress(prefix = KEYS)
  @ConditionalOnMissingBean({
      OfficialTasklistApi.class, OfficialWorkflowlistApi.class, OfficialWorkflowModulesApi.class, OfficialNotificationConfigApi.class, OfficialCurrentUserApi.class
  })
  @ImportHttpServices(group = GROUP, types = {
      OfficialTasklistApi.class, OfficialWorkflowlistApi.class, OfficialWorkflowModulesApi.class, OfficialNotificationConfigApi.class, OfficialCurrentUserApi.class
  })
  static class OfficialGuiApiGroup {
  }

}
