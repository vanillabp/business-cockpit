package io.vanillabp.cockpit.support.rest.httpservice;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpComponentsClientHttpRequestFactoryBuilder;

/**
 * Sets the size of Apache HttpClient 5's connection pool. It is a class of its own because it names
 * classes of Apache HttpClient 5, which an application without that library does not have. The JVM
 * loads this class only when the application sends with Apache HttpClient 5.
 */
final class HttpComponentsConnectionPool {

  private HttpComponentsConnectionPool() {
  }

  /**
   * A builder whose clients open up to {@code size} connections to each server and in total.
   *
   * @param builder the builder of the application
   * @param size    the number of connections, at least 1
   * @return the builder with the pool of that size
   */
  static ClientHttpRequestFactoryBuilder<?> ofSize(
      final HttpComponentsClientHttpRequestFactoryBuilder builder,
      final int size) {

    // a group talks to the one server of its base URL, so both limits get the same number
    return builder.withConnectionManagerCustomizer(pool -> pool
        .setMaxConnPerRoute(size)
        .setMaxConnTotal(size));

  }

}
