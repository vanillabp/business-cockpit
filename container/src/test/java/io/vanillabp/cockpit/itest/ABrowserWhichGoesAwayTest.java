package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import io.vanillabp.cockpit.gui.api.v1.UpdateStreams;
import io.vanillabp.integration.test.utils.CapturedOutput;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A browser goes away all the time: the user closes a tab or reloads the page, and the update
 * stream ends from the browser's side. That is no error, so the log stays quiet about it. The next
 * ping of the stream finds the connection gone. Tomcat then dispatches the request once more, and
 * Spring answers the failed write with an exception of its own. Neither may end up in the log as an
 * error or a warning, and the stream is closed on the server as well.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ABrowserWhichGoesAwayTest extends ItestBase {

  @Autowired
  private UpdateStreams updateStreams;

  @Test
  void aClosedUpdateStreamLeavesNoErrorInTheLog(
      final CapturedOutput output) throws Exception {

    final var cookie = loginToGui(USER_MARTIN);
    final var streamsBefore = updateStreams.openStreams().size();
    final var cockpit = URI.create(url("/"));

    try (var browser = new Socket(cockpit.getHost(), cockpit.getPort())) {
      final var request = """
          GET /gui/api/v1/updates HTTP/1.1\r
          Host: %s:%d\r
          Accept: text/event-stream\r
          Cookie: %s\r
          \r
          """.formatted(cockpit.getHost(), cockpit.getPort(), cookie);
      browser.getOutputStream().write(request.getBytes(StandardCharsets.UTF_8));
      browser.getOutputStream().flush();
      final var answer = new BufferedReader(new InputStreamReader(browser.getInputStream(), StandardCharsets.UTF_8));
      assertThat(answer.readLine()).startsWith("HTTP/1.1 200");
      // the first ping says that the stream is open
      String line;
      do {
        line = answer.readLine();
      } while ((line != null) && !line.equals("event:ping"));
      assertThat(line).isEqualTo("event:ping");
    }

    // every ping writes to the closed connection, until the server notices and ends the stream
    await().untilAsserted(() -> {
      updateStreams.cleanupUpdateEmitters();
      assertThat(updateStreams.openStreams()).hasSize(streamsBefore);
    });
    // the dispatch after the failed write runs on a thread of its own, so it gets a moment
    await().pollDelay(Duration.ofMillis(500)).untilAsserted(() -> assertThat(output.getAllOfThisTest())
        .doesNotContain("AuthenticationCredentialsNotFoundException")
        .doesNotContain("AsyncRequestNotUsableException")
        .doesNotContain("disconnected client")
        .doesNotContain("Broken pipe")
        .doesNotContain("Could not ping"));

  }

}
