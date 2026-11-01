package io.vanillabp.cockpit.config.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An unknown path gets the shell of the single-page application if the application has one, and
 * a 404 if it has none. The jar of this module carries no shell since the Maven build stopped
 * building the user interface, see decision 62 in the repository's DECISIONS.md.
 */
@ExtendWith(SuppressOutputExtension.class)
class SpaNoHandlerFoundExceptionHandlerTest {

  private SpaNoHandlerFoundExceptionHandler handlerWithShellAt(
      final String defaultFile) {

    final var handler = new SpaNoHandlerFoundExceptionHandler();
    ReflectionTestUtils.setField(handler, "defaultFile", defaultFile);
    ReflectionTestUtils.setField(handler, "resourceLoader", new DefaultResourceLoader());
    return handler;

  }

  @Test
  void anApplicationWithoutAShellAnswersNotFound() {

    final var response = handlerWithShellAt("classpath:/static/index.html").handleNotFound();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNull();

  }

  /**
   * The library has no value for {@code application.spa-default-file}. Without one, an unknown
   * path gets 404 and not an error.
   */
  @Test
  void anApplicationWhichNamesNoShellAnswersNotFound() throws Exception {

    assertThat(handlerWithShellAt(null).handleNotFound().getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(handlerWithShellAt("").handleNotFound().getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    // the empty default after the colon: the library names no file, the module container does
    assertThat(SpaNoHandlerFoundExceptionHandler.class
        .getDeclaredField("defaultFile")
        .getAnnotation(Value.class)
        .value())
        .isEqualTo("${application.spa-default-file:}");

  }

  @Test
  void anApplicationWithAShellAnswersWithIt() {

    // any file on the class path stands in for a shell here
    final var response = handlerWithShellAt("classpath:/api/gui/v1.yaml").handleNotFound();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().exists()).isTrue();

  }

}
