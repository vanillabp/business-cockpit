package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A workflow module, or a client module, which calls a service only if the application sets its
 * address. The module imports the group only then, so the conventions never see a group without
 * an address. A group which is there still needs a valid address, as every other group.
 */
@ExtendWith(SuppressOutputExtension.class)
public class AnOptionalGroupTest {

  interface OrdersInventory extends InventoryApi {
  }

  /** The group of the module, there only if its address is set. */
  @Configuration
  @ConditionalOnHttpServiceGroupAddress(prefix = "orders.inventory")
  @ImportHttpServices(group = "orders-inventory", types = OrdersInventory.class)
  static class OrdersModule {

    @Bean
    HttpServiceGroupPrefix ordersInventoryKeys() {
      return HttpServiceGroupPrefix.of("orders-inventory", "orders.inventory");
    }

  }

  @Configuration
  @ConditionalOnHttpServiceGroupAddress(prefix = "Orders Inventory")
  static class MisspelledPrefix {
  }

  /** A group of another module, which is not optional. */
  @Configuration
  @ImportHttpServices(group = "billing-inventory", types = InventoryApi.class)
  static class BillingModule {
  }

  @Test
  public void without_its_address_the_group_is_not_there_and_the_application_starts() {

    try (var context = start(new Class<?>[]{
        TestApplication.class, OrdersModule.class
    })) {

      assertThat(context.getBeansOfType(OrdersInventory.class)).isEmpty();
      assertThat(context.getBeansOfType(HttpServiceGroupPrefix.class)).isEmpty();

    }

  }

  @Test
  public void without_its_address_the_group_does_not_bother_the_other_groups() {

    try (var context = start(new Class<?>[]{
        TestApplication.class, OrdersModule.class, BillingModule.class
    }, "--spring.http.serviceclient.billing-inventory.base-url=http://localhost:1/api")) {

      assertThat(context.getBeansOfType(OrdersInventory.class)).isEmpty();
      assertThat(context.getBeansOfType(InventoryApi.class)).hasSize(1);

    }

  }

  @Test
  public void a_client_module_without_its_address_imports_nothing() {

    try (var context = start(new Class<?>[]{
        TestApplication.class, InventoryClientAutoConfiguration.class
    })) {

      assertThat(context.getBeansOfType(InventoryClient.class)).isEmpty();

    }

  }

  @Test
  public void with_its_address_the_group_is_there() {

    try (var context = start(new Class<?>[]{
        TestApplication.class, OrdersModule.class
    }, "--orders.inventory.base-url=http://localhost:1/api")) {

      assertThat(context.getBeansOfType(OrdersInventory.class)).hasSize(1);

    }

  }

  @Test
  public void with_a_broken_address_the_start_still_stops_and_names_the_key() {

    final var problems = Starts.problemsOf(new Class<?>[]{
        TestApplication.class, OrdersModule.class
    }, "orders.inventory.base-url=inventory.example.com/api");

    assertThat(problems).singleElement().asString()
        .contains("'orders.inventory.base-url' is no complete http or https address");

  }

  @Test
  public void with_an_address_nothing_resolves_the_start_still_names_the_placeholder() {

    final var problems = Starts.problemsOf(new Class<?>[]{
        TestApplication.class, OrdersModule.class
    }, "orders.inventory.base-url=${INVENTORY_URL}/api");

    assertThat(problems).singleElement().asString()
        .contains("holds a placeholder nothing resolves")
        .contains("'INVENTORY_URL'");

  }

  @Test
  public void with_an_empty_address_the_start_stops_and_names_the_key() {

    final var problems = Starts.problemsOf(new Class<?>[]{
        TestApplication.class, OrdersModule.class
    }, "orders.inventory.base-url=");

    assertThat(problems).singleElement().asString()
        .contains("'orders-inventory' has no address")
        .contains("Set 'orders.inventory.base-url'");

  }

  @Test
  public void a_prefix_which_is_no_key_is_refused() {

    assertThatThrownBy(() -> start(new Class<?>[]{
        TestApplication.class, MisspelledPrefix.class
    }))
        .rootCause()
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("'Orders Inventory'");

  }

  private static ConfigurableApplicationContext start(
      final Class<?>[] configurations,
      final String... arguments) {

    return new SpringApplicationBuilder(configurations)
        .web(WebApplicationType.NONE)
        .run(arguments);

  }

}
