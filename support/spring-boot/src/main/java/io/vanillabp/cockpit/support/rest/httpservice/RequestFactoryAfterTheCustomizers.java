package io.vanillabp.cockpit.support.rest.httpservice;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

/**
 * Puts the request factory which the keys of a group built back on the group's client, after the
 * {@code RestClientCustomizer} beans ran. Spring Boot applies those customizers to the clients of
 * HTTP service groups with order {@code 0}, after {@link HttpServiceClientConventions}. Spring Cloud
 * Gateway brings one which hands its own request factory to every client of the application, built
 * from {@code spring.http.clients.*} with {@code redirects: dont-follow}. Without this configurer a
 * group of an application with the gateway would lose its timeouts, its TLS, its proxy, its pool and
 * its redirects.
 *
 * <p>
 * It runs right after the customizers. A {@code RestClientHttpServiceGroupConfigurer} of the
 * application with a later order, the default of a bean, can still set another factory for a group.
 * </p>
 */
public class RequestFactoryAfterTheCustomizers implements RestClientHttpServiceGroupConfigurer {

  /** Right after Spring Boot's configurer of the customizers, which has order 0. */
  public static final int ORDER = 1;

  private final HttpServiceClientConventions conventions;

  /**
   * Creates the configurer.
   *
   * @param conventions the conventions which built the request factories of the groups
   */
  public RequestFactoryAfterTheCustomizers(
      final HttpServiceClientConventions conventions) {

    this.conventions = conventions;

  }

  @Override
  public int getOrder() {

    return ORDER;

  }

  @Override
  public void configureGroups(
      final Groups<RestClient.Builder> groups) {

    groups.forEachClient((
        group,
        builder) -> conventions
            .requestFactoryOf(group.name())
            .ifPresent(builder::requestFactory));

  }

}
