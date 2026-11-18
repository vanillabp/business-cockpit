package io.vanillabp.cockpit.support.rest.restclient;

import java.util.List;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/** Declares the path of the API of {@link InventoryWithItsPath}, as a client module does. */
@Singleton
public class InventoryApiPath {

  @Produces
  @Singleton
  @Unremovable
  RestClientApiPath inventoryApiPath() {
    return new RestClientApiPath("/inventory/v2/", List.of(InventoryWithItsPath.class));
  }

}
