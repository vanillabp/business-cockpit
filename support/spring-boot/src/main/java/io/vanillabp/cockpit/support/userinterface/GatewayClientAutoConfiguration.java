package io.vanillabp.cockpit.support.userinterface;

import java.net.http.HttpClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.handler.ProxyExchange;
import org.springframework.cloud.gateway.server.mvc.handler.RestClientProxyExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The HTTP client of Spring Cloud Gateway, for every route of an application which has the gateway:
 * the route to the user interface of a workflow module, and in a cockpit the proxy of the workflow
 * modules below {@code /wm/}. A proxy passes on what it gets, so this client changes nothing on
 * the way:
 * <ul>
 * <li>It does not unpack a compressed answer. Spring's own clients do so by default and drop the
 * header {@code Content-Encoding}, so the browser gets more bytes than the server sent, and a cache
 * which compares them gets confused.</li>
 * <li>It speaks HTTP/1.1. The client of the JDK asks a server for HTTP/2 over a plain connection,
 * with the header {@code Upgrade: h2c}, and the development server of Vite does not answer that the
 * way the client expects. The route to a user interface which a developer serves with Vite then
 * hangs.</li>
 * </ul>
 * The other settings of the application's HTTP clients, {@code spring.http.clients.*}, hold for this
 * client as well. The gateway itself sets {@code spring.http.clients.redirects} to
 * {@code dont-follow}, so a redirect reaches the browser. An application which needs another client
 * for the gateway writes a bean of the type {@code ProxyExchange}, and this one steps back.
 *
 * <p>
 * Registered in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}. It runs
 * before the configuration of the gateway, because the gateway adds its own client only if the
 * application has none.
 * </p>
 */
@AutoConfiguration(beforeName = GatewayClientAutoConfiguration.GATEWAY_CONFIGURATION)
@ConditionalOnClass(name = GatewayClientAutoConfiguration.PROXY_EXCHANGE)
public class GatewayClientAutoConfiguration {

  /** The configuration of the gateway which adds its own client if there is none. */
  static final String GATEWAY_CONFIGURATION = "org.springframework.cloud.gateway.server.mvc.GatewayServerMvcAutoConfiguration";

  /** The client of the gateway this configuration provides. */
  static final String PROXY_EXCHANGE = "org.springframework.cloud.gateway.server.mvc.handler.RestClientProxyExchange";

  /**
   * Creates the configuration. Spring Boot creates it from the list of auto-configurations.
   */
  public GatewayClientAutoConfiguration() {
  }

  /**
   * The client which sends the requests of every route of the gateway.
   *
   * @param restClients The builder of Spring Boot, with the customizers of the application
   * @param gatewayProperties The settings of the gateway
   * @param settingsOfTheApplication The settings of {@code spring.http.clients.*}
   * @return The client of the gateway
   */
  @Bean
  @ConditionalOnMissingBean(ProxyExchange.class)
  public RestClientProxyExchange gatewayClientWhichChangesNothing(
      final RestClient.Builder restClients,
      final GatewayMvcProperties gatewayProperties,
      final ObjectProvider<HttpClientSettings> settingsOfTheApplication) {

    return new RestClientProxyExchange(
        restClients
            .requestFactory(requestFactory(settingsOfTheApplication.getIfAvailable(HttpClientSettings::defaults)))
            .build(), gatewayProperties);

  }

  /**
   * The request factory of the gateway's client, see the class comment.
   *
   * @param settings The settings of {@code spring.http.clients.*}
   * @return The factory
   */
  static ClientHttpRequestFactory requestFactory(
      final HttpClientSettings settings) {

    return ClientHttpRequestFactoryBuilder
        .jdk()
        .withHttpClientCustomizer(client -> client.version(HttpClient.Version.HTTP_1_1))
        .withCustomizer(factory -> factory.enableCompression(false))
        .build(settings);

  }

}
