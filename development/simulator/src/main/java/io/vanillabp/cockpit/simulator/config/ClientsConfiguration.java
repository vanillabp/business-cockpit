package io.vanillabp.cockpit.simulator.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;

import io.vanillabp.cockpit.bpms.api.v1_1.ApiClient;
import io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi;
import io.vanillabp.cockpit.bpms.api.v1_1.BpmsApiPath;
import io.vanillabp.cockpit.support.rest.adapter.Client;
import io.vanillabp.cockpit.support.rest.adapter.ClientsConfigurationBase;

/**
 * Builds the REST client which sends user tasks and workflows to the Business Cockpit.
 */
@Configuration
public class ClientsConfiguration extends ClientsConfigurationBase {

  /**
   * Creates the configuration. Spring creates it, nobody else does.
   */
  public ClientsConfiguration() {
  }

  @Autowired
  private ApplicationProperties properties;

  /**
   * The client of the Business Cockpit's BPMS API. It exists only in the profile
   * {@code rest-sync}, where the simulator reports by REST.
   * <p>
   * {@code application.business-cockpit.base-url} is the address of the cockpit only, like
   * {@code vanillabp.cockpit.rest.base-url} of a workflow module. The path of the API comes with
   * the client, from the server URL of the OpenAPI file it was generated from.
   *
   * @return The client
   */
  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Profile("rest-sync")
  public BpmsApi businessCockpitClient() {

    Client businessCockpit = properties.getBusinessCockpit();

    final var client = new ApiClient();
    client.setBasePath(addressOf(businessCockpit.getBaseUrl()) + BpmsApiPath.PATH);

    super.configureFeignBuilder(
        client.getClass(),
        client.getFeignBuilder(),
        businessCockpit);

    return client.buildClient(BpmsApi.class);

  }

  private static String addressOf(
      final String baseUrl) {

    var address = baseUrl;
    while (address.endsWith("/")) {
      address = address.substring(0, address.length() - 1);
    }
    // an address with the path would send every report to /bpms/api/v1_1/bpms/api/v1_1/..., and
    // the cockpit would answer each one with 404
    if (address.endsWith(BpmsApiPath.PATH)) {
      throw new IllegalStateException("""
          The address '%s' in 'application.business-cockpit.base-url' ends with '%s', the path \
          of the BPMS API which the simulator adds itself. Set it to the address of the cockpit \
          without it: '%s'.""".formatted(
          baseUrl, BpmsApiPath.PATH, address.substring(0, address.length() - BpmsApiPath.PATH.length())));
    }
    return address;

  }

}
