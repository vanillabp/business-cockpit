package io.vanillabp.cockpit.support.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Each constructor keeps what it was given, because the message is what the caller of the
 * request gets to read.
 */
@ExtendWith(SuppressOutputExtension.class)
public class BcUnauthorizedExceptionTest {

  @Test
  public void theMessageAloneIsKept() {

    assertThat(new BcUnauthorizedException("no login"))
        .hasMessage("no login")
        .hasNoCause();

  }

  @Test
  public void theCauseIsKeptWithTheMessage() {

    final var cause = new IllegalStateException("expired");

    assertThat(new BcUnauthorizedException("no login", cause))
        .hasMessage("no login")
        .hasCause(cause);

  }

  @Test
  public void noStackTraceIsRecordedWhenTheCallerSaysSo() {

    final var exception = new BcUnauthorizedException("no login", null, false, false);

    assertThat(exception).hasMessage("no login");
    assertThat(exception.getStackTrace()).isEmpty();

  }

}
