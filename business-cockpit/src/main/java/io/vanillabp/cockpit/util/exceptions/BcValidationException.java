package io.vanillabp.cockpit.util.exceptions;

import java.util.Map;

public class BcValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BcValidationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BcValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BcValidationException(String message) {
        super(message);
    }
    
    public Map<String, String> getViolations() {
        return Map.of();
    }

}
