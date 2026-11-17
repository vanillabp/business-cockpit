package io.vanillabp.cockpit.support.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.security.usercontext.DirectoryOfTheTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Without the secret, no token can be checked, and every user would get {@code 401}. So the start
 * ends, with the words the Spring Boot module uses, a secret to copy and the way out for an
 * application which does not read the token.
 */
@ExtendWith(SuppressOutputExtension.class)
public class AMissingKeyIsExplainedTest {

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> TheSupportOnQuarkus
          .withTheSupport(jar)
          .addClasses(DirectoryOfTheTest.class))
      .assertException(failure -> assertThat(failure)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageStartingWith("No property 'business-cockpit.jwt.hmacSHA256-base64' set! "
              + "One could use this generated key: '")
          .hasMessageContaining("Set it to the secret of the cockpit")
          .hasMessageContaining("'business-cockpit.jwt.enabled=false'"));

  @Test
  public void theStartEnds() {

    // the assertion is the one of the extension above, it runs when the start fails
  }

}
