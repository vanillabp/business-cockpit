package io.vanillabp.cockpit.support.quarkus.it;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;

/**
 * Stands in for the nginx of a user interface. It answers every request with what it got: the
 * method, the path with the query, and the {@code Host} header. A path which ends with
 * {@code missing.js} gets {@code 404}.
 * <p>
 * The test framework loads a test class twice, and the two copies share no static field. So the
 * server is started once per JVM, and its port is kept in a system property, which both copies see.
 */
public final class EchoingNginx {

  private static final String PORT = "echoing-nginx.port";

  private EchoingNginx() {
  }

  /**
   * The address of the server, started on the first call.
   *
   * @return The address, like {@code http://127.0.0.1:12345}
   */
  public static String address() {

    synchronized (System.getProperties()) {
      var port = System.getProperty(PORT);
      if (port == null) {
        port = Integer.toString(start());
        System.setProperty(PORT, port);
      }
      return "http://127.0.0.1:"
          + port;
    }

  }

  private static int start() {

    try {
      final var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
      server.createContext("/", exchange -> {
        final var body = ("nginx got "
            + exchange.getRequestMethod()
            + " "
            + exchange.getRequestURI()
            + " for "
            + exchange.getRequestHeaders().getFirst("Host")).getBytes(StandardCharsets.UTF_8);
        final var status = exchange.getRequestURI().getPath().endsWith("missing.js")
            ? 404
            : 200;
        exchange.getResponseHeaders().add("Content-Type", "text/plain");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
      });
      server.start();
      return server.getAddress().getPort();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

  }

}
