package io.vanillabp.cockpit.extension.test.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.vanillabp.integration.test.utils.FreePortUtil;

/**
 * The cockpit server, played by the test: it records every request and answers with whatever the
 * test told it to answer.
 * <p>
 * It is started once per JVM, before an application boots, because an application is configured
 * with its address. Every question a test asks it goes over HTTP, to whichever copy of this class
 * started the server. That detour exists for the Quarkus tests: an extension test initializes its
 * test class TWICE, once while the application is being built and again inside the class loader of
 * the running application, and those two copies share no static field. Without the detour a test
 * would read an empty list while the application reported into the other copy, which looks exactly
 * like an extension that never reported anything. On Spring Boot there is one copy and the detour
 * is a call to localhost.
 * <p>
 * Three numbers differ between the repositories using this server, and each is a system property
 * rather than a subclass:
 *
 * <pre>
 * businesscockpit.test-server.wait-millis          how long a test waits for a request (30 s)
 * businesscockpit.test-server.quiet-window-millis  how long nothing may arrive to count as quiet (1 s)
 * businesscockpit.test-server.quiet-wait-millis    how long {@link #awaitQuiet()} hopes for that (30 s)
 * </pre>
 *
 * A repository whose reports travel through a cluster raises the first one. A system property
 * carries across the two Quarkus class loaders, which a static field set by a test would not.
 */
public final class CockpitServer {

  /**
   * One request the server received.
   *
   * @param path What was called
   * @param body What it carried
   */
  public record Request(
                        String path,
                        String body) {
  }

  /** Where the port of the running server is published, so that a second copy finds it. */
  private static final String PORT_PROPERTY = "businesscockpit.test-server.port";

  /** What a copy asks for to read what arrived. */
  private static final String RECEIVED_PATH = "/__received";

  /** What a copy asks for to read the registrations, which are never forgotten. */
  private static final String REGISTRATIONS_PATH = "/__registrations";

  /** What a copy asks for to forget everything received so far. */
  private static final String FORGET_PATH = "/__forget";

  /** What a copy posts to make the server refuse the next reports about one event. */
  private static final String REFUSE_PATH = "/__refuse";

  /** Separates the path from the body in what the server answers, one request per line. */
  private static final char FIELD_SEPARATOR = '\t';

  /** How long a test waits for something to arrive. */
  private static final long WAIT_MILLIS = millisProperty("wait-millis", 30000);

  /** How long nothing may arrive before the server counts as quiet. */
  private static final long QUIET_WINDOW_MILLIS = millisProperty("quiet-window-millis", 1000);

  /** How long {@link #awaitQuiet()} keeps hoping for that silence. */
  private static final long QUIET_WAIT_MILLIS = millisProperty("quiet-wait-millis", 30000);

  /** How long the server sleeps between two looks. */
  private static final long POLL_MILLIS = 200;

  private static final List<Request> RECEIVED = Collections.synchronizedList(new LinkedList<>());

  /**
   * Kept apart from the rest and never forgotten: a workflow module registers itself once while
   * the application starts, long before the test asserting it runs.
   */
  private static final List<Request> REGISTRATIONS = Collections
      .synchronizedList(new LinkedList<>());

  /** How many more reports about {@link #REFUSED_MARKER} are answered with a failure. */
  private static final AtomicInteger REFUSALS_LEFT = new AtomicInteger();

  /** What the body of a refused report has to carry, or <code>null</code> for any report. */
  private static final AtomicReference<String> REFUSED_MARKER = new AtomicReference<>();

  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  private static final int PORT = startOrJoin();

  private CockpitServer() {
  }

  /**
   * Starts the server unless another copy of this class already did.
   *
   * @return The port every copy talks to
   */
  private static int startOrJoin() {

    final var published = System.getProperty(PORT_PROPERTY);
    if (published != null) {
      return Integer.parseInt(published);
    }
    final var port = FreePortUtil.getFreePort();
    final HttpServer server;
    try {
      server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
    } catch (final IOException e) {
      throw new IllegalStateException(
          "Could not start the cockpit server of the test on port "
              + port, e);
    }
    server.createContext("/", CockpitServer::answer);
    server.start();
    System.setProperty(PORT_PROPERTY, String.valueOf(port));
    return port;

  }

