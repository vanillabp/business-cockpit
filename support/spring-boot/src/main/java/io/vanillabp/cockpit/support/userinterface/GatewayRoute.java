package io.vanillabp.cockpit.support.userinterface;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Builds the route of Spring Cloud Gateway for one workflow module. It is the only class which names
 * the gateway or Spring MVC, so the JVM loads them only for an application which has a route and
 * the gateway with it. Every GET which {@link UserInterfaceRoute#forwards(String)} takes goes to the
 * address, with its path and its query unchanged.
 */
final class GatewayRoute {

  private GatewayRoute() {
  }

  static BeanDefinition definitionOf(
      final UserInterfaceRoute route) {

    final var definition = new RootBeanDefinition();
    definition.setTargetType(ResolvableType.forClassWithGenerics(RouterFunction.class, ServerResponse.class));
    definition.setInstanceSupplier(() -> of(route));
    return definition;

  }

  static RouterFunction<ServerResponse> of(
      final UserInterfaceRoute route) {

    return GatewayRouterFunctions
        .route(route.name())
        .route(
            RequestPredicates
                .method(HttpMethod.GET)
                .and(request -> route.forwards(request.requestPath().pathWithinApplication().value())),
            HandlerFunctions.http())
        .before(BeforeFilterFunctions.uri(route.address()))
        .build();

  }

}
