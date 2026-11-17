package io.vanillabp.cockpit.support.rest.restclient;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import io.vanillabp.cockpit.support.rest.restclient.inventory.InventoryApi;
import io.vanillabp.cockpit.support.rest.restclient.inventory.model.Item;

/**
 * What the REST client tests of this module put into the application they start. The test
 * framework builds that application only out of the classes a test names, so the main classes of
 * the package are named here once, together with the client generated from the inventory API.
 */
public final class TheRestClientsOnQuarkus {

  private TheRestClientsOnQuarkus() {
  }

  /**
   * Adds the REST client classes of this module and the generated inventory client.
   *
   * @param application The application of a test
   * @return The application
   */
  public static JavaArchive withTheRestClients(
      final JavaArchive application) {

    return application
        .addClasses(
            RestClients.class,
            RestClientKeys.class,
            RestClientKeys.Proxy.class,
            RestClientKeys.Retry.class,
            RestClientKeys.OAuth.class,
            RestClientsMisconfigured.class,
            UnreadKeys.class,
            ClientCredentials.class,
            ClientCredentials.Token.class,
            TokenEndpoint.class,
            TokenRequestFailed.class,
            Retries.class,
            ExchangeLog.class,
            DefaultHeaders.class,
            Stubs.class,
            TheRestClientsOnQuarkus.class)
        .addPackage(InventoryApi.class.getPackage())
        .addPackage(Item.class.getPackage());

  }

  /**
   * A builder of clients of its own, so a test starts without the settings and the OAuth token a
   * test before it left behind.
   *
   * @return the builder, reading the configuration of the running application
   */
  public static RestClients fresh() {

    final var restClients = new RestClients();
    restClients.config = ConfigProvider.getConfig();
    return restClients;

  }

}
