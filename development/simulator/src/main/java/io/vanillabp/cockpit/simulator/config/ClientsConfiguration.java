package io.vanillabp.cockpit.simulator.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;

import io.vanillabp.cockpit.bpms.api.v1_1.ApiClient;
import io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi;
import io.vanillabp.cockpit.commons.rest.adapter.Client;
import io.vanillabp.cockpit.commons.rest.adapter.ClientsConfigurationBase;

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
   *
   * @return The client
   */
  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  @Profile("rest-sync")
  public BpmsApi businessCockpitClient() {

    Client businessCockpit = properties.getBusinessCockpit();

    final var client = new ApiClient();
    client.setBasePath(businessCockpit.getBaseUrl());

    super.configureFeignBuilder(
        client.getClass(),
        client.getFeignBuilder(),
        businessCockpit);

    return client.buildClient(BpmsApi.class);

  }

}
