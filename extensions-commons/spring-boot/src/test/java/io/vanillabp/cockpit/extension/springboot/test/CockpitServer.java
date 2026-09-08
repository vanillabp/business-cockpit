package io.vanillabp.cockpit.extension.springboot.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
              if (REFUSALS_LEFT.getAndUpdate(left -> left > 0 ? left - 1 : 0) > 0) {
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
   * @param count How many of the next requests are answered with a failure, so that the outbox
   *          has to try again
   */
  public static void refuseNextRequests(
      final int count) {

    REFUSALS_LEFT.set(count);

  }

  /**
   * Forgets everything received so far.
   */
  public static void forgetRequests() {

    RECEIVED.clear();
    REFUSALS_LEFT.set(0);

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

    final var deadline = System.currentTimeMillis() + 10000;
    while (System.currentTimeMillis() < deadline) {
      final var match = received()
          .stream()
          .filter(request -> request.path().endsWith(pathSuffix))
          .findFirst();
      if (match.isPresent()) {
        return match.get();
      }
      sleep();
    }
    throw new AssertionError(
        "No request ending in '%s' arrived. Received: %s"
            .formatted(pathSuffix, received().stream().map(Request::path).toList()));

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
