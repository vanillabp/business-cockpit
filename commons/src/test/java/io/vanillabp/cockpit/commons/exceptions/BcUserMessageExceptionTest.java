package io.vanillabp.cockpit.commons.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BcUserMessageExceptionTest {

    @Test
    void isRuntimeException() {
        BcUserMessageException exception = new BcUserMessageException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void constructor_withMessage_setsMessage() {
        BcUserMessageException exception = new BcUserMessageException("User-friendly message");

        assertThat(exception.getMessage()).isEqualTo("User-friendly message");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_withMessageAndCause_setsBoth() {
        Throwable cause = new IllegalArgumentException("Invalid input");
        BcUserMessageException exception = new BcUserMessageException("Please check your input", cause);

        assertThat(exception.getMessage()).isEqualTo("Please check your input");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructor_withAllParameters_setsAllFields() {
        Throwable cause = new IllegalArgumentException("Invalid input");
        BcUserMessageException exception = new BcUserMessageException("Please check your input", cause, true, true);

        assertThat(exception.getMessage()).isEqualTo("Please check your input");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructor_withSuppression_disabled() {
        Throwable cause = new IllegalArgumentException("Invalid input");
        BcUserMessageException exception = new BcUserMessageException("Please check your input", cause, false, false);

        assertThat(exception.getMessage()).isEqualTo("Please check your input");
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getStackTrace()).isEmpty();
    }

    @Test
    void canBeThrown() {
        try {
            throw new BcUserMessageException("Something went wrong. Please try again.");
        } catch (BcUserMessageException e) {
            assertThat(e.getMessage()).isEqualTo("Something went wrong. Please try again.");
        }
    }
}
