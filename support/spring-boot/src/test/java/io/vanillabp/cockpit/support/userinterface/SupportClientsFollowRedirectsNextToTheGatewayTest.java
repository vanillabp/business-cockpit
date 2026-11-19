package io.vanillabp.cockpit.support.userinterface;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.support.rest.httpservice.Stubs;
import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;

/**
 * An application with Spring Cloud Gateway, which sets {@code spring.http.clients.redirects} to
 * {@code dont-follow} for every HTTP client. An HTTP service group of {@code support} follows a
 * redirect all the same, because {@code support} follows redirects unless the group's own
 * {@code redirects} key says otherwise. A group with that key keeps it.
 */
@SpringBootTest(classes = SupportClientsFollowRedirectsNextToTheGatewayTest.Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ExtendWith(SuppressOutputExtension.class)
public class SupportClientsFollowRedirectsNextToTheGatewayTest {

  private static final MockWebServer server = new MockWebServer();

  @Autowired
  private Environment environment;

  @Autowired
  private Following inventory;

  @Autowired
  private NotFollowing notFollowing;

  /** The API in a group without a key of its own for redirects. */
  interface Following extends InventoryApi {
  }

  /** The same API in a group which says itself that it does not follow a redirect. */
  interface NotFollowing extends InventoryApi {
  }

  @DynamicPropertySource
  static void address(
      final DynamicPropertyRegistry registry) throws IOException {

    registry.add("spring.http.serviceclient.inventory.base-url", () -> Stubs.apiUrl(server));
    registry.add("spring.http.serviceclient.not-following.base-url", () -> Stubs.apiUrl(server));
    registry.add("spring.http.serviceclient.not-following.redirects", () -> "dont-follow");

  }

  @AfterAll
  static void stop() {

    server.close();

  }

  @Test
  public void theGatewayTellsEveryClientNotToFollowRedirects() {

    assertThat(environment.getProperty("spring.http.clients.redirects", HttpRedirects.class))
        .isEqualTo(HttpRedirects.DONT_FOLLOW);

  }

  @Test
  public void aClientOfSupportFollowsARedirectAllTheSame() {

    Stubs.drain(server);
    server.enqueue(new MockResponse.Builder()
        .code(HttpStatus.FOUND.value())
        .addHeader(HttpHeaders.LOCATION, Stubs.apiUrl(server)
            + "/items/2")
        .build());
    server.enqueue(Stubs.item("2"));

    assertThat(inventory.getItem("1").getBody().getId()).isEqualTo("2");

  }

  @Test
  public void theOwnKeyOfAGroupStillWins() {

    Stubs.drain(server);
    server.enqueue(new MockResponse.Builder()
        .code(HttpStatus.FOUND.value())
        .addHeader(HttpHeaders.LOCATION, Stubs.apiUrl(server)
            + "/items/2")
        .build());

    assertThat(notFollowing.getItem("1").getStatusCode()).isEqualTo(HttpStatus.FOUND);

  }

  /** An application with two HTTP service groups at Spring Boot's place. */
  @SpringBootConfiguration
  @EnableAutoConfiguration
  @ImportHttpServices(group = "inventory", types = Following.class)
  @ImportHttpServices(group = "not-following", types = NotFollowing.class)
  static class Application {
  }

}
