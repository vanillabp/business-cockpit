package io.vanillabp.cockpit.config.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.vanillabp.cockpit.exceptions.BcContentTooLargeException;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The cockpit ships two limits for the size of what a request sends: the GUI API reads a body of
 * at most 1 MB, and Jackson reads a string of at most 10,000,000 characters from any JSON body.
 * An application can set both higher or lower.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheSizeOfARequestHasADefaultLimitTest {

  private static final int ONE_MEGABYTE = 1024 * 1024;

  private static GuiApiRequestBodyLimit.LimitFilter filterOf(
      final MockEnvironment environment) {

    return new GuiApiRequestBodyLimit().guiApiRequestBodyLimitFilter(environment).getFilter();

  }

  @Test
  void aBodyLargerThanOneMegabyteIsRefusedBeforeItIsRead() throws Exception {

    final var request = new MockHttpServletRequest("PATCH", "/gui/api/v1/usertask/note");
    request.setContent(new byte[ONE_MEGABYTE + 1]);
    final var response = new MockHttpServletResponse();
    final var reached = new boolean[]{
        false
    };

    filterOf(new MockEnvironment()).doFilter(request, response, (
        req,
        res) -> reached[0] = true);

    assertThat(response.getStatus()).isEqualTo(413);
    assertThat(response.getContentType()).startsWith("text/plain");
    assertThat(response.getContentAsString()).contains(String.valueOf(ONE_MEGABYTE));
    assertThat(reached[0]).isFalse();

  }

  @Test
  void aBodyWithoutALengthIsRefusedWhenItsCountGoesOverTheLimit() throws Exception {

    final var request = new MockHttpServletRequest("PATCH", "/gui/api/v1/usertask/note") {
      @Override
      public long getContentLengthLong() {
        return -1;
      }
    };
    request.setContent(new byte[ONE_MEGABYTE + 1]);

    assertThatThrownBy(() -> filterOf(new MockEnvironment())
        .doFilter(request, new MockHttpServletResponse(), (
            req,
            res) -> req.getInputStream().readAllBytes()))
        .isInstanceOf(BcContentTooLargeException.class);

  }

  /** The positive twin: a body of exactly the limit is read in full. */
  @Test
  void aBodyOfOneMegabyteIsRead() throws Exception {

    final var request = new MockHttpServletRequest("PATCH", "/gui/api/v1/usertask/note") {
      @Override
      public long getContentLengthLong() {
        return -1;
      }
    };
    request.setContent(new byte[ONE_MEGABYTE]);
    final var read = new int[1];

    filterOf(new MockEnvironment())
        .doFilter(request, new MockHttpServletResponse(),
            (
                req,
                res) -> read[0] = req.getInputStream().readAllBytes().length);

    assertThat(read[0]).isEqualTo(ONE_MEGABYTE);

  }

  @Test
  void anApplicationSetsItsOwnLimitForTheBody() throws Exception {

    final var request = new MockHttpServletRequest("PATCH", "/gui/api/v1/usertask/note");
    request.setContent(new byte[2048]);
    final var response = new MockHttpServletResponse();

    filterOf(new MockEnvironment().withProperty(GuiApiRequestBodyLimit.MAX_REQUEST_BODY_SIZE_PROPERTY, "1KB"))
        .doFilter(request, response, (
            req,
            res) -> {
        });

    assertThat(response.getStatus()).isEqualTo(413);

  }

  @Test
  void jacksonReadsAStringOfTenMillionCharactersAtMost() {

    final var environment = new StandardEnvironment();

    new JsonStringLengthDefault().postProcessEnvironment(environment, null);

    assertThat(environment.getProperty(JsonStringLengthDefault.MAX_STRING_LENGTH_PROPERTY, Integer.class))
        .isEqualTo(10_000_000);

  }

  @Test
  void anApplicationKeepsItsOwnLimitForAString() {

    final var environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource("application", Map.of(JsonStringLengthDefault.MAX_STRING_LENGTH_PROPERTY, "500")));

    new JsonStringLengthDefault().postProcessEnvironment(environment, null);

    assertThat(environment.getProperty(JsonStringLengthDefault.MAX_STRING_LENGTH_PROPERTY, Integer.class))
        .isEqualTo(500);

  }

}
