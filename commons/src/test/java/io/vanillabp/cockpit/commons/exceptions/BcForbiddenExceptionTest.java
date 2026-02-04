package io.vanillabp.cockpit.commons.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BcForbiddenExceptionTest {

    @Test
    void isRuntimeException() {
        BcForbiddenException exception = new BcForbiddenException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void constructor_withMessage_setsMessage() {
        BcForbiddenException exception = new BcForbiddenException("Access denied");

        assertThat(exception.getMessage()).isEqualTo("Access denied");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_withMessageAndCause_setsBoth() {
        Throwable cause = new IllegalStateException("Original error");
        BcForbiddenException exception = new BcForbiddenException("Access denied", cause);

        assertThat(exception.getMessage()).isEqualTo("Access denied");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructor_withAllParameters_setsAllFields() {
        Throwable cause = new IllegalStateException("Original error");
        BcForbiddenException exception = new BcForbiddenException("Access denied", cause, true, true);

        assertThat(exception.getMessage()).isEqualTo("Access denied");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructor_withSuppression_disabled() {
        Throwable cause = new IllegalStateException("Original error");
        BcForbiddenException exception = new BcForbiddenException("Access denied", cause, false, false);

        assertThat(exception.getMessage()).isEqualTo("Access denied");
        assertThat(exception.getCause()).isSameAs(cause);
        // With writableStackTrace=false, getStackTrace() returns empty array
        assertThat(exception.getStackTrace()).isEmpty();
    }

    @Test
    void canBeThrown() {
        try {
            throw new BcForbiddenException("No permission");
        } catch (BcForbiddenException e) {
            assertThat(e.getMessage()).isEqualTo("No permission");
        }
    }
}
