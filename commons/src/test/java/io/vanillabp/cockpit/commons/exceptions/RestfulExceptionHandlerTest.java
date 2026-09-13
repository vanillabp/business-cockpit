package io.vanillabp.cockpit.commons.exceptions;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.io.InputStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

/**
 * The status code every kind of exception turns into. These numbers are part of what a
 * client of the cockpit's REST APIs sees, so they are pinned as plain integers: a renamed
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

        // the status comes from the annotation on the method, all the method itself does is log
        Assertions
                .assertThatNoException()
                .isThrownBy(() -> handler.handle(
                        new HttpMessageNotReadableException("no JSON in here", emptyRequest)));

    }

}
