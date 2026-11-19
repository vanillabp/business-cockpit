package io.vanillabp.cockpit.support.quarkus.it;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * The API of the workflow module, below {@code /loan-approval/api/}, which stays in the application
 * when its user interface comes from an nginx.
 */
@Path("/loan-approval/api")
public class ModuleApi {

  @GET
  @Path("/ping")
  public String ping() {

    return "pong";

  }

}
