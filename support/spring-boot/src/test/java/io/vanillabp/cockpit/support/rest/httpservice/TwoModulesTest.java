package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;

/**
 * Two workflow modules in one application call the same external service through the same
 * generated interface, but with different keys: one module waits 300 milliseconds for an answer,
 * the other three seconds. Each module imports the interface into a group of its own and gives the
 * group a prefix of its own. The client module of the API steps back, because the application has
 * clients of that API already.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = {
        TestApplication.class, TwoModulesTest.OrdersModule.class, TwoModulesTest.BillingModule.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "orders.inventory.read-timeout=300ms", "billing.inventory.read-timeout=3s",
    })
@ImportAutoConfiguration(InventoryClientAutoConfiguration.class)
@DirtiesContext
public class TwoModulesTest {

  static final MockWebServer server = new MockWebServer();

  @DynamicPropertySource
  static void urls(
      final DynamicPropertyRegistry registry) {

    registry.add("orders.inventory.base-url", () -> Stubs.apiUrl(server));
    registry.add("billing.inventory.base-url", () -> Stubs.apiUrl(server));

  }

  @AfterAll
  static void stop() {
    server.close();
  }

  /** In the orders module. */
  interface OrdersInventory extends InventoryApi {
  }

  /** In the billing module. */
  interface BillingInventory extends InventoryApi {
  }

  @Configuration
  @ImportHttpServices(group = "orders-inventory", types = OrdersInventory.class)
  static class OrdersModule {

    @Bean
    HttpServiceGroupPrefix ordersInventoryKeys() {
      return HttpServiceGroupPrefix.of("orders-inventory", "orders.inventory");
    }

  }

  @Configuration
  @ImportHttpServices(group = "billing-inventory", types = BillingInventory.class)
  static class BillingModule {

    @Bean
    HttpServiceGroupPrefix billingInventoryKeys() {
      return HttpServiceGroupPrefix.of("billing-inventory", "billing.inventory");
    }

  }

  @Autowired
  OrdersInventory orders;

  @Autowired
  BillingInventory billing;

  @Autowired
  ApplicationContext context;

  static MockResponse oneSecondLate() {
    return new MockResponse.Builder()
        .code(200)
        .addHeader("Content-Type", "application/json")
        .body("""
            {"id": "1", "name": "Screw", "stock": 7}""")
        .headersDelay(1, TimeUnit.SECONDS)
        .build();
  }

  @Test
  public void each_module_waits_as_long_as_its_own_keys_say() {

    server.enqueue(oneSecondLate());
    assertThatThrownBy(() -> orders.getItem("1")).isInstanceOf(ResourceAccessException.class);

    server.enqueue(oneSecondLate());
    assertThat(billing.getItem("1").getBody().getId()).isEqualTo("1");

  }

  @Test
  public void the_client_module_steps_back() {

    assertThat(context.getBeansOfType(InventoryClient.class)).isEmpty();
    assertThat(context.getBeansOfType(InventoryApi.class)).hasSize(2);

  }

}
