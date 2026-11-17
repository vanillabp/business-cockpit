package io.vanillabp.cockpit.support.rest.httpservice;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;

/**
 * The client of the inventory service, as its client module brings it. It gives the module a bean
 * type of its own next to the generated interface.
 */
public interface InventoryClient extends InventoryApi {
}
