package io.vanillabp.cockpit.util.exceptions;

public class BcUserMessageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BcUserMessageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BcUserMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public BcUserMessageException(String message) {
        super(message);
    }

}
