package io.vanillabp.cockpit.commons.exceptions;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class RestfulExceptionHandler {

    public static class RestError {

        public int code;
    }

    @Autowired
    private Logger logger;

    @ExceptionHandler(BcValidationException.class)
    public ResponseEntity<Object> handleValidationException(
            final Exception exception) {

        logger.debug("Validation failed", exception);

        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(((BcValidationException) exception).getViolations());

    }

    @ExceptionHandler(BcUserMessageException.class)
    public ResponseEntity<String> handleUserMessageException(
            final Exception exception) {

        logger.debug("Unprocessable entity", exception);

        return ResponseEntity
                .unprocessableEntity()
                .body(exception.getMessage());

    }

    @ExceptionHandler(BcForbiddenException.class)
    public ResponseEntity<String> handleForbiddenException(
            final Exception exception) {

        logger.debug("Forbidden", exception);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(exception.getMessage());

    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpectedException(
            final Exception exception) {

        logger.warn("Unexpected exeception", exception);

        return ResponseEntity
                .internalServerError()
                .body(exception.getMessage());

    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public void handle(
            final HttpMessageNotReadableException e) {
        
        logger.warn("Returning HTTP 400 Bad Request", e);
        
    }

}
