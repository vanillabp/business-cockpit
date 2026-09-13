package io.vanillabp.cockpit.extension.springboot.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;

/**
 * The cockpit server, played by the test: it records every request and answers with whatever
 * the test told it to answer.
 * <p>
 * It is started once for the whole JVM, before the application boots, because the application
 * is configured with its address. Refusing requests for a while is how a test proves that
 * nothing is lost while the real server is down.
 */
public final class CockpitServer {

  /** One request the cockpit server received. */
  public record Request(
                        String path,
                        String body) {
  }

  private static final HttpServer SERVER;

  private static final List<Request> RECEIVED = Collections.synchronizedList(new LinkedList<>());

  private static final List<Request> REGISTRATIONS = Collections
      .synchronizedList(new LinkedList<>());

  private static final AtomicInteger REFUSALS_LEFT = new AtomicInteger();

  private static final AtomicReference<String> REFUSED_MARKER = new AtomicReference<>();

  static {
    try {
      SERVER = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    } catch (final IOException e) {
      throw new IllegalStateException("Could not start the cockpit server of the test", e);
    }
    SERVER
        .createContext(
            "/",
            exchange -> {
              final var body = new String(
                  exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
              final var refusedMarker = REFUSED_MARKER.get();
              if (((refusedMarker == null) || body.contains(refusedMarker)) && (REFUSALS_LEFT
                  .getAndUpdate(left -> left > 0 ? left - 1 : 0) > 0)) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
                return;
              }
              final var request = new Request(exchange.getRequestURI().getPath(), body);
              RECEIVED.add(request);
              if (request.path().contains("/workflow-module/")) {
                // kept apart from the rest: a workflow module registers once while the
                // application starts, long before the test which asserts it runs
                REGISTRATIONS.add(request);
              }
              exchange.sendResponseHeaders(200, -1);
              exchange.close();
            });
    SERVER.start();
  }

  private CockpitServer() {
  }

  /**
   * @return Where the application has to send its events
   */
  public static String baseUrl() {

    return "http://localhost:%d".formatted(SERVER.getAddress().getPort());

  }

  /**
   * Answers the next requests about one event with a failure, so that the outbox has to try
   * again.
   * <p>
   * Which event it is has to be said, because every test class of this module talks to this one
   * server and the contexts Spring keeps cached go on dispatching while another class runs. A
   * refusal which took whatever arrived next was spent on a report of a class which had long
   * finished, and the test then waited for a repetition nobody owed it.
   *
   * @param marker Something only the body of the event under test contains, its event id
   * @param count How many requests about it are refused
   */
  public static void refuseRequestsAbout(
      final String marker,
      final int count) {

    REFUSED_MARKER.set(marker);
    REFUSALS_LEFT.set(count);

  }

  /**
   * Forgets everything received so far.
   */
  public static void forgetRequests() {

    RECEIVED.clear();
    REFUSALS_LEFT.set(0);
    REFUSED_MARKER.set(null);

  }

  /**
   * Waits for the registration of a workflow module, which happened while the application
   * started and is therefore remembered separately from everything a single test provokes.
   *
   * @return The registration
   */
  public static Request awaitRegistration() {

    final var deadline = System.currentTimeMillis() + 10000;
    while (System.currentTimeMillis() < deadline) {
      synchronized (REGISTRATIONS) {
        if (!REGISTRATIONS.isEmpty()) {
          return REGISTRATIONS.getFirst();
        }
      }
      sleep();
    }
    throw new AssertionError("No workflow module was registered");

  }

  /**
   * @return Everything received so far
   */
  public static List<Request> received() {

    synchronized (RECEIVED) {
      return List.copyOf(RECEIVED);
    }

  }

  /**
   * Waits for a request whose path ends with the given text.
   *
   * @param pathSuffix What the path has to end with
   * @return The request
   * @throws AssertionError If nothing arrived within ten seconds
   */
  public static Request awaitRequest(
      final String pathSuffix) {

    return awaitRequest(pathSuffix, "");

  }

  /**
   * Waits for one request about one event, which is what a test asserts on where a report of
   * another test class may arrive at the same path while it runs.
   *
   * @param pathSuffix What the path of the awaited request ends in
   * @param marker Something only the body of the awaited event contains, its event id
   * @return The request
   */
  public static Request awaitRequest(
      final String pathSuffix,
      final String marker) {

    // half a minute rather than the ten seconds this used to wait: a test which boots the
    // application spends most of its time on that, and the report it then waits for was arriving
    // just past the deadline on a loaded machine. A slow answer is not the failure under test
    // here, so waiting longer costs nothing but the seconds of a build which is red anyway.
    final var deadline = System.currentTimeMillis() + 30000;
    while (System.currentTimeMillis() < deadline) {
      final var match = received()
          .stream()
          .filter(request -> request.path().endsWith(pathSuffix))
          .filter(request -> request.body().contains(marker))
          .findFirst();
      if (match.isPresent()) {
        return match.get();
      }
      sleep();
    }
    throw new AssertionError(
        "No request ending in '%s' about '%s' arrived. Received: %s"
            .formatted(pathSuffix, marker, received().stream().map(Request::path).toList()));

  }

  /**
   * Waits until nothing arrived for half a second, so that a test asserting that nothing is
   * sent does not pass by being quick.
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
