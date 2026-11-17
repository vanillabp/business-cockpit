package io.vanillabp.cockpit.support.rest.restclient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * Small helpers around the stub server, so the tests read like the feature they show.
 * <p>
 * The test framework loads a test class twice: once while it builds the application, which reads
 * the address of the stub from the configuration, and once inside the application, where the test
 * methods run. The two copies share no static field. So the port is chosen once and kept in a
 * system property, which both copies see, and only the copy which runs the tests starts the server
 * on it, the first time a test needs it.
 */
public final class Stubs {

  private static final String HOST = "127.0.0.1";

  private Stubs() {
  }

  /**
   * The port of a stub, the same in both copies of a test class.
   *
   * @param name the name of the stub, unique in the test run
   * @return the port
   */
  public static int portOf(
      final String name) {

    final var key = "restclient-test.port."
        + name;
    synchronized (System.getProperties()) {
      var port = System.getProperty(key);
      if (port == null) {
        try (var socket = new ServerSocket(0)) {
          port = Integer.toString(socket.getLocalPort());
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
        System.setProperty(key, port);
      }
      return Integer.parseInt(port);
    }

  }

  /**
   * The address of a path on a stub.
   *
   * @param scheme {@code http} or {@code https}
   * @param name   the name of the stub
   * @param path   the path
   * @return the address
   */
  public static String url(
      final String scheme,
      final String name,
      final String path) {

    return scheme
        + "://"
        + HOST
        + ":"
        + portOf(name)
        + path;

  }

  /**
   * The address of a path on a stub which speaks plain HTTP.
   *
   * @param name the name of the stub
   * @param path the path
   * @return the address
   */
  public static String url(
      final String name,
      final String path) {

    return url("http", name, path);

  }

  /**
   * Starts the server on the port of its name, once.
   *
   * @param server the server
   * @param name   the name of the stub
   * @return the server
   */
  public static MockWebServer started(
      final MockWebServer server,
      final String name) {

    if (!server.getStarted()) {
      try {
        server.start(InetAddress.getByName(HOST), portOf(name));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return server;

  }

  public static MockResponse json(
      final int code,
      final String body) {

    return new MockResponse.Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build();

  }

  public static MockResponse item(
      final String id) {

    return json(200, """
        {"id": "%s", "name": "Screw", "stock": 7, "changedAt": "2024-01-31T12:00:00Z"}""".formatted(id));

  }

  /** An answer which comes later than a short read timeout allows. */
  public static MockResponse tooSlow() {

    return new MockResponse.Builder().code(200).headersDelay(1, TimeUnit.SECONDS).build();

  }

  /** Forgets the requests of earlier tests, so that {@link #next} returns one of this test. */
  public static void drain(
      final MockWebServer server) {

    try {
      while (server.takeRequest(10, TimeUnit.MILLISECONDS) != null) {
        // forget it
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

  }

  /** The next request the stub got, or a failure if none came within a few seconds. */
  public static RecordedRequest next(
      final MockWebServer server) {

    try {
      final var request = server.takeRequest(5, TimeUnit.SECONDS);
      if (request == null) {
        throw new AssertionError("The stub server got no request");
      }
      return request;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    }

  }

}
