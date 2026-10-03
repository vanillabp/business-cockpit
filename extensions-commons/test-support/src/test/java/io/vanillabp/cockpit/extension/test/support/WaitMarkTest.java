package io.vanillabp.cockpit.extension.test.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What a wait for a report may and may not be satisfied by. The server is driven by hand here,
 * without an application, because the question is about the wait and not about what reports.
 * <p>
 * The waits of this class are short: the module names a wait of a second for its own tests, where
 * a report arrives in the same thread which sent it.
 */
@ExtendWith(SuppressOutputExtension.class)
public class WaitMarkTest {

  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  @BeforeEach
  public void startWithAnEmptyServer() {

    CockpitServer.forgetRequests();

  }

  /**
   * Reports what an extension would report.
   *
   * @param path Where the report goes
   * @param body What it carries
   */
  private static void report(
      final String path,
      final String body) {

    try {
      CLIENT
          .send(
              HttpRequest
                  .newBuilder(URI.create(CockpitServer.baseUrl() + path))
                  .POST(HttpRequest.BodyPublishers.ofString(body))
                  .build(),
              HttpResponse.BodyHandlers.discarding());
    } catch (final IOException e) {
      throw new IllegalStateException("Could not report to the cockpit server of the test", e);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while reporting to the cockpit server", e);
    }

  }

  @Test
  @DisplayName("A wait for the next report on a collecting path is refused")
  public void aWaitOnACollectingPathIsRefused() {

    report("/usertask/created", "{\"userTaskId\":\"task-1\"}");

    final var refused = assertThrows(
        AssertionError.class,
        () -> CockpitServer.awaitAnyRequest("/usertask/created"));

    assertTrue(
        refused.getMessage().contains("awaitRequestOf(pathSuffix, bodyPart)"),
        refused.getMessage());

  }

  @Test
  @DisplayName("The refusal comes before the wait, not after it")
  public void theRefusalDoesNotWait() {

    final var startedAt = System.currentTimeMillis();

    assertThrows(
        AssertionError.class,
        () -> CockpitServer.awaitRequests("/workflow/created", 2));

    // the module waits a second for a report. A refusal which took that second would be a wait
    // which ran and then complained, and the message would reach a developer who had already
    // started looking for the report
    assertTrue(
        (System.currentTimeMillis() - startedAt) < 500,
        "the refusal waited for the report it refuses to wait for");

  }

  @Test
  @DisplayName("A wait on a path carrying the id of its case is served")
  public void aWaitOnAPathOfOneCaseIsServed() {

    report("/usertask/task-1/completed", "{\"userTaskId\":\"task-1\"}");

    assertEquals(
        "{\"userTaskId\":\"task-1\"}",
        CockpitServer.awaitAnyRequest("/usertask/task-1/completed").body());

  }

  @Test
  @DisplayName("A wait which names its report takes no other report on that path")
  public void aNamedWaitTakesNoOtherReport() {

    report("/usertask/created", "{\"userTaskId\":\"task-1\"}");

    assertEquals(
        "{\"userTaskId\":\"task-2\"}",
        reportedWhileWaiting(
            "/usertask/created", "{\"userTaskId\":\"task-2\"}", "\"userTaskId\":\"task-2\"")
            .body());

  }

  @Test
  @DisplayName("A wait which names its report falls when that report stays away")
  public void aNamedWaitFallsWithoutItsReport() {

    report("/usertask/created", "{\"userTaskId\":\"task-1\"}");

    final var fell = assertThrows(
        AssertionError.class,
        () -> CockpitServer.awaitRequestOf("/usertask/created", "\"userTaskId\":\"task-2\""));

    // the report which did arrive is named as well, because a developer reading this cannot
    // otherwise tell a report which never came from one which came with another content
    assertTrue(fell.getMessage().contains("task-1"), fell.getMessage());

  }

  /**
   * Sends a report from another thread while a wait for it is running, so that the wait is
   * satisfied by something which arrives after it started and not by what was already there.
   *
   * @param path Where the report goes
   * @param body What it carries
   * @param bodyPart What the wait names
   * @return What the wait returned
   */
  private static CockpitServer.Request reportedWhileWaiting(
      final String path,
      final String body,
      final String bodyPart) {

    final var reporter = new Thread(() -> report(path, body));
    reporter.start();
    try {
      return CockpitServer.awaitRequest(path, bodyPart);
    } finally {
      try {
        reporter.join();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

  }

}
