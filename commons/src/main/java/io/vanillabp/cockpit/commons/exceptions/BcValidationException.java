package io.vanillabp.cockpit.commons.exceptions;

import java.util.Map;

/**
 * The values a request sent are not valid. {@link RestfulExceptionHandler} answers it with
 * {@code 400 Bad Request} and sends {@link #getViolations()} as a JSON body.
 */
public class BcValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception and says how its stack trace and suppressed exceptions are handled.
     *
     * @param message            What went wrong
     * @param cause              The exception which led to this one
     * @param enableSuppression  Whether suppressed exceptions are recorded
     * @param writableStackTrace Whether the stack trace is recorded
     */
    public BcValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Creates the exception with the exception which led to it.
     *
     * @param message What went wrong
     * @param cause   The exception which led to this one
     */
    public BcValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception.
     *
     * @param message What went wrong
     */
    public BcValidationException(String message) {
        super(message);
    }
    
    /**
     * What is wrong with the request, one entry per field. This class has none, so the body of the
     * answer is an empty JSON object; the message only goes to the log.
     *
     * @return The violations, the field name as key and the problem as value
     */
    public Map<String, String> getViolations() {
        return Map.of();
    }

}
