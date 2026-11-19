package io.vanillabp.cockpit.support.userinterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

/**
 * Adds one bean of type {@code RouterFunction} for each workflow module whose address is set, named
 * like the route, {@code <id>-user-interface}. The keys are checked by {@link UserInterfaceRoutes}
 * of {@code core}, which the Quarkus module of {@code support} uses as well. A wrong key ends the
 * start with {@link UserInterfaceRoutesMisconfigured}, and so does an address without the gateway.
 *
 * <p>
 * A workflow module which still brings a bean of the same name, its own route from the time before
 * {@code support} had one, keeps it. This registrar then adds nothing for that module and says so in
 * the log.
 * </p>
 */
class UserInterfaceRouteRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware, BeanClassLoaderAware {

  private static final Logger logger = LoggerFactory.getLogger(UserInterfaceRouteRegistrar.class);

  /** A class of the gateway in the flavour for Spring MVC, the one the route is built with. */
  static final String GATEWAY_CLASS = "org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions";

  private Environment environment;

  private ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

  @Override
  public void setEnvironment(
      final Environment environment) {

    this.environment = environment;

  }

  @Override
  public void setBeanClassLoader(
      final ClassLoader classLoader) {

    this.classLoader = classLoader;

  }

  @Override
  public void postProcessBeanDefinitionRegistry(
      final BeanDefinitionRegistry registry) {

    final var routes = UserInterfaceRoutes.of(
        UserInterfaceRoutes.workflowModulesOn(classLoader),
        key -> Optional.ofNullable(environment.getProperty(key)),
        propertyNames(),
        ClassUtils.isPresent(GATEWAY_CLASS, classLoader),
        """
            Add the dependency 'org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc' \
            and import the BOM 'org.springframework.cloud:spring-cloud-dependencies', or remove the \
            key to serve the files from the UI jar.""");

    for (final var route : routes) {
      if (registry.containsBeanDefinition(route.name())) {
        logger.info(
            "The application already has a bean '{}', so the route of business-cockpit-spring-boot-support "
                + "to {} is left out. Remove that bean to use the route of the support library.",
            route.name(), route.address());
        continue;
      }
      registry.registerBeanDefinition(route.name(), GatewayRoute.definitionOf(route));
      logger.info("The files of the user interface of '{}' come from {}.", route.workflowModuleId(), route.address());
    }

  }

  private List<String> propertyNames() {

    final var names = new ArrayList<String>();
    if (!(environment instanceof ConfigurableEnvironment configurable)) {
      return names;
    }
    for (final var source : configurable.getPropertySources()) {
      if (source instanceof EnumerablePropertySource<?> enumerable) {
        names.addAll(List.of(enumerable.getPropertyNames()));
      }
    }
    return names;

  }

}
