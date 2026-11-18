package io.vanillabp.cockpit.support.rest.restclient;

import java.util.Set;
import java.util.StringJoiner;

import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

/**
 * Logs each request and its answer with headers and bodies at level INFO, with {@code log: true},
 * as the Spring Boot module and the REST client of version 1 do. Quarkus' own logger of the REST
 * client cuts bodies after 100 characters and logs nothing unless its category is switched to
 * DEBUG.
 * <p>
 * The values of the headers which carry a login are not written to the log.
 */
final class ExchangeLog implements ClientLogger {

  private static final Logger logger = LoggerFactory.getLogger(ExchangeLog.class);

  private static final Set<String> LOGINS = Set.of("authorization", "proxy-authorization", "cookie", "set-cookie");

  private static final String HIDDEN = "<hidden>";

  private final String prefix;

  /**
   * Creates the logger of the clients at one prefix.
   *
   * @param prefix written in front of each line, so the lines of two clients can be told apart
   */
  ExchangeLog(
      final String prefix) {
    this.prefix = prefix;
  }

  @Override
  public void setBodySize(
      final int bodySize) {
    // the whole body is logged, as on Spring Boot
  }

  @Override
  public void logRequest(
      final HttpClientRequest request,
      final Buffer body,
      final boolean omitBody) {

    logger.info("[{}] ---> {} {} {}\n{}", prefix, request.getMethod(), request.absoluteURI(),
        withoutLogins(request.headers()), (omitBody || (body == null)) ? "" : body.toString());

  }

  @Override
  public void logResponse(
      final HttpClientResponse response,
      final boolean redirect) {

    response.bodyHandler(body -> logger.info("[{}] <--- {} {}\n{}", prefix, response.statusCode(),
        withoutLogins(response.headers()), body.toString()));

  }

  private static String withoutLogins(
      final MultiMap headers) {

    final var shown = new StringJoiner(", ", "[", "]");
    headers.forEach(header -> shown.add(header.getKey()
        + ":\""
        + (LOGINS.contains(header.getKey().toLowerCase()) ? HIDDEN : header.getValue())
        + "\""));
    return shown.toString();

  }

}
