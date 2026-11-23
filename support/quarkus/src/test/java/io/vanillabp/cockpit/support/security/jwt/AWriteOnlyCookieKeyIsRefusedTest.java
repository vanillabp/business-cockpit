package io.vanillabp.cockpit.support.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.security.usercontext.DirectoryOfTheTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A developer copies the cockpit's settings of the cookie into the workflow module. The keys which
 * say how the cookie is written would do nothing there, because only the cockpit writes the cookie.
 * So the start ends and says so.
 */
@ExtendWith(SuppressOutputExtension.class)
public class AWriteOnlyCookieKeyIsRefusedTest {

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> TheSupportOnQuarkus
          .withTheSupport(jar)
          .addClasses(DirectoryOfTheTest.class))
      .overrideConfigKey(JwtSettings.SECRET_KEY, TheSupportOnQuarkus.SECRET_BASE64)
      .overrideConfigKey(JwtSettings.COOKIE_NAME_KEY, "bc")
      .overrideConfigKey("business-cockpit.jwt.cookie.secure", "true")
      .overrideConfigKey("business-cockpit.jwt.cookie.same-site", "Strict")
      .overrideConfigKey("business-cockpit.jwt.cookie.domain", "example.com")
      .overrideConfigKey("business-cockpit.jwt.cookie.path", "/")
      .overrideConfigKey("business-cockpit.jwt.cookie.expires-duration", "PT1H")
      .overrideConfigKey("business-cockpit.jwt.cookie.max-login-duration", "P1D")
      .overrideConfigKey("business-cockpit.jwt.cookie.unknown-user-renewal", "refuse")
      .assertException(failure -> assertThat(failure)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("'business-cockpit.jwt.cookie.secure' says how the cookie is written, "
              + "and only the cockpit writes the cookie.")
          .hasMessageContaining("'business-cockpit.jwt.cookie.same-site' says how")
          .hasMessageContaining("'business-cockpit.jwt.cookie.domain' says how")
          .hasMessageContaining("'business-cockpit.jwt.cookie.path' says how")
          .hasMessageContaining("'business-cockpit.jwt.cookie.expires-duration' says how")
          .hasMessageContaining("'business-cockpit.jwt.cookie.max-login-duration' says how")
          .hasMessageContaining("'business-cockpit.jwt.cookie.unknown-user-renewal' says how")
          .hasMessageContaining("keep it in the settings of the cockpit")
          .hasMessageNotContaining("'business-cockpit.jwt.cookie.name'")
          .hasMessageNotContaining("'business-cockpit.jwt.hmacSHA256-base64'"));

  @Test
  public void theStartEnds() {

    // the assertion is the one of the extension above, it runs when the start fails
  }

}
