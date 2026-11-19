package io.vanillabp.cockpit.support.userinterface;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An address with a path, and an address for a workflow module the application does not have. The
 * start ends and names both keys, with the words of the Spring Boot module.
 */
@ExtendWith(SuppressOutputExtension.class)
public class AWrongRouteKeyEndsTheStartTest {

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> jar
          .addClasses(UserInterfaceRoutesOnQuarkus.class, VertxProxyRoute.class)
          .addAsResource(new StringAsset("loan-approval"), UserInterfaceRoutes.WORKFLOW_MODULE_FILE))
      .overrideConfigKey(UserInterfaceRoute.keyOf("loan-approval"), "http://nginx:8081/loan-approval")
      .overrideConfigKey("loan-aproval.user-interface.address", "http://nginx:8081")
      .assertException(failure -> assertThat(failure)
          // the application has a class loader of its own, so the class is compared by its name
          .satisfies(thrown -> assertThat(thrown.getClass().getName())
              .isEqualTo(UserInterfaceRoutesMisconfigured.class.getName()))
          .hasMessageContaining("'loan-approval.user-interface.address' is 'http://nginx:8081/loan-approval', but "
              + "only its scheme, host and port count.")
          .hasMessageContaining("'loan-aproval.user-interface.address' names no workflow module of this "
              + "application, so there is no route for it. The workflow modules of the application are: "
              + "loan-approval."));

  @Test
  public void theStartEnds() {

    // the assertion is the one of the extension above, it runs when the start fails
  }

}
