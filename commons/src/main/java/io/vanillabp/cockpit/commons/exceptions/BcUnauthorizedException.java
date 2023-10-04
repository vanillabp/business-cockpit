package io.vanillabp.cockpit.commons.exceptions;

public class BcUnauthorizedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BcUnauthorizedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BcUnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public BcUnauthorizedException(String message) {
        super(message);
    }

}
