package io.vanillabp.cockpit.support.rest.httpservice;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;

/**
 * Brings {@link HttpServiceClientConventions} to every application which has HTTP service groups
 * and this module on its class path. It applies to every group of the application. Spring Boot's
 * HTTP service client configuration runs first, so its settings are there to build on.
 */
@AutoConfiguration(after = HttpServiceClientAutoConfiguration.class)
@ConditionalOnClass(RestClientHttpServiceGroupConfigurer.class)
@ConditionalOnBean(HttpServiceProxyRegistry.class)
public class HttpServiceClientConventionsAutoConfiguration {

  /**
   * Creates the configuration. Spring Boot creates it from the list of auto-configurations.
   */
  public HttpServiceClientConventionsAutoConfiguration() {
  }

  @Bean
  HttpServiceClientConventions httpServiceClientConventions(
      final Environment environment,
      final ObjectProvider<SslBundles> sslBundles,
      final ObjectProvider<HttpClientSettings> defaultSettings,
      final ObjectProvider<ClientHttpRequestFactoryBuilder<?>> requestFactoryBuilder,
      final ObjectProvider<HttpServiceGroupPrefix> prefixes) {

    return new HttpServiceClientConventions(
        environment, sslBundles.getIfAvailable(), defaultSettings.getIfAvailable(), requestFactoryBuilder
            .getIfAvailable(), prefixes.orderedStream().toList());

  }

}