  private static void answer(
      final HttpExchange exchange) throws IOException {

    final var path = exchange.getRequestURI().getPath();
    if (RECEIVED_PATH.equals(path)) {
      answerWith(exchange, RECEIVED);
      return;
    }
    if (REGISTRATIONS_PATH.equals(path)) {
      answerWith(exchange, REGISTRATIONS);
      return;
    }
    final var body = new String(
        exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    if (FORGET_PATH.equals(path)) {
      RECEIVED.clear();
      REFUSALS_LEFT.set(0);
      REFUSED_MARKER.set(null);
      answerWith(exchange, List.of());
      return;
    }
    if (REFUSE_PATH.equals(path)) {
      final var order = requestsOf(body).getFirst();
      REFUSED_MARKER.set(order.body().isEmpty()
          ? null
          : order.body());
      REFUSALS_LEFT.set(Integer.parseInt(order.path()));
      answerWith(exchange, List.of());
      return;
    }
    if (refuses(body)) {
      exchange.sendResponseHeaders(503, -1);
      exchange.close();
      return;
    }
    final var request = new Request(path, body);
    RECEIVED.add(request);
    if (path.contains("/workflow-module/")) {
      REGISTRATIONS.add(request);
    }
    exchange.sendResponseHeaders(200, -1);
    exchange.close();

  }

  /**
   * @param body What the report carries
   * @return Whether this report is one of those the test asked to have refused, which also
   *         spends one of the refusals it asked for
   */
  private static boolean refuses(
      final String body) {

    final var marker = REFUSED_MARKER.get();
    if ((marker != null) && !body.contains(marker)) {
      return false;
    }
    return REFUSALS_LEFT.getAndUpdate(left -> left > 0
        ? left - 1
        : 0) > 0;

  }

  private static void answerWith(
      final HttpExchange exchange,
      final List<Request> requests) throws IOException {

    final var bytes = asLines(requests).getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();

  }

  /**
   * One request per line, the body encoded rather than escaped, so that a report carrying a line
   * break or a tab of its own is read back as the bytes it was sent as.
   *
   * @param requests What to write
   * @return The lines
   */
  private static String asLines(
      final List<Request> requests) {

    synchronized (requests) {
      return String
          .join(
              "\n",
              requests
                  .stream()
                  .map(
                      request -> request.path() + FIELD_SEPARATOR + Base64
                          .getEncoder()
                          .encodeToString(request.body().getBytes(StandardCharsets.UTF_8)))
                  .toList());
    }

  }

  private static List<Request> requestsOf(
      final String answer) {

    if (answer.isBlank()) {
      return List.of();
    }
    return answer.lines().map(line -> {
      final var separator = line.indexOf(FIELD_SEPARATOR);
      if (separator < 0) {
        return new Request(line, "");
      }
      return new Request(
          line.substring(0, separator), new String(
              Base64.getDecoder().decode(line.substring(separator + 1)), StandardCharsets.UTF_8));
    }).toList();

  }

  /**
   * @return Where an application has to send its reports
   */
  public static String baseUrl() {

    return "http://localhost:%d".formatted(PORT);

  }

  /**
   * Forgets everything received so far, and stops refusing reports. The registrations are kept.
   */
  public static void forgetRequests() {

    ask(FORGET_PATH, "");

  }

  /**
   * Answers the next reports about one event with a failure, so that the outbox has to try again.
   * <p>
   * Which event it is has to be said, because every test class of a module talks to this one
   * server and a context which is kept cached goes on dispatching while another class runs. A
   * refusal which took whatever arrived next was spent on a report of a class which had long
   * finished, and the test then waited for a repetition nobody owed it.
   *
   * @param marker Something only the body of the event under test carries, its event id
   * @param count How many reports about it are refused
   */
  public static void refuseRequestsAbout(
      final String marker,
      final int count) {

    ask(
        REFUSE_PATH,
        count + Character.toString(FIELD_SEPARATOR) + Base64.getEncoder()
            .encodeToString(marker.getBytes(StandardCharsets.UTF_8)));

  }

  /**
   * @return Everything the server received since it was last asked to forget
   */
  public static List<Request> received() {

    return requestsOf(ask(RECEIVED_PATH, ""));

  }

  /**
   * @param pathSuffix What the paths have to end with
   * @return What arrived so far and ends in that
   */
  public static List<Request> matching(
      final String pathSuffix) {

    return received()
        .stream()
        .filter(request -> request.path().endsWith(pathSuffix))
        .toList();

  }

  /**
   * Waits for the registration of a workflow module, which happened while the application started
   * and is therefore remembered separately from everything a single test provokes.
   *
   * @return The registration
   */
  public static Request awaitRegistration() {

    final var deadline = System.currentTimeMillis() + WAIT_MILLIS;
    while (System.currentTimeMillis() < deadline) {
      final var registrations = requestsOf(ask(REGISTRATIONS_PATH, ""));
      if (!registrations.isEmpty()) {
        return registrations.getFirst();
      }
      sleep();
    }
    throw new AssertionError("No workflow module was registered");

  }

  /**
   * Waits for a request whose path ends with the given text.
   *
   * @param pathSuffix What the path has to end with
   * @return The request
   */
  public static Request awaitRequest(
      final String pathSuffix) {

    return awaitRequests(pathSuffix, 1).getFirst();

  }

  /**
   * Waits for a request of one kind which is about the thing the caller means.
   * <p>
   * Every test of a class shares this server, and a report of an earlier test may arrive after
   * that test forgot what it had seen, because the dispatch of an entry outlives the test which
   * caused it. So a test which asserts content waits for the request carrying it rather than for
   * the next one of its kind.
   *
   * @param pathSuffix What the path has to end with
   * @param bodyPart What the body has to carry
   * @return The request
   */
  public static Request awaitRequest(
      final String pathSuffix,
      final String bodyPart) {

    final var deadline = System.currentTimeMillis() + WAIT_MILLIS;
    while (System.currentTimeMillis() < deadline) {
      final var match = matching(pathSuffix)
          .stream()
          .filter(request -> request.body().contains(bodyPart))
          .findFirst();
      if (match.isPresent()) {
        return match.get();
      }
      sleep();
    }
    // what arrived on that path is named in full: a caller who only sees the paths cannot tell
    // whether the report it wanted never came or came with another content, and that is the
    // question every failure here raises
    throw new AssertionError(
        "No request ending in '%s' carried '%s'. What arrived on that path: %s. All paths received: %s"
            .formatted(
                pathSuffix,
                bodyPart,
                matching(pathSuffix),
                received().stream().map(Request::path).toList()));

  }

  /**
   * Waits until the given number of requests of one kind arrived.
   *
   * @param pathSuffix What the paths have to end with
   * @param count How many are expected
   * @return The requests, in the order they arrived
   */
  public static List<Request> awaitRequests(
      final String pathSuffix,
      final int count) {

    final var deadline = System.currentTimeMillis() + WAIT_MILLIS;
    while (System.currentTimeMillis() < deadline) {
      final var matches = matching(pathSuffix);
      if (matches.size() >= count) {
        return matches;
      }
      sleep();
    }
    throw new AssertionError(
        "Fewer than %d requests ending in '%s' arrived. Received: %s"
            .formatted(count, pathSuffix, received().stream().map(Request::path).toList()));

  }

  /**
   * Waits until nothing arrived for the quiet window, so that a test asserting that something is
   * NOT reported does not pass by being quick.
   * <p>
   * A report which is on its way spends at least one outbox cycle waiting to be dispatched, so a
   * shorter silence proves nothing: it is the silence between two polls of an outbox which is
   * about to send. The window therefore spans two cycles.
   */
  public static void awaitQuiet() {

    awaitQuiet(Duration.ofMillis(QUIET_WINDOW_MILLIS));

  }

  /**
   * @param quietFor How long nothing may arrive before the server counts as quiet
   * @see #awaitQuiet()
   */
  public static void awaitQuiet(
      final Duration quietFor) {

    final var deadline = System.currentTimeMillis() + QUIET_WAIT_MILLIS;
    var lastCount = received().size();
    var quietSince = System.currentTimeMillis();
    while (System.currentTimeMillis() < deadline) {
      sleep();
      final var count = received().size();
      if (count != lastCount) {
        lastCount = count;
        quietSince = System.currentTimeMillis();
        continue;
      }
      if ((System.currentTimeMillis() - quietSince) >= quietFor.toMillis()) {
        return;
      }
    }
    throw new AssertionError(
        "Requests kept arriving for %d ms, so nothing here proves that a report stayed away. Received: %s"
            .formatted(QUIET_WAIT_MILLIS, received().stream().map(Request::path).toList()));

  }

  private static String ask(
      final String path,
      final String body) {

    try {
      return CLIENT
          .send(
              HttpRequest
                  .newBuilder(URI.create(baseUrl() + path))
                  .POST(HttpRequest.BodyPublishers.ofString(body))
                  .build(),
              HttpResponse.BodyHandlers.ofString())
          .body();
    } catch (final IOException e) {
      throw new IllegalStateException("Could not read what the cockpit server received", e);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while reading the cockpit server", e);
    }

  }

  private static long millisProperty(
      final String name,
      final long whenNothingIsSet) {

    final var value = System.getProperty("businesscockpit.test-server."
        + name);
    return (value == null) || value.isBlank()
        ? whenNothingIsSet
        : Long.parseLong(value.trim());

  }

  private static void sleep() {

    try {
      Thread.sleep(POLL_MILLIS);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for the cockpit server", e);
    }

  }

}
