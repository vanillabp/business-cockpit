package io.vanillabp.cockpit.support.rest.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.rest.restclient.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;

/**
 * {@code retry.*}: a request without an answer is sent again, as often as the keys say. An answer
 * with an error status is not, because the server saw the request. Without the keys a request is
 * sent once.
 */
@ExtendWith(SuppressOutputExtension.class)
public class RetryTest {

  private static final String STUB = "retry";

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(TheRestClientsOnQuarkus::withTheRestClients)
      .overrideConfigKey("retrying.base-url", Stubs.url(STUB, "/api"))
      .overrideConfigKey("retrying.read-timeout", "300ms")
      .overrideConfigKey("retrying.retry.enabled", "true")
      .overrideConfigKey("once.base-url", Stubs.url(STUB, "/api"))
      .overrideConfigKey("once.read-timeout", "300ms")
      .overrideConfigKey("thrice.base-url", Stubs.url(STUB, "/api"))
      .overrideConfigKey("thrice.read-timeout", "300ms")
      .overrideConfigKey("thrice.retry.enabled", "true")
      .overrideConfigKey("thrice.retry.max-attempts", "3")
      .overrideConfigKey("thrice.retry.period", "10ms")
      .overrideConfigKey("thrice.retry.max-period", "20ms");

  static final MockWebServer server = new MockWebServer();

  @AfterAll
  static void stop() {
    server.close();
  }

  @BeforeEach
  void startTheStub() {
    Stubs.started(server, STUB);
  }

  private static InventoryApi client(
      final String prefix) {
    return TheRestClientsOnQuarkus.fresh().build(InventoryApi.class, prefix);
  }

  @Test
  public void with_retry_a_request_without_answer_is_sent_again() {

    final var before = server.getRequestCount();
    server.enqueue(Stubs.tooSlow());
    server.enqueue(Stubs.item("1"));

    assertThat(client("retrying").getItem("1").getId()).isEqualTo("1");
    assertThat(server.getRequestCount() - before).isEqualTo(2);

  }

  @Test
  public void without_retry_a_request_is_sent_once() {

    final var before = server.getRequestCount();
    server.enqueue(Stubs.tooSlow());

    assertThatThrownBy(() -> client("once").getItem("1")).isInstanceOf(ProcessingException.class);
    assertThat(server.getRequestCount() - before).isEqualTo(1);

  }

  @Test
  public void the_keys_set_the_number_of_attempts() {

    final var before = server.getRequestCount();
    server.enqueue(Stubs.tooSlow());
    server.enqueue(Stubs.tooSlow());
    server.enqueue(Stubs.tooSlow());

    assertThatThrownBy(() -> client("thrice").getItem("1")).isInstanceOf(ProcessingException.class);
    assertThat(server.getRequestCount() - before).isEqualTo(3);

  }

  @Test
  public void an_error_status_is_not_retried() {

    final var before = server.getRequestCount();
    server.enqueue(new MockResponse.Builder().code(503).build());

    assertThatThrownBy(() -> client("retrying").getItem("1")).isInstanceOf(WebApplicationException.class);
    assertThat(server.getRequestCount() - before).isEqualTo(1);

  }

  @Test
  public void the_retrying_client_answers_for_itself() {

    final var client = client("retrying");

    assertThat(client).isEqualTo(client).isNotEqualTo(client("retrying"));
    assertThat(client.hashCode()).isEqualTo(System.identityHashCode(client));
    assertThat(client.toString()).startsWith("retrying ");

  }

}
