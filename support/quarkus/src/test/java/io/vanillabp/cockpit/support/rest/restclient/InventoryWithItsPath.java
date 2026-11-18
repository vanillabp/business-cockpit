package io.vanillabp.cockpit.support.rest.restclient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 * A client interface which adds the path of its API to each call itself, as a generated client of
 * a cockpit API does with the server path of its OpenAPI file.
 */
@Path("/inventory/v2")
public interface InventoryWithItsPath {

  /**
   * Reads one item.
   *
   * @param id the id of the item
   * @return the item as JSON
   */
  @GET
  @Path("/items/{id}")
  @Produces("application/json")
  String item(
      @PathParam("id") String id);

}
