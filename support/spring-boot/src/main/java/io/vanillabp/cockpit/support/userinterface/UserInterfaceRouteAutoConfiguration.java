package io.vanillabp.cockpit.support.userinterface;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Brings the routes to the user interfaces of the workflow modules: for every workflow module whose
 * address {@code <id>.user-interface.address} is set, a route of Spring Cloud Gateway which passes
 * the files of its user interface on to that address. A workflow module needs no class and no key
 * for it. The application adds the gateway, {@code spring-cloud-starter-gateway-server-webmvc}, and
 * sets the address.
 *
 * <p>
 * An application which sets no address gets no route and needs no gateway. Its files come from the
 * UI jar, which Spring Boot serves below {@code META-INF/resources/}. With the address, Spring MVC
 * asks the route before it looks into the jars, so the other server answers even if the UI jar is
 * there as well.
 * </p>
 *
 * <p>
 * Registered in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 * </p>
 */
@AutoConfiguration
public class UserInterfaceRouteAutoConfiguration {

  /**
   * Creates the configuration. Spring Boot creates it from the list of auto-configurations.
   */
  public UserInterfaceRouteAutoConfiguration() {
  }

  /**
   * The registrar of the routes. It is static, because it adds bean definitions and so runs before
   * any bean of this configuration could be created.
   *
   * @return The registrar
   */
  @Bean
  static UserInterfaceRouteRegistrar userInterfaceRouteRegistrar() {

    return new UserInterfaceRouteRegistrar();

  }

}
