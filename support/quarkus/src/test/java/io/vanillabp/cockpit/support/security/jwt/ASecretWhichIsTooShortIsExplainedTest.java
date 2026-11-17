package io.vanillabp.cockpit.support.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.security.usercontext.DirectoryOfTheTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A secret of the wrong length could never check a token of the cockpit, so it ends the start as
 * well, and the message names the key.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ASecretWhichIsTooShortIsExplainedTest {

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> TheSupportOnQuarkus
          .withTheSupport(jar)
          .addClasses(DirectoryOfTheTest.class))
      .overrideConfigKey(JwtSettings.SECRET_KEY, "c2hvcnQ=")
      .assertException(failure -> assertThat(failure)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageStartingWith("'business-cockpit.jwt.hmacSHA256-base64': ")
          .hasMessageContaining("40 bits long")
          .hasMessageContaining("at least 256 bits"));

  @Test
  public void theStartEnds() {

    // the assertion is the one of the extension above, it runs when the start fails
  }

}
