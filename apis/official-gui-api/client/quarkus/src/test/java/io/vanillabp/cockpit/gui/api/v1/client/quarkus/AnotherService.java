package io.vanillabp.cockpit.gui.api.v1.client.quarkus;

import io.quarkus.runtime.Startup;
import io.vanillabp.cockpit.support.rest.restclient.RestClients;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * A service which is not the cockpit. Its client must never get the user's cookie.
 */
@Path("/hello")
public interface AnotherService {

  /**
   * Says hello.
   *
   * @return the greeting
   */
  @GET
  @jakarta.ws.rs.Produces("text/plain")
  String hello();

  /** Builds the client of the service. */
  @ApplicationScoped
  class Client {

    @Produces
    @Singleton
    @Startup
    AnotherService anotherService(
        final RestClients restClients) {
      return restClients.build(AnotherService.class, "another-service");
    }

  }

}
