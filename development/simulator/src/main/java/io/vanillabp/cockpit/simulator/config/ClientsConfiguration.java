package io.vanillabp.cockpit.simulator.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.simulator.bpms.BpmsApi;
import io.vanillabp.cockpit.simulator.bpms.BpmsApiPath;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceApiPath;
import io.vanillabp.cockpit.support.rest.httpservice.HttpServiceGroupPrefix;

/**
 * Builds the REST client which sends user tasks and workflows to the Business Cockpit. It exists
 * only in the profile {@code rest-sync}, where the simulator reports by REST.
 * <p>
 * The client is an HTTP service client of {@code support}, so its keys are the ones of an HTTP
 * service group below {@code business-cockpit}, see the README of {@code support}.
 * {@code business-cockpit.base-url} is the address of the cockpit only, like
 * {@code vanillabp.cockpit.rest.base-url} of a workflow module. The path of the API comes with
 * the client, from the server URL of the OpenAPI file it was generated from, and an address which
 * ends with it stops the start.
 */
@Configuration
@Profile("rest-sync")
@ImportHttpServices(group = ClientsConfiguration.GROUP, types = BpmsApi.class)
public class ClientsConfiguration {

  /** The HTTP service group of the client, and the prefix of its keys. */
  static final String GROUP = "business-cockpit";

  /**
   * Creates the configuration. Spring creates it, nobody else does.
   */
  public ClientsConfiguration() {
  }

  /**
   * Where the keys of the client live.
   *
   * @return The prefix of the group
   */
  @Bean
  HttpServiceGroupPrefix businessCockpitKeys() {
    return HttpServiceGroupPrefix.of(GROUP, GROUP);
  }

  /**
   * The path of the BPMS API, which the group adds to {@code base-url}.
   *
   * @return The path and the interface it belongs to
   */
  @Bean
  HttpServiceApiPath bpmsApiPath() {
    return new HttpServiceApiPath(BpmsApiPath.PATH, List.of(BpmsApi.class));
  }

}
