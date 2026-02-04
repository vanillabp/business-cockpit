package io.vanillabp.cockpit.commons.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BcValidationExceptionTest {

    @Test
    void isRuntimeException() {
        BcValidationException exception = new BcValidationException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void constructor_withMessage_setsMessage() {
        BcValidationException exception = new BcValidationException("Validation failed");

        assertThat(exception.getMessage()).isEqualTo("Validation failed");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_withMessageAndCause_setsBoth() {
        Throwable cause = new IllegalArgumentException("Invalid value");
        BcValidationException exception = new BcValidationException("Validation failed", cause);

        assertThat(exception.getMessage()).isEqualTo("Validation failed");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructor_withAllParameters_setsAllFields() {
        Throwable cause = new IllegalArgumentException("Invalid value");
        BcValidationException exception = new BcValidationException("Validation failed", cause, true, true);

        assertThat(exception.getMessage()).isEqualTo("Validation failed");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructor_withSuppression_disabled() {
        Throwable cause = new IllegalArgumentException("Invalid value");
        BcValidationException exception = new BcValidationException("Validation failed", cause, false, false);

        assertThat(exception.getMessage()).isEqualTo("Validation failed");
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getStackTrace()).isEmpty();
    }

    @Test
    void getViolations_returnsEmptyMap() {
        BcValidationException exception = new BcValidationException("Validation failed");

        assertThat(exception.getViolations()).isEmpty();
    }

    @Test
    void canBeThrown() {
        try {
            throw new BcValidationException("Field 'name' is required");
        } catch (BcValidationException e) {
            assertThat(e.getMessage()).isEqualTo("Field 'name' is required");
            assertThat(e.getViolations()).isEmpty();
        }
    }
}
