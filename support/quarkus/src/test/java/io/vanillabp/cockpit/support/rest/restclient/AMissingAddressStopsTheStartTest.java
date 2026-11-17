package io.vanillabp.cockpit.support.rest.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.Startup;
import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.rest.restclient.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * A workflow module produces its client while the application starts, the way the README shows.
 * A missing address then stops the start, with the message which names the key, and not the first
 * call.
 */
@ExtendWith(SuppressOutputExtension.class)
public class AMissingAddressStopsTheStartTest {

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(jar -> TheRestClientsOnQuarkus
          .withTheRestClients(jar)
          .addClasses(Clients.class))
      .overrideConfigKey("loan-approval.inventory.read-timeout", "2s")
      .assertException(failure -> assertThat(failure)
          .hasStackTraceContaining("The REST clients at 'loan-approval.inventory' cannot be built")
          .hasStackTraceContaining("Set 'loan-approval.inventory.base-url'"));

  /** What a workflow module writes. */
  @ApplicationScoped
  public static class Clients {

    @Produces
    @Singleton
    @Startup
    InventoryApi inventory(
        final RestClients restClients) {
      return restClients.build(InventoryApi.class, "loan-approval.inventory");
    }

  }

  @Test
  public void theStartEnds() {

    // the assertion is the one of the extension above, it runs when the start fails
  }

}
