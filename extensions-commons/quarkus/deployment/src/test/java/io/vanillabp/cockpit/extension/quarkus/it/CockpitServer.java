package io.vanillabp.cockpit.extension.quarkus.it;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.sun.net.httpserver.HttpServer;

/**
 * The cockpit server, played by the test: it records every request and answers what it
 * received when it is asked over HTTP.
 * <p>
 * Being asked over HTTP rather than through a static field is what makes this work under
 * Quarkus. A test class of a <code>QuarkusExtensionTest</code> runs in the application's own
 * class loader, so a second copy of this class exists there with static fields of its own; the
 * requests are recorded in whichever copy started the server, and only the wire reaches both.
 * The server itself is started once per JVM, guarded by a system property for the same reason.
 */
public final class CockpitServer {

  /** One request the cockpit server received. */
  public record Request(
                        String path,
                        String body) {
  }

  /** Where the URL of the running server is published, so that every class loader finds it. */
  private static final String URL_PROPERTY = "business-cockpit.test-server.url";

  /** Answers with everything received so far, one request per line. */
  private static final String REQUESTS_PATH = "/__requests";

  /** Forgets everything received so far, and stops refusing requests. */
  private static final String FORGET_PATH = "/__forget";

  /** Answers with the registrations of workflow modules, which are never forgotten. */
  private static final String REGISTRATIONS_PATH = "/__registrations";

  private static final List<Request> RECEIVED = Collections.synchronizedList(new LinkedList<>());

  private static final List<Request> REGISTRATIONS = Collections
      .synchronizedList(new LinkedList<>());

  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  private CockpitServer() {
  }

  /**
   * @return Where the application has to send its events, starting the server if this JVM has
   *         none yet
   */
  public static synchronized String baseUrl() {

    final var running = System.getProperty(URL_PROPERTY);
    if (running != null) {
      return running;
    }
    final var url = start();
    System.setProperty(URL_PROPERTY, url);
    return url;

  }

  private static String start() {

    final HttpServer server;
    try {
      server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    } catch (final IOException e) {
      throw new IllegalStateException("Could not start the cockpit server of the test", e);
    }
    server
        .createContext(
            "/",
            exchange -> {
              final var path = exchange.getRequestURI().getPath();
              if (REQUESTS_PATH.equals(path)) {
                answerWith(exchange, encode(RECEIVED));
                return;
              }
              if (REGISTRATIONS_PATH.equals(path)) {
                answerWith(exchange, encode(REGISTRATIONS));
                return;
              }
              final var body = new String(
                  exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
              if (FORGET_PATH.equals(path)) {
                RECEIVED.clear();
                answerWith(exchange, "");
                return;
              }
              final var request = new Request(path, body);
              RECEIVED.add(request);
              if (path.contains("/workflow-module/")) {
                REGISTRATIONS.add(request);
              }
              exchange.sendResponseHeaders(200, -1);
              exchange.close();
            });
    server.start();
    return "http://localhost:%d".formatted(server.getAddress().getPort());

  }

  private static void answerWith(
      final com.sun.net.httpserver.HttpExchange exchange,
      final String content) throws IOException {

    final var bytes = content.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();

  }

  private static String encode(
      final List<Request> requests) {

    synchronized (requests) {
      return String
          .join(
              "\n",
              requests
                  .stream()
                  .map(
                      request -> "%s\t%s".formatted(
                          request.path(),
                          Base64
                              .getEncoder()
                              .encodeToString(request.body().getBytes(StandardCharsets.UTF_8))))
                  .toList());
    }

  }

  private static List<Request> decode(
      final String content) {

    if (content.isBlank()) {
      return List.of();
    }
    return content.lines().map(line -> {
      final var separator = line.indexOf('\t');
      return new Request(
          line.substring(0, separator), new String(
              Base64.getDecoder().decode(line.substring(separator + 1)), StandardCharsets.UTF_8));
    }).toList();

  }

  private static String get(
      final String path) {

    try {
      return CLIENT
          .send(
              HttpRequest.newBuilder(URI.create(baseUrl() + path)).GET().build(),
              HttpResponse.BodyHandlers.ofString())
          .body();
    } catch (final IOException e) {
      throw new IllegalStateException("Could not ask the cockpit server of the test", e);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while asking the cockpit server", e);
    }

  }

  /**
   * Forgets everything received so far.
   */
  public static void forgetRequests() {

    get(FORGET_PATH);

  }

  /**
   * @return Everything received so far
   */
  public static List<Request> received() {

    return decode(get(REQUESTS_PATH));

  }

  /**
   * Waits for a request whose path ends with the given text.
   *
   * @param pathSuffix What the path has to end with
   * @return The request
   */
  public static Request awaitRequest(
      final String pathSuffix) {

    return await(
        () -> received().stream().filter(request -> request.path().endsWith(pathSuffix)),
        "No request ending in '%s' arrived".formatted(pathSuffix));

  }

  /**
   * Waits for the registration of a workflow module, which happened while the application
   * started and is therefore remembered separately from everything a single test provokes.
   *
   * @return The registration
   */
  public static Request awaitRegistration() {

    return await(
        () -> decode(get(REGISTRATIONS_PATH)).stream(), "No workflow module was registered");

  }

  private static Request await(
      final java.util.function.Supplier<java.util.stream.Stream<Request>> candidates,
      final String failure) {

    final var deadline = System.currentTimeMillis() + 15000;
    while (System.currentTimeMillis() < deadline) {
      final var match = candidates.get().findFirst();
      if (match.isPresent()) {
        return match.get();
      }
      sleep();
    }
    throw new AssertionError(
        "%s. Received: %s".formatted(failure, received().stream().map(Request::path).toList()));

  }

  /**
   * Waits until nothing arrived for a moment, so that a test asserting that nothing is sent
   * does not pass by being quick.
   */
  public static void awaitQuiet() {

    final var deadline = System.currentTimeMillis() + 3000;
    var lastCount = -1;
    while (System.currentTimeMillis() < deadline) {
      final var count = received().size();
      if (count == lastCount) {
        return;
      }
      lastCount = count;
      sleep();
    }

  }

  private static void sleep() {

    try {
      Thread.sleep(250);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for the cockpit server", e);
    }

  }

}
