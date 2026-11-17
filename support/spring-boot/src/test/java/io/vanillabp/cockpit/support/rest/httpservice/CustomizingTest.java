package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockWebServer;

/**
 * What the keys do not cover, the application changes with Spring's own beans. A group configurer
 * and a {@code RestClientCustomizer} run after the conventions, so they win over the keys.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = {
        TestApplication.class, CustomizingTest.Clients.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "spring.http.serviceclient.inventory.default-header.X-Caller=from-the-keys")
@DirtiesContext
public class CustomizingTest {

  static final MockWebServer server = new MockWebServer();

  @DynamicPropertySource
  static void urls(
      final DynamicPropertyRegistry registry) {
    registry.add("spring.http.serviceclient.inventory.base-url", () -> Stubs.apiUrl(server));
  }

  @AfterAll
  static void stop() {
    server.close();
  }

  interface Inventory extends InventoryApi {
  }

  @Configuration
  @ImportHttpServices(group = "inventory", types = Inventory.class)
  static class Clients {

    @Bean
    RestClientHttpServiceGroupConfigurer inventoryCaller() {
      return groups -> groups
          .filterByName("inventory")
          .forEachClient((
              group,
              builder) -> builder.defaultHeader("X-Caller", "from-the-configurer"));
    }

    @Bean
    RestClientCustomizer everyClient() {
      return builder -> builder.defaultHeader("X-Application", "stock-service");
    }

  }

  @Autowired
  Inventory inventory;

  @Test
  public void the_applications_beans_run_after_the_conventions() {

    server.enqueue(Stubs.item("1"));

    inventory.getItem("1");

    final var request = Stubs.next(server);
    assertThat(request.getHeaders().get("X-Caller")).isEqualTo("from-the-configurer");
    assertThat(request.getHeaders().get("X-Application")).isEqualTo("stock-service");

  }

}
