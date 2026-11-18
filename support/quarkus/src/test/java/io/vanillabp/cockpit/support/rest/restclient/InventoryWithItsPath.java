package io.vanillabp.cockpit.support.rest.restclient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 * A client interface of an API whose path is declared by {@link InventoryApiPath}, as a client
 * module of a cockpit API declares the server path of its OpenAPI file.
 */
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
