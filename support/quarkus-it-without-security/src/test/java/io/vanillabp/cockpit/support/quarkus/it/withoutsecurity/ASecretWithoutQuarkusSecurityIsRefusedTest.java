package io.vanillabp.cockpit.support.quarkus.it.withoutsecurity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application which sets the cockpit's secret wants the login token checked. Without
 * quarkus-security nothing would check it, so the start ends and says what to add.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ASecretWithoutQuarkusSecurityIsRefusedTest {

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      // The classes of the support are added again. Loaded from the jar, they would sit in another
      // class loader than the beans Quarkus generates for them, and those could not reach their
      // fields.
      .withApplicationRoot(jar -> jar
          .addClasses(AssessRiskForm.class)
          .addPackages(true, "io.vanillabp.cockpit.support"))
      .overrideConfigKey("business-cockpit.jwt.hmacSHA256-base64", "9kt7o2AC1AF/tV55Txl2MV5cnjtVr6azrL7I2ID0BUw=")
      .assertException(failure -> assertThat(failure)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("the Quarkus extension quarkus-security is missing")
          .hasMessageContaining("'io.quarkus:quarkus-security'")
          .hasMessageContaining("'business-cockpit.jwt.enabled=false'"));

  @Test
  public void theApplicationDoesNotStart() {

    // the assertion is the one of the extension above, it runs when the start fails
  }

}
