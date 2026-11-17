package io.vanillabp.cockpit.support.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.security.usercontext.DirectoryOfTheTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A misspelled key stands in the file, and nobody reads it. The start ends and names the key that
 * was probably meant, also when the key holds for one profile only.
 */
@ExtendWith(SuppressOutputExtension.class)
public class AMisspelledJwtKeyIsRefusedTest {

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> TheSupportOnQuarkus
          .withTheSupport(jar)
          .addClasses(DirectoryOfTheTest.class))
      .overrideConfigKey(JwtSettings.SECRET_KEY, TheSupportOnQuarkus.SECRET_BASE64)
      .overrideConfigKey("business-cockpit.jwt.cookie.nmae", "cockpit")
      .overrideConfigKey("%test.business-cockpit.jwt.hmacsha256-base64", TheSupportOnQuarkus.SECRET_BASE64)
      .overrideConfigKey("business-cockpit.something-else", "x")
      .assertException(failure -> assertThat(failure)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("a workflow module does not read")
          .hasMessageContaining(
              "'business-cockpit.jwt.cookie.nmae' is no setting of the Business Cockpit. "
                  + "Did you mean 'business-cockpit.jwt.cookie.name'?")
          .hasMessageContaining(
              "business-cockpit.jwt.hmacsha256-base64' is no setting of the Business Cockpit. "
                  + "Did you mean 'business-cockpit.jwt.hmacSHA256-base64'?")
          .hasMessageContaining("'business-cockpit.something-else' is no setting of the Business Cockpit.")
          .hasMessageContaining("business-cockpit.jwt.hmacSHA256-base64, business-cockpit.jwt.cookie.name"));

  @Test
  public void theStartEnds() {

    // the assertion is the one of the extension above, it runs when the start fails
  }

}
