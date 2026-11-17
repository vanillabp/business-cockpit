package io.vanillabp.cockpit.support.quarkus.it;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The client of a service the workflow module of the tests calls. A real module generates it from
 * the service's OpenAPI file. One method is enough to show that the client is built from keys.
 */
@Path("/items")
public interface InventoryClient {

  /**
   * The name of an item.
   *
   * @param itemId the id of the item
   * @return the name
   */
  @GET
  @Path("/{itemId}")
  @Produces(MediaType.TEXT_PLAIN)
  String nameOf(
      @PathParam("itemId") String itemId);

}
