package io.vanillabp.cockpit.support.security.usercontext;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.security.jwt.JwtSettings;
import io.vanillabp.cockpit.support.security.jwt.TheSupportOnQuarkus;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The login token names the user but carries no groups, so a workflow module needs a directory of
 * its own. Without one the start ends with the words the Spring Boot module uses, and with the code
 * a Quarkus developer writes. It is the twin of the Spring Boot test of the same name.
 */
@ExtendWith(SuppressOutputExtension.class)
public class AMissingDirectoryIsExplainedTest {

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(TheSupportOnQuarkus::withTheSupport)
      .overrideConfigKey(JwtSettings.SECRET_KEY, TheSupportOnQuarkus.SECRET_BASE64)
      .assertException(failure -> assertThat(failure)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining(UserDirectoryService.class.getName())
          .hasMessageContaining("business-cockpit-quarkus-support needs one.")
          .hasMessageContaining("public class UserDirectory implements UserDirectoryService")
          .hasMessageContaining("    @ApplicationScoped")
          .hasMessageContaining("cache the users")
          .hasMessageContaining("a directory of its own")
          .hasMessageContaining(UserDirectoryIsMissing.WIKI_SECURITY));

  @Test
  public void theStartEnds() {

    // the assertion is the one of the extension above, it runs when the start fails
  }

}
