package io.vanillabp.cockpit.commons.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BcUnauthorizedExceptionTest {

    @Test
    void isRuntimeException() {
        BcUnauthorizedException exception = new BcUnauthorizedException("test");

        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    void constructor_withMessage_setsMessage() {
        BcUnauthorizedException exception = new BcUnauthorizedException("Not authenticated");

        assertThat(exception.getMessage()).isEqualTo("Not authenticated");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructor_withMessageAndCause_setsBoth() {
        Throwable cause = new SecurityException("Auth failure");
        BcUnauthorizedException exception = new BcUnauthorizedException("Not authenticated", cause);

        assertThat(exception.getMessage()).isEqualTo("Not authenticated");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructor_withAllParameters_setsAllFields() {
        Throwable cause = new SecurityException("Auth failure");
        BcUnauthorizedException exception = new BcUnauthorizedException("Not authenticated", cause, true, true);

        assertThat(exception.getMessage()).isEqualTo("Not authenticated");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void constructor_withSuppression_disabled() {
        Throwable cause = new SecurityException("Auth failure");
        BcUnauthorizedException exception = new BcUnauthorizedException("Not authenticated", cause, false, false);

        assertThat(exception.getMessage()).isEqualTo("Not authenticated");
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getStackTrace()).isEmpty();
    }

    @Test
    void canBeThrown() {
        try {
            throw new BcUnauthorizedException("Please login");
        } catch (BcUnauthorizedException e) {
            assertThat(e.getMessage()).isEqualTo("Please login");
        }
    }
}
