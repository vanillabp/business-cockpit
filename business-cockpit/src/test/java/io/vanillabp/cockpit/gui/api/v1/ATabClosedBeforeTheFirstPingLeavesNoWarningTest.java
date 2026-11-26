package io.vanillabp.cockpit.gui.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.vanillabp.cockpit.config.properties.ApplicationProperties;
import io.vanillabp.cockpit.gui.api.v1.UpdateStreamFixtures.Person;
import io.vanillabp.integration.test.utils.CapturedOutput;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * An update stream is pinged 300 milliseconds after it was opened, so the browser knows that the
 * stream is open. A user who closes the tab within these 300 milliseconds is not an error, so the
 * log stays quiet about it. Only a stream which is still open and cannot be written to is worth a
 * warning, because its browser keeps waiting for the ping.
 *
 * <p>The stream is opened through Spring MVC, the way the cockpit answers the browser. The
 * scheduler is a double, so each test runs the first ping by hand at the moment it wants.
 */
@ExtendWith(SuppressOutputExtension.class)
class ATabClosedBeforeTheFirstPingLeavesNoWarningTest {

  private static final Person MARTIN = Person.of("martin", "accounting");

  @RestController
  static class ControllerLikeTheGuiApi {

    private final UpdateStreams streams;

    ControllerLikeTheGuiApi(
        final UpdateStreams streams) {

      this.streams = streams;

    }

    @GetMapping("/gui/api/v1/updates")
    public SseEmitter updates() {

      return streams.subscribe(MARTIN, Optional.empty());

    }

  }

  private final AtomicBoolean connectionLost = new AtomicBoolean();

  private TaskScheduler taskScheduler;

  private UpdateStreams streams;

  @BeforeEach
  void noStreamOpenYet() {

    taskScheduler = mock(TaskScheduler.class);
    streams = new UpdateStreams(new ApplicationProperties(), taskScheduler, List.of());

  }

  @Test
  void aBrowserWhichWentAwayBeforeTheFirstPingLeavesNoWarning(
      final CapturedOutput output) throws Exception {

    openStream(cockpitLike());

    // the next write finds the connection gone, the way a servlet container reports it
    connectionLost.set(true);
    firstPing().run();

    assertThat(streams.openStreams())
        .as("the stream is closed on the server as well")
        .isEmpty();
    assertThat(warningsAndErrors(output)).isEmpty();

  }

  @Test
  void aStreamTheContainerReportedClosedBeforeTheFirstPingLeavesNoWarning(
      final CapturedOutput output) throws Exception {

    final var stream = openStream(cockpitLike());

    final var asyncContext = (MockAsyncContext) stream.getRequest().getAsyncContext();
    final var event = new AsyncEvent(asyncContext, new IOException("Connection reset by peer"));
    for (final var listener : asyncContext.getListeners()) {
      listener.onError(event);
    }
    assertThat(streams.openStreams())
        .as("the error the container reported ended the stream")
        .isEmpty();
    firstPing().run();

    assertThat(warningsAndErrors(output)).isEmpty();

  }

  /**
   * The emitter is complete, but the callback which drops it from the open streams has not run
   * yet. Both happen on different threads, so the ping can come in between.
   */
  @Test
  void aStreamCompletedBeforeTheFirstPingLeavesNoWarning(
      final CapturedOutput output) throws Exception {

    openStream(cockpitLike());

    streams.openStreams().forEach(UpdateEmitter::complete);
    firstPing().run();

    assertThat(streams.openStreams()).isEmpty();
    assertThat(warningsAndErrors(output)).isEmpty();

  }

  /**
   * The stream is open, and the browser is still there, but the ping cannot be written. Here the
   * cockpit has no converter for the ping, and the browser would wait for it in vain.
   */
  @Test
  void aStreamWhichIsOpenButCannotBeWrittenToWarns(
      final CapturedOutput output) throws Exception {

    openStream(cockpitLike().setMessageConverters(new StringHttpMessageConverter()));

    firstPing().run();

    assertThat(warningsAndErrors(output))
        .singleElement()
        .asString()
        .contains("Could not send the first ping to update stream '")
        .contains("so its browser may hang");

  }

  private StandaloneMockMvcBuilder cockpitLike() {

    return MockMvcBuilders
        .standaloneSetup(new ControllerLikeTheGuiApi(streams))
        .addFilters(connectionWhichCanBeLost());

  }

  private MvcResult openStream(
      final StandaloneMockMvcBuilder cockpit) throws Exception {

    final var stream = cockpit.build().perform(get("/gui/api/v1/updates")).andReturn();
    assertThat(stream.getRequest().isAsyncStarted()).isTrue();
    assertThat(streams.openStreams()).hasSize(1);
    return stream;

  }

  private Runnable firstPing() {

    final var scheduled = ArgumentCaptor.forClass(Runnable.class);
    verify(taskScheduler).schedule(scheduled.capture(), any(Instant.class));
    return scheduled.getValue();

  }

  private static List<String> warningsAndErrors(
      final CapturedOutput output) {

    return output
        .getAllOfThisTest()
        .lines()
        .filter(line -> line.contains(" WARN ") || line.contains(" ERROR "))
        .toList();

  }

  /**
   * Writes like the connection of a servlet container, which fails the first write after the
   * browser closed it. Tomcat throws its {@code ClientAbortException} then, with the message of
   * the operating system.
   */
  private Filter connectionWhichCanBeLost() {

    return (
        request,
        response,
        chain) -> chain.doFilter(request, new HttpServletResponseWrapper(
            (HttpServletResponse) response) {

          @Override
          public ServletOutputStream getOutputStream() throws IOException {

            final var connection = super.getOutputStream();
            return new ServletOutputStream() {

              @Override
              public boolean isReady() {
                return connection.isReady();
              }

              @Override
              public void setWriteListener(
                  final WriteListener writeListener) {
                connection.setWriteListener(writeListener);
              }

              @Override
              public void write(
                  final int b) throws IOException {
                failIfLost();
                connection.write(b);
              }

              @Override
              public void write(
                  final byte[] b,
                  final int off,
                  final int len) throws IOException {
                failIfLost();
                connection.write(b, off, len);
              }

              @Override
              public void flush() throws IOException {
                failIfLost();
                connection.flush();
              }

              private void failIfLost() throws IOException {
                if (connectionLost.get()) {
                  throw new IOException("Broken pipe");
                }
              }

            };

          }

        });

  }

}
