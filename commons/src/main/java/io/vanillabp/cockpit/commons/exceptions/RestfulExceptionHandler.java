package io.vanillabp.cockpit.commons.exceptions;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

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
                .unprocessableContent()
                .body(exception.getMessage());

    }

    @ExceptionHandler({
            BcForbiddenException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<String> handleForbiddenException(
            final Exception exception) {

        logger.debug("Forbidden", exception);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(exception.getMessage());

    }

    @ExceptionHandler({
            BcUnauthorizedException.class,
            AuthenticationCredentialsNotFoundException.class
    })
    public ResponseEntity<String> handleUnauthorizedException(
            final Exception exception) {

        logger.debug("Unauthorized", exception);

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(exception.getMessage());

    }

    /**
     * A request body which breaks the rules of its schema, for example one which leaves out a
     * required field. Sending the same request again cannot help, so the answer is
     * {@code 400 Bad Request} and not the {@code 500} of the catch-all below. A sender which
     * repeats a 500 would repeat this request until it gives up.
     * <p>
     * The body of the answer names each field and the rule it breaks. It never repeats the value
     * the request sent, which can be large or personal.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleInvalidRequestBody(
            final MethodArgumentNotValidException exception) {

        return invalidRequest(
                Stream.concat(
                        exception.getBindingResult().getFieldErrors().stream().map(RestfulExceptionHandler::described),
                        exception.getBindingResult().getGlobalErrors().stream()
                                .map(error -> "'%s' %s".formatted(error.getObjectName(), brokenRule(error)))));

    }

    /**
     * The same as {@link #handleInvalidRequestBody(MethodArgumentNotValidException)}, for the
     * validation Spring runs when a parameter of a controller method carries a constraint itself,
     * like {@code @NotNull} on a path variable.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<String> handleInvalidParameter(
            final HandlerMethodValidationException exception) {

        return invalidRequest(
                exception
                        .getParameterValidationResults()
                        .stream()
                        .flatMap(result -> result instanceof ParameterErrors errors
                                ? errors.getFieldErrors().stream().map(RestfulExceptionHandler::described)
                                : result.getResolvableErrors().stream().map(error -> "'%s' %s".formatted(
                                        result.getMethodParameter().getParameterName() != null
                                                ? result.getMethodParameter().getParameterName()
                                                : "parameter " + result.getMethodParameter().getParameterIndex(),
                                        brokenRule(error)))));

    }

    private ResponseEntity<String> invalidRequest(
            final Stream<String> violations) {

        final var reason = "The request is not valid: %s."
                .formatted(violations.sorted().distinct().collect(Collectors.joining(", ")));
        // no stack trace: the reason says all there is, and the sender gets it too
        logger.warn("Returning HTTP 400 Bad Request: {}", reason);

        return ResponseEntity
                .badRequest()
                .contentType(MediaType.TEXT_PLAIN)
                .body(reason);

    }

    private static String described(
            final FieldError error) {

        return "'%s' %s".formatted(error.getField(), brokenRule(error));

    }

    /**
     * @param error What the validation found
     * @return The rule in a few words. The validator's own message is not used, because it is
     *         written in the language of the server's locale.
     */
    private static String brokenRule(
            final MessageSourceResolvable error) {

        final var codes = error.getCodes();
        // the last code is the plain name of the constraint, like 'NotNull' or 'Size'
        final var constraint = (codes == null) || (codes.length == 0)
                ? null
                : codes[codes.length - 1];
        if ("NotNull".equals(constraint)) {
            return "is missing";
        }
        return constraint == null
                ? "is not valid"
                : "breaks the rule @" + constraint;

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
