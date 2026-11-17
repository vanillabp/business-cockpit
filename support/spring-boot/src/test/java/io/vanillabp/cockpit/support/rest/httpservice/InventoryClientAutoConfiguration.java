package io.vanillabp.cockpit.support.rest.httpservice;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.autoconfigure.service.HttpServiceClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;

/**
 * What the client module of the inventory service brings, as the support README shows it. An
 * application adds the dependency, sets the keys below {@code inventory.client} and injects
 * {@link InventoryClient}. It steps back if the application imports the API into groups of its
 * own. The tests import it where they need it.
 */
@AutoConfiguration(before = HttpServiceClientAutoConfiguration.class)
@ConditionalOnMissingBean(InventoryApi.class)
@ImportHttpServices(group = InventoryClientAutoConfiguration.GROUP, types = InventoryClient.class)
public class InventoryClientAutoConfiguration {

  public static final String GROUP = "inventory";

  @Bean
  HttpServiceGroupPrefix inventoryClientKeys() {
    return HttpServiceGroupPrefix.of(GROUP, "inventory.client");
  }

}
