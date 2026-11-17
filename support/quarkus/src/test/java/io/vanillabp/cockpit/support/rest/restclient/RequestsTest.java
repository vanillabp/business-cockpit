package io.vanillabp.cockpit.support.rest.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.rest.restclient.inventory.InventoryApi;
import io.vanillabp.cockpit.support.rest.restclient.inventory.model.Reservation;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;

/**
 * What a client built from the keys sends and reads: JSON, plain text and form bodies, a date as an ISO query
 * parameter, the headers of {@code default-header.*}, and redirects. An answer with an error status
 * comes as the REST client's {@link WebApplicationException}, no answer at all as a
 * {@link ProcessingException}, and the read timeout of the keys applies.
 */
@ExtendWith(SuppressOutputExtension.class)
public class RequestsTest {

  private static final String STUB = "requests";

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(TheRestClientsOnQuarkus::withTheRestClients)
      .overrideConfigKey("inventory.base-url", Stubs.url(STUB, "/api"))
      .overrideConfigKey("inventory.read-timeout", "300ms")
      .overrideConfigKey("inventory.default-header.X-Tenant", "north")
      .overrideConfigKey("not-following.base-url", Stubs.url(STUB, "/api"))
      .overrideConfigKey("not-following.redirects", "dont-follow");

  static final MockWebServer server = new MockWebServer();

  @AfterAll
  static void stop() {
    server.close();
  }

  InventoryApi inventory;

  @BeforeEach
  void startTheStub() {

    Stubs.started(server, STUB);
    Stubs.drain(server);
    inventory = TheRestClientsOnQuarkus.fresh().build(InventoryApi.class, "inventory");

  }

  @Test
  public void json_bodies_are_written_and_read() {

    server.enqueue(Stubs.json(201, """
        {"id": "r-1", "quantity": 3}"""));

    final var reservation = inventory.reserveItem("1", new Reservation().quantity(3));

    assertThat(reservation.getId()).isEqualTo("r-1");
    final var request = Stubs.next(server);
    assertThat(request.getTarget()).isEqualTo("/api/items/1/reservations");
    assertThat(request.getBody().utf8()).contains("\"quantity\":3");
    assertThat(request.getHeaders().get("Content-Type")).startsWith("application/json");

  }

  @Test
  public void a_plain_text_answer_is_read_into_a_string() {

    server.enqueue(new MockResponse.Builder()
        .code(Response.Status.OK.getStatusCode())
        .addHeader("Content-Type", "text/plain")
        .body("Shelf 3, left")
        .build());

    assertThat(inventory.getLabel("1")).isEqualTo("Shelf 3, left");
    assertThat(Stubs.next(server).getHeaders().get("Accept")).contains("text/plain");

  }

  @Test
  public void a_builder_created_with_new_reads_the_keys_without_cdi() {

    server.enqueue(Stubs.item("1"));

    final var builtWithoutCdi = new RestClients().build(InventoryApi.class, "inventory");

    assertThat(builtWithoutCdi.getItem("1").getId()).isEqualTo("1");
    assertThat(Stubs.next(server).getHeaders().get("X-Tenant")).isEqualTo("north");

  }

  @Test
  public void a_form_body_is_sent_as_a_form() {

    server.enqueue(new MockResponse.Builder().code(204).build());

    inventory.addNote("1", "Sold out soon");

    final var request = Stubs.next(server);
    assertThat(request.getHeaders().get("Content-Type")).startsWith("application/x-www-form-urlencoded");
    assertThat(request.getBody().utf8()).isEqualTo("text=Sold+out+soon");

  }

  @Test
  public void a_date_time_query_parameter_is_written_in_iso_format() {

    server.enqueue(Stubs.json(200, "[]"));

    inventory.findItems(OffsetDateTime.parse("2024-01-31T12:00:00Z"));

    assertThat(Stubs.next(server).getTarget()).contains("changedSince=2024-01-31T12%3A00Z");

  }

  @Test
  public void the_default_headers_of_the_keys_are_sent() {

    server.enqueue(Stubs.item("1"));

    inventory.getItem("1");

    assertThat(Stubs.next(server).getHeaders().get("X-Tenant")).isEqualTo("north");

  }

  @Test
  public void an_error_status_comes_as_the_rest_clients_exception() {

    server.enqueue(new MockResponse.Builder().code(404).build());

    assertThatThrownBy(() -> inventory.getItem("unknown"))
        .isInstanceOf(WebApplicationException.class)
        .satisfies(e -> assertThat(((WebApplicationException) e).getResponse().getStatus()).isEqualTo(404));

  }

  @Test
  public void no_answer_within_the_read_timeout_comes_as_a_processing_exception() {

    server.enqueue(Stubs.tooSlow());

    assertThatThrownBy(() -> inventory.getItem("1")).isInstanceOf(ProcessingException.class);

  }

  @Test
  public void redirects_are_followed_unless_the_keys_say_otherwise() {

    server.enqueue(new MockResponse.Builder().code(302).addHeader("Location", "/api/items/2").build());
    server.enqueue(Stubs.item("2"));
    assertThat(inventory.getItem("1").getId()).isEqualTo("2");

    server.enqueue(new MockResponse.Builder().code(302).addHeader("Location", "/api/items/2").build());
    final var notFollowing = TheRestClientsOnQuarkus.fresh().build(InventoryApi.class, "not-following");
    assertThatThrownBy(() -> notFollowing.getItem("1")).isInstanceOf(WebApplicationException.class);

  }

}
