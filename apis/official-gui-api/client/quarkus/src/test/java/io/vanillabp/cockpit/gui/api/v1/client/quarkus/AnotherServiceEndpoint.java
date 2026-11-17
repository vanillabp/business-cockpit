package io.vanillabp.cockpit.gui.api.v1.client.quarkus;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** An endpoint which calls another service while it answers the user's request. */
@Path("/another-service")
public class AnotherServiceEndpoint {

  @Inject
  AnotherService anotherService;

  /**
   * Asks the other service.
   *
   * @return its answer
   */
  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String hello() {
    return anotherService.hello();
  }

}
