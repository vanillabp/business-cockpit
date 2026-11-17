package io.vanillabp.cockpit.support.rest.httpservice;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/** Small helpers around the stub server, so the tests read like the feature they show. */
public final class Stubs {

  private Stubs() {
  }

  /** Starts the server once. Called from {@code @DynamicPropertySource}, which runs before the context. */
  public static MockWebServer started(
      final MockWebServer server) {

    if (!server.getStarted()) {
      try {
        server.start(0);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return server;

  }

  /** The base URL of the API on the stub server. */
  public static String apiUrl(
      final MockWebServer server) {

    return started(server).url("/api").toString();

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
