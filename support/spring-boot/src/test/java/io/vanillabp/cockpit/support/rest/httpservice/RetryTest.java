package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * {@code retry.*}: off by default. Switched on, a request without an answer is sent again, five
 * times at most unless the keys say otherwise. An answer with an error status is not sent again,
 * because the server saw the request.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = {
        TestApplication.class, RetryTest.Clients.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.http.clients.read-timeout=300ms", "spring.http.serviceclient.retrying.retry.enabled=true", "spring.http.serviceclient.thrice.retry.enabled=true", "spring.http.serviceclient.thrice.retry.max-attempts=3", "spring.http.serviceclient.thrice.retry.period=10ms", "spring.http.serviceclient.thrice.retry.max-period=50ms",
    })
@DirtiesContext
public class RetryTest {

  static final MockWebServer server = new MockWebServer();

  @DynamicPropertySource
  static void urls(
      final DynamicPropertyRegistry registry) {

    registry.add("spring.http.serviceclient.retrying.base-url", () -> Stubs.apiUrl(server));
    registry.add("spring.http.serviceclient.once.base-url", () -> Stubs.apiUrl(server));
    registry.add("spring.http.serviceclient.thrice.base-url", () -> Stubs.apiUrl(server));

  }

  @AfterAll
  static void stop() {
    server.close();
  }

  interface RetryingClient extends InventoryApi {
  }

  interface OnceClient extends InventoryApi {
  }

  interface ThriceClient extends InventoryApi {
  }

  @Configuration
  @ImportHttpServices(group = "retrying", types = RetryingClient.class)
  @ImportHttpServices(group = "once", types = OnceClient.class)
  @ImportHttpServices(group = "thrice", types = ThriceClient.class)
  static class Clients {
  }

  @Autowired
  RetryingClient retrying;

  @Autowired
  OnceClient once;

  @Autowired
  ThriceClient thrice;

  static MockResponse tooSlow() {
    return new MockResponse.Builder().code(200).headersDelay(1, TimeUnit.SECONDS).build();
  }

  @Test
  public void with_retry_a_request_without_answer_is_sent_again() {

    final var before = server.getRequestCount();
    server.enqueue(tooSlow());
    server.enqueue(Stubs.item("1"));

    assertThat(retrying.getItem("1").getBody().getId()).isEqualTo("1");
    assertThat(server.getRequestCount() - before).isEqualTo(2);

  }

  @Test
  public void without_retry_a_request_is_sent_once() {

    final var before = server.getRequestCount();
    server.enqueue(tooSlow());

    assertThatThrownBy(() -> once.getItem("1")).isInstanceOf(ResourceAccessException.class);
    assertThat(server.getRequestCount() - before).isEqualTo(1);

  }

  @Test
  public void the_keys_set_the_number_of_attempts() {

    final var before = server.getRequestCount();
    server.enqueue(tooSlow());
    server.enqueue(tooSlow());
    server.enqueue(tooSlow());

    assertThatThrownBy(() -> thrice.getItem("1")).isInstanceOf(ResourceAccessException.class);
    assertThat(server.getRequestCount() - before).isEqualTo(3);

  }

  @Test
  public void an_error_status_is_not_retried() {

    final var before = server.getRequestCount();
    server.enqueue(new MockResponse.Builder().code(503).build());

    assertThatThrownBy(() -> retrying.getItem("1")).hasMessageContaining("503");
    assertThat(server.getRequestCount() - before).isEqualTo(1);

  }

}
