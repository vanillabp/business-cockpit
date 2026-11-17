package io.vanillabp.cockpit.gui.api.v1.client.autoconfigure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/** The cockpit of the tests: a stub server which answers what a test queues. */
final class Cockpit {

  private Cockpit() {
  }

  /** The address of the official GUI API, the value of 'base-url'. Starts the stub once. */
  static String guiApiUrl(
      final MockWebServer server) {

    if (!server.getStarted()) {
      try {
        server.start(0);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return server.url("/gui/api/v1").toString();

  }

  static MockResponse userTask(
      final String id,
      final String assignee) {

    return new MockResponse.Builder()
        .code(200)
        .addHeader("Content-Type", "application/json")
        .body("""
            {"id": "%s", "workflowModuleId": "loan-approval", "assignee": {"id": "%s"}}"""
            .formatted(id, assignee))
        .build();

  }

  static MockResponse currentUser(
      final String id,
      final String group) {

    return new MockResponse.Builder()
        .code(200)
        .addHeader("Content-Type", "application/json")
        .body("""
            {"id": "%s", "display": "Doe, Jane", "groups": [{"id": "%s"}]}"""
            .formatted(id, group))
        .build();

  }

  /** The next request the stub got, or a failure if none came within a few seconds. */
  static RecordedRequest next(
      final MockWebServer server) {

    try {
      final var request = server.takeRequest(5, TimeUnit.SECONDS);
      if (request == null) {
        throw new AssertionError("The cockpit got no request");
      }
      return request;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    }

  }

}
