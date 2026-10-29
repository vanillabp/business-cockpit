package io.vanillabp.cockpit.commons.exceptions;

import java.io.InputStream;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * The status code every kind of exception turns into. These numbers are part of what a
 * client of the cockpit's REST APIs sees, so they are pinned as plain integers. A renamed
 * constant in Spring must not be able to move them.
 */
@ExtendWith(SuppressOutputExtension.class)
public class RestfulExceptionHandlerTest {

  private RestfulExceptionHandler handler;

  @BeforeEach
  public void buildHandler() throws Exception {

    handler = new RestfulExceptionHandler();

    // the handler gets its logger injected into a field, so the test fills that field itself
    final var loggerField = RestfulExceptionHandler.class.getDeclaredField("logger");
    loggerField.setAccessible(true);
    loggerField.set(handler, LoggerFactory.getLogger(RestfulExceptionHandlerTest.class));

  }

  @Test
  public void testValidationFailureIsBadRequestAndCarriesTheViolations() {

    final var exception = new BcValidationException("title is missing");

    final var response = handler.handleValidationException(exception);

    Assertions.assertThat(response.getStatusCode().value()).isEqualTo(400);
    Assertions.assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    Assertions.assertThat(response.getBody()).isEqualTo(exception.getViolations());

  }

  @Test
  public void testUserMessageIsUnprocessableContentAndCarriesTheMessage() {

    final var response = handler.handleUserMessageException(
        new BcUserMessageException("this task was completed by somebody else"));

    Assertions.assertThat(response.getStatusCode().value()).isEqualTo(422);
    Assertions.assertThat(response.getBody()).isEqualTo("this task was completed by somebody else");

  }

  @Test
  public void testForbiddenKeepsItsMessage() {

    Assertions
        .assertThat(handler.handleForbiddenException(new BcForbiddenException("no")).getStatusCode().value())
        .isEqualTo(403);

    Assertions
        .assertThat(handler.handleForbiddenException(new AccessDeniedException("no")).getBody())
        .isEqualTo("no");

  }

  @Test
  public void testMissingCredentialsAreUnauthorized() {

    Assertions
        .assertThat(handler.handleUnauthorizedException(new BcUnauthorizedException("who?")).getStatusCode()
            .value())
        .isEqualTo(401);

    Assertions
        .assertThat(handler
            .handleUnauthorizedException(new AuthenticationCredentialsNotFoundException("who?")).getBody())
        .isEqualTo("who?");

  }

  /** A report as a controller would take it, for the parameter the validation is about. */
  public static class Report {

    public String timestamp;

    public String details;

  }

  @SuppressWarnings("unused")
  private void receive(
      final Report report) {
  }

  @Test
  public void anInvalidBodyIsBadRequestAndNamesEveryFieldButNoValue() throws Exception {

    final var report = new Report();
    report.details = "a value the answer must not repeat";
    final var errors = new BeanPropertyBindingResult(report, "report");
    errors.addError(new FieldError(
        "report", "timestamp", null, false, new String[]{
            "NotNull.report.timestamp", "NotNull"
        }, null, "darf nicht null sein"));
    errors.addError(new FieldError(
        "report", "details", report.details, false, new String[]{
            "Size.report.details", "Size"
        }, null, "size must be between 0 and 3"));
    errors.addError(new ObjectError("report", new String[]{
        "SameIds"
    }, null, "ids differ"));
    errors.addError(new FieldError("report", "details", null, false, null, null, "no code at all"));
    final var parameter = new MethodParameter(
        RestfulExceptionHandlerTest.class.getDeclaredMethod("receive", Report.class), 0);

    final var response = handler.handleInvalidRequestBody(new MethodArgumentNotValidException(parameter, errors));

    Assertions.assertThat(response.getStatusCode().value()).isEqualTo(400);
    Assertions.assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    Assertions.assertThat(response.getBody()).isEqualTo(
        "The request is not valid: 'details' breaks the rule @Size, 'details' is not valid, "
            + "'report' breaks the rule @SameIds, 'timestamp' is missing.");

  }

  @Test
  public void aMistakeAControllerFoundIsAnsweredLikeABrokenSchema() {

    final var response = handler.handleInvalidRequest(
        new BcInvalidRequestException("'pageSize' is missing", "'details' is not valid"));

    Assertions.assertThat(response.getStatusCode().value()).isEqualTo(400);
    Assertions.assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    Assertions.assertThat(response.getBody()).isEqualTo(
        "The request is not valid: 'details' is not valid, 'pageSize' is missing.");

  }

  @Test
  public void testAnythingElseIsAnInternalServerError() {

    final var response = handler.handleUnexpectedException(new RuntimeException("boom"));

    Assertions.assertThat(response.getStatusCode().value()).isEqualTo(500);
    Assertions.assertThat(response.getBody()).isEqualTo("boom");

  }

  @Test
  public void testAnUnreadableBodyIsAnsweredWithoutOne() {

    final var emptyRequest = new HttpInputMessage() {
      @Override
      public InputStream getBody() {
        return InputStream.nullInputStream();
      }

      @Override
      public HttpHeaders getHeaders() {
        return new HttpHeaders();
      }
    };

    final var response = handler.handleUnreadableRequestBody(
        new HttpMessageNotReadableException("no JSON in here", emptyRequest));

    Assertions.assertThat(response.getStatusCode().value()).isEqualTo(400);
    Assertions.assertThat(response.getBody()).isNull();

  }

  /** A field which takes a fixed set of names, like the mode of a list in the GUI API. */
  enum Mode {
    ALL("All"),
    OPEN("Open");

    private final String value;

    Mode(
        final String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return value;
    }

    // the same kind of factory the OpenAPI generator writes for every enum
    @JsonCreator
    public static Mode fromValue(
        final String value) {
      return Arrays
          .stream(values())
          .filter(mode -> mode.value.equals(value))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Unexpected value '"
              + value
              + "'"));
    }
  }

  record ListRequest(Mode mode) {
  }

  @Test
  public void testANameTheFieldDoesNotKnowIsAnsweredWithTheNamesItKnows() {

    final var response = handler.handleUnreadableRequestBody(unreadable("{ \"mode\": \"Gone\" }"));

    Assertions.assertThat(response.getStatusCode().value()).isEqualTo(400);
    Assertions
        .assertThat(response.getBody())
        .isEqualTo("The request is not valid: 'mode' takes one of 'All', 'Open'.")
        .doesNotContain("Gone");

  }

  @Test
  public void testAValueOfTheWrongKindIsAnsweredWithTheField() {

    final var response = handler.handleUnreadableRequestBody(unreadable("{ \"mode\": [ 1 ] }"));

    Assertions.assertThat(response.getStatusCode().value()).isEqualTo(400);
    Assertions.assertThat(response.getBody()).startsWith("The request is not valid: 'mode' ");

  }

  private static HttpMessageNotReadableException unreadable(
      final String json) {

    final var request = new HttpInputMessage() {
      @Override
      public InputStream getBody() {
        return InputStream.nullInputStream();
      }

      @Override
      public HttpHeaders getHeaders() {
        return new HttpHeaders();
      }
    };
    try {
      JsonMapper.builder().build().readValue(json, ListRequest.class);
    } catch (JacksonException e) {
      return new HttpMessageNotReadableException(e.getOriginalMessage(), e, request);
    }
    throw new IllegalStateException("The body was read: "
        + json);

  }

}
