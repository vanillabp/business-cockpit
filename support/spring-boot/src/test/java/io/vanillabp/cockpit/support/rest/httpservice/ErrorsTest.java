package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.cockpit.support.rest.httpservice.inventory.model.Reservation;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;

/**
 * What a caller gets: JSON bodies read and written with the application's Jackson, form bodies as
 * forms, and Spring's exceptions for an error status or a missing answer, where the REST client of
 * version 1 threw Feign's.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = {
        TestApplication.class, ErrorsTest.Clients.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
public class ErrorsTest {

  static final MockWebServer server = new MockWebServer();

  @DynamicPropertySource
  static void urls(
      final DynamicPropertyRegistry registry) {

    registry.add("spring.http.serviceclient.inventory.base-url", () -> Stubs.apiUrl(server));
    // nothing listens there
    registry.add("spring.http.serviceclient.nowhere.base-url", () -> "http://localhost:1/api");

  }

  @AfterAll
  static void stop() {
    server.close();
  }

  interface Inventory extends InventoryApi {
  }

  interface Nowhere extends InventoryApi {
  }

  @Configuration
  @ImportHttpServices(group = "inventory", types = Inventory.class)
  @ImportHttpServices(group = "nowhere", types = Nowhere.class)
  static class Clients {
  }

  @Autowired
  Inventory inventory;

  @Autowired
  Nowhere nowhere;

  @Test
  public void json_bodies_are_written_and_read() {

    Stubs.drain(server);
    server.enqueue(Stubs.json(201, """
        {"id": "r-1", "quantity": 3}"""));

    final var answer = inventory.reserveItem("1", new Reservation().quantity(3));

    assertThat(answer.getBody().getId()).isEqualTo("r-1");
    final var request = Stubs.next(server);
    assertThat(request.getHeaders().get("Content-Type")).startsWith("application/json");
    assertThat(request.getBody().utf8()).contains("\"quantity\":3");

  }

  @Test
  public void a_form_body_is_sent_as_a_form() {

    Stubs.drain(server);
    server.enqueue(new MockResponse.Builder().code(204).build());

    inventory.addNote("1", "counted twice");

    final var request = Stubs.next(server);
    assertThat(request.getHeaders().get("Content-Type")).startsWith("application/x-www-form-urlencoded");
    assertThat(request.getBody().utf8()).isEqualTo("text=counted+twice");

  }

  @Test
  public void a_date_time_query_parameter_is_written_in_iso_format() {

    Stubs.drain(server);
    server.enqueue(Stubs.json(200, "[]"));

    inventory.findItems(OffsetDateTime.of(2024, 1, 31, 12, 0, 0, 0, ZoneOffset.UTC));

    assertThat(Stubs.next(server).getTarget()).isEqualTo("/api/items?changedSince=2024-01-31T12%3A00%3A00Z");

  }

  @Test
  public void an_error_status_comes_as_springs_exception() {

    server.enqueue(new MockResponse.Builder().code(404).build());
    assertThatThrownBy(() -> inventory.getItem("1")).isInstanceOf(HttpClientErrorException.NotFound.class);

    server.enqueue(new MockResponse.Builder().code(500).build());
    assertThatThrownBy(() -> inventory.getItem("1")).isInstanceOf(HttpServerErrorException.InternalServerError.class);

  }

  @Test
  public void no_answer_comes_as_a_resource_access_exception() {

    assertThatThrownBy(() -> nowhere.getItem("1")).isInstanceOf(ResourceAccessException.class);

  }

}
