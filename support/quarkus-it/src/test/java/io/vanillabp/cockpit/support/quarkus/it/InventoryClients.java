package io.vanillabp.cockpit.support.quarkus.it;

import io.quarkus.runtime.Startup;
import io.vanillabp.cockpit.support.rest.restclient.RestClients;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * What the workflow module of the tests writes to call a service: one producer, which builds the
 * client from the keys at the module's own prefix while the application starts.
 */
@ApplicationScoped
public class InventoryClients {

  /**
   * The client of the inventory.
   *
   * @param restClients the builder of the support module
   * @return the client
   */
  @Produces
  @Singleton
  @Startup
  InventoryClient inventory(
      final RestClients restClients) {
    return restClients.build(InventoryClient.class, "loan-approval.inventory");
  }

}
