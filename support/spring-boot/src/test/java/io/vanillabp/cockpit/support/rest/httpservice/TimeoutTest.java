package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
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
 * The timeouts: a group's own key wins over the one for all clients in
 * {@code spring.http.clients.*}, and without either the timeouts of the REST client of version 1
 * apply. Redirects are followed.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = {
        TestApplication.class, TimeoutTest.Clients.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.http.clients.read-timeout=300ms", "spring.http.serviceclient.patient.read-timeout=3s",
    })
@DirtiesContext
public class TimeoutTest {

  static final MockWebServer server = new MockWebServer();

  @DynamicPropertySource
  static void urls(
      final DynamicPropertyRegistry registry) {

    registry.add("spring.http.serviceclient.impatient.base-url", () -> Stubs.apiUrl(server));
    registry.add("spring.http.serviceclient.patient.base-url", () -> Stubs.apiUrl(server));

  }

  @AfterAll
  static void stop() {
    server.close();
  }

  interface ImpatientClient extends InventoryApi {
  }

  interface PatientClient extends InventoryApi {
  }

  @Configuration
  @ImportHttpServices(group = "impatient", types = ImpatientClient.class)
  @ImportHttpServices(group = "patient", types = PatientClient.class)
  static class Clients {
  }

  @Autowired
  ImpatientClient impatient;

  @Autowired
  PatientClient patient;

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
  public void the_timeout_for_all_clients_applies_to_a_group_without_its_own() {

    server.enqueue(oneSecondLate());

    assertThatThrownBy(() -> impatient.getItem("1")).isInstanceOf(ResourceAccessException.class);

  }

  @Test
  public void the_timeout_of_the_group_wins() {

    server.enqueue(oneSecondLate());

    assertThat(patient.getItem("1").getBody().getId()).isEqualTo("1");

  }

  @Test
  public void redirects_are_followed() {

    Stubs.drain(server);
    server.enqueue(new MockResponse.Builder()
        .code(302)
        .addHeader("Location", Stubs.apiUrl(server)
            + "/items/2")
        .build());
    server.enqueue(Stubs.item("2"));

    assertThat(patient.getItem("1").getBody().getId()).isEqualTo("2");

  }

  @Test
  public void without_any_key_the_timeouts_of_clients_configuration_base_apply() {

    assertThat(HttpServiceClientConventions.DEFAULT_SETTINGS.connectTimeout()).isEqualTo(Duration.ofMillis(1500));
    assertThat(HttpServiceClientConventions.DEFAULT_SETTINGS.readTimeout()).isEqualTo(Duration.ofSeconds(10));

  }

}
