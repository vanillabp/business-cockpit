package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;

/**
 * An operation which answers {@code text/plain} returns a {@code String}, and a client module needs
 * no code of its own for it: the HTTP service clients read text out of the box, next to JSON.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = {
        TestApplication.class, PlainTextTest.Clients.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
public class PlainTextTest {

  static final MockWebServer server = new MockWebServer();

  @DynamicPropertySource
  static void urls(
      final DynamicPropertyRegistry registry) {

    registry.add("spring.http.serviceclient.labels.base-url", () -> Stubs.apiUrl(server));

  }

  @AfterAll
  static void stop() {
    server.close();
  }

  interface LabelsClient extends InventoryApi {
  }

  @Configuration
  @ImportHttpServices(group = "labels", types = LabelsClient.class)
  static class Clients {
  }

  @Autowired
  LabelsClient labels;

  @Test
  public void a_plain_text_answer_is_read_into_a_string() {

    Stubs.drain(server);
    server.enqueue(new MockResponse.Builder()
        .code(HttpStatus.OK.value())
        .addHeader("Content-Type", "text/plain")
        .body("Shelf 3, left")
        .build());

    assertThat(labels.getLabel("1").getBody()).isEqualTo("Shelf 3, left");
    assertThat(Stubs.next(server).getHeaders().get("Accept")).contains("text/plain");

  }

  @Test
  public void json_is_still_read_into_the_generated_model() {

    server.enqueue(Stubs.item("7"));

    assertThat(labels.getItem("7").getBody().getStock()).isEqualTo(7);

  }

}
