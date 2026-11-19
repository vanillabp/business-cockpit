package io.vanillabp.cockpit.support.userinterface;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Which applications get a route, and how a wrong key ends the start. The workflow module
 * {@code loan-approval} is named by {@code META-INF/workflow-module} of the test resources.
 */
@ExtendWith(SuppressOutputExtension.class)
public class TheKeysOfTheRouteAreCheckedTest {

  private static final String ADDRESS = UserInterfaceRoute.keyOf("loan-approval");

  private final WebApplicationContextRunner application = new WebApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(UserInterfaceRouteAutoConfiguration.class));

  @Test
  public void withoutAnAddressThereIsNoRoute() {

    application.run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context).doesNotHaveBean("loan-approval-user-interface");
    });

  }

  @Test
  public void withoutAnAddressTheGatewayIsNotNeeded() {

    application
        .withClassLoader(new FilteredClassLoader(GatewayRouterFunctions.class))
        .run(context -> assertThat(context).hasNotFailed());

  }

  @Test
  public void withAnAddressTheModuleGetsItsRoute() {

    application
        .withPropertyValues(ADDRESS
            + "=http://nginx:8081")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasBean("loan-approval-user-interface");
          assertThat(context.getBean("loan-approval-user-interface")).isInstanceOf(RouterFunction.class);
        });

  }

  @Test
  public void anAddressWithoutTheGatewayEndsTheStart() {

    application
        .withClassLoader(new FilteredClassLoader(GatewayRouterFunctions.class))
        .withPropertyValues(ADDRESS
            + "=http://nginx:8081")
        .run(context -> assertThat(context)
            .getFailure()
            .isInstanceOf(UserInterfaceRoutesMisconfigured.class)
            .hasMessageContaining("'loan-approval.user-interface.address' asks for a route")
            .hasMessageContaining(
                "Add the dependency 'org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc'"));

  }

  @Test
  public void aWrongAddressAndAnUnknownModuleEndTheStartTogether() {

    application
        .withPropertyValues(ADDRESS
            + "=http://nginx:8081/loan-approval", "loan-aproval.user-interface.address=http://nginx")
        .run(context -> {
          assertThat(context)
              .getFailure()
              .isInstanceOfSatisfying(UserInterfaceRoutesMisconfigured.class,
                  failure -> assertThat(failure.getProblems())
                      .hasSize(2))
              .hasMessageContaining("only its scheme, host and port count")
              .hasMessageContaining(
                  "'loan-aproval.user-interface.address' names no workflow module of this application");
          final var analysis = new UserInterfaceRoutesMisconfiguredFailureAnalyzer()
              .analyze(context.getStartupFailure());
          assertThat(analysis.getDescription()).startsWith("The routes to the user interfaces");
          assertThat(analysis.getAction()).contains("'<id of the module>.user-interface.address'");
        });

  }

  @Test
  public void aRouteTheModuleBringsItselfIsKept() {

    application
        .withUserConfiguration(OwnRoute.class)
        .withPropertyValues(ADDRESS
            + "=http://nginx:8081")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean("loan-approval-user-interface")).isSameAs(OwnRoute.ROUTE);
        });

  }

  /** A route of the module's own, as the blueprints had it before the support library had one. */
  @Configuration(proxyBeanMethods = false)
  static class OwnRoute {

    static final RouterFunction<ServerResponse> ROUTE = RouterFunctions
        .route()
        .GET("/own", request -> ServerResponse.ok().build())
        .build();

    @Bean("loan-approval-user-interface")
    RouterFunction<ServerResponse> ownRoute() {

      return ROUTE;

    }

  }

}
