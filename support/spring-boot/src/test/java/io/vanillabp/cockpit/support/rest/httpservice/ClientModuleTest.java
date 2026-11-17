package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockWebServer;

/**
 * An application which adds the client module, sets its keys and injects the client. The keys are
 * below the prefix the client module chose, Spring Boot's and the helper's alike.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = TestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "inventory.client.default-header.X-Caller=stock-service", "inventory.client.retry.enabled=true",
    })
@ImportAutoConfiguration(InventoryClientAutoConfiguration.class)
@DirtiesContext
public class ClientModuleTest {

  static final MockWebServer server = new MockWebServer();

  @DynamicPropertySource
  static void urls(
      final DynamicPropertyRegistry registry) {
    registry.add("inventory.client.base-url", () -> Stubs.apiUrl(server));
  }

  @AfterAll
  static void stop() {
    server.close();
  }

  @Autowired
  InventoryClient inventory;

  @Test
  public void the_keys_below_the_prefix_of_the_client_module_apply() {

    server.enqueue(Stubs.item("1"));

    assertThat(inventory.getItem("1").getBody().getName()).isEqualTo("Screw");

    final var request = Stubs.next(server);
    assertThat(request.getTarget()).isEqualTo("/api/items/1");
    assertThat(request.getHeaders().get("X-Caller")).isEqualTo("stock-service");

  }

}
