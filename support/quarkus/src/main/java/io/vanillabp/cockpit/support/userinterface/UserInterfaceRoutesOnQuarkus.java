package io.vanillabp.cockpit.support.userinterface;

import java.util.ArrayList;

import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Adds the routes to the user interfaces of the workflow modules: for every workflow module whose
 * address {@code <id>.user-interface.address} is set, a route of Vert.x which passes the files of
 * its user interface on to that address. A workflow module needs no class and no key for it. The
 * application adds the library {@code io.vertx:vertx-http-proxy}, whose version the Quarkus BOM
 * names, and sets the address. These are the keys of the Spring Boot module, checked by the same
 * {@link UserInterfaceRoutes} of {@code core}.
 *
 * <p>
 * An application which sets no address gets no route and needs no library. Its files come from the
 * UI jar, which Quarkus serves below {@code META-INF/resources/}. With the address, the route
 * answers before Quarkus looks into the jars, so the other server answers even if the UI jar is
 * there as well. A wrong key ends the start with {@link UserInterfaceRoutesMisconfigured}, and so
 * does an address without the library.
 * </p>
 *
 * <p>
 * Quarkus finds this bean in the Jandex index of the jar. It adds the routes when Quarkus hands out
 * its router, before the application takes requests.
 * </p>
 */
@ApplicationScoped
public class UserInterfaceRoutesOnQuarkus {

  private static final Logger logger = LoggerFactory.getLogger(UserInterfaceRoutesOnQuarkus.class);

  /** A class of the library which passes a request on. */
  static final String PROXY_CLASS = "io.vertx.httpproxy.HttpProxy";

  /**
   * Creates the bean. Quarkus calls this constructor.
   */
  public UserInterfaceRoutesOnQuarkus() {
  }

  void addRoutes(
      @Observes final Router router,
      final Vertx vertx) {

    final var classLoader = Thread.currentThread().getContextClassLoader();
    final var config = ConfigProvider.getConfig();
    final var propertyNames = new ArrayList<String>();
    config.getPropertyNames().forEach(propertyNames::add);

    final var routes = UserInterfaceRoutes.of(
        UserInterfaceRoutes.workflowModulesOn(classLoader),
        key -> config.getOptionalValue(key, String.class),
        propertyNames,
        isPresent(classLoader),
        """
            Add the dependency 'io.vertx:vertx-http-proxy'. The Quarkus BOM names its version. Or \
            remove the key to serve the files from the UI jar.""");

    for (final var route : routes) {
      VertxProxyRoute.add(router, vertx, route);
      logger.info("The files of the user interface of '{}' come from {}.", route.workflowModuleId(), route.address());
    }

  }

  private static boolean isPresent(
      final ClassLoader classLoader) {

    try {
      Class.forName(PROXY_CLASS, false, classLoader);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }

  }

}
