package io.vanillabp.cockpit.support.userinterface;

import java.util.List;

import io.quarkus.vertx.http.runtime.RouteConstants;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.HostAndPort;
import io.vertx.ext.web.Router;
import io.vertx.httpproxy.HttpProxy;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyResponse;

/**
 * Builds the route of Vert.x for one workflow module. It is the only class which names
 * {@code vertx-http-proxy}, so the JVM loads that library only for an application which has a route
 * and the library with it. Every GET which {@link UserInterfaceRoute#forwards(String)} takes goes to
 * the address, with its path and its query unchanged, and the {@code Host} header of the address,
 * as Spring Cloud Gateway sends it on Spring Boot. The answer goes back without the headers of the
 * connection to the nginx, as Spring Cloud Gateway leaves them out too, so it is valid over HTTP/2
 * as well.
 *
 * <p>
 * The route comes after the security of Quarkus, so a permission of
 * {@code quarkus.http.auth.permission} covers the files of the user interface, and before the
 * routes of Quarkus REST and of the files in the jars.
 * </p>
 */
final class VertxProxyRoute {

  /**
   * The headers which hold for one connection only, RFC 9110, section 7.6.1. They must not be
   * passed on, and in HTTP/2 an answer with one of them is malformed, RFC 9113, section 8.2.2.
   * {@code vertx-http-proxy} leaves out {@code Transfer-Encoding} only.
   */
  private static final List<String> HEADERS_OF_THE_CONNECTION = List.of(
      "Connection", "Keep-Alive", "Proxy-Connection", "Proxy-Authenticate", "TE", "Trailer",
      "Transfer-Encoding", "Upgrade");

  private VertxProxyRoute() {
  }

  /**
   * Removes the headers of the connection to the nginx from its answer: those of
   * {@link #HEADERS_OF_THE_CONNECTION}, and every header the {@code Connection} header names.
   *
   * @param headers the headers of the answer
   */
  static void withoutTheHeadersOfTheConnection(
      final MultiMap headers) {

    for (final var connection : headers.getAll("Connection")) {
      for (final var named : connection.split(",")) {
        if (!named.isBlank()) {
          headers.remove(named.strip());
        }
      }
    }
    HEADERS_OF_THE_CONNECTION.forEach(headers::remove);

  }

  static void add(
      final Router router,
      final Vertx vertx,
      final UserInterfaceRoute route) {

    final var client = vertx.createHttpClient(new HttpClientOptions()
        .setSsl(route.isTls())
        .setForceSni(route.isTls()));
    final var origin = HostAndPort.create(route.address().getHost(), route.port());
    final var proxy = HttpProxy
        .reverseProxy(client)
        .origin(origin.port(), origin.host())
        .addInterceptor(new ProxyInterceptor() {
          @Override
          public Future<ProxyResponse> handleProxyRequest(
              final ProxyContext context) {

            context.request().setAuthority(origin);
            return context.sendRequest();

          }

          @Override
          public Future<Void> handleProxyResponse(
              final ProxyContext context) {

            withoutTheHeadersOfTheConnection(context.response().headers());
            return context.sendResponse();

          }
        });

    router
        .get("/"
            + route.workflowModuleId()
            + "/*")
        .order(RouteConstants.ROUTE_ORDER_BEFORE_DEFAULT)
        .handler(request -> {
          if (route.forwards(request.request().path())) {
            proxy.handle(request.request());
          } else {
            request.next();
          }
        });

  }

}
