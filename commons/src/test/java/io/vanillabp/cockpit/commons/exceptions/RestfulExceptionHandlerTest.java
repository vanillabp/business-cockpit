package io.vanillabp.cockpit.commons.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RestfulExceptionHandlerTest {

    private RestfulExceptionHandler handler;
    private Logger mockLogger;

    @BeforeEach
    void setUp() throws Exception {
        handler = new RestfulExceptionHandler();
        mockLogger = mock(Logger.class);

        // Inject mock logger using reflection
        Field loggerField = RestfulExceptionHandler.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(handler, mockLogger);
    }

    @Test
    void handleValidationException_returnsBadRequest() {
        BcValidationException exception = new BcValidationException("Validation failed");

        ResponseEntity<Object> response = handler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    @Test
    void handleUserMessageException_returnsUnprocessableEntity() {
        BcUserMessageException exception = new BcUserMessageException("User message");

        ResponseEntity<String> response = handler.handleUserMessageException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isEqualTo("User message");
    }

    @Test
    void handleForbiddenException_withBcForbiddenException_returnsForbidden() {
        BcForbiddenException exception = new BcForbiddenException("Access denied");

        ResponseEntity<String> response = handler.handleForbiddenException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("Access denied");
    }

    @Test
    void handleForbiddenException_withAccessDeniedException_returnsForbidden() {
        AccessDeniedException exception = new AccessDeniedException("No access");

        ResponseEntity<String> response = handler.handleForbiddenException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("No access");
    }

    @Test
    void handleUnauthorizedException_withBcUnauthorizedException_returnsUnauthorized() {
        BcUnauthorizedException exception = new BcUnauthorizedException("Not authenticated");

        ResponseEntity<String> response = handler.handleUnauthorizedException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Not authenticated");
    }

    @Test
    void handleUnauthorizedException_withCredentialsNotFound_returnsUnauthorized() {
        AuthenticationCredentialsNotFoundException exception =
                new AuthenticationCredentialsNotFoundException("Credentials not found");

        ResponseEntity<String> response = handler.handleUnauthorizedException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Credentials not found");
    }

    @Test
    void handleUnexpectedException_returnsInternalServerError() {
        Exception exception = new RuntimeException("Unexpected error");

        ResponseEntity<String> response = handler.handleUnexpectedException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Unexpected error");
    }

    @Test
    void handle_httpMessageNotReadableException_doesNotThrow() {
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

        // This method returns void, just verify it doesn't throw
        handler.handle(exception);
    }

    @Test
    void restError_hasCodeField() {
        RestfulExceptionHandler.RestError error = new RestfulExceptionHandler.RestError();
        error.code = 404;

        assertThat(error.code).isEqualTo(404);
    }
}
