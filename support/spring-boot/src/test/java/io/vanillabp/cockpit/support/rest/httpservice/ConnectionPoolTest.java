package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockWebServer;

/**
 * {@code connection-pool-size} says how many connections a group opens at most at the same time.
 * Apache HttpClient 5 opens five by default and lets every further request wait, so a client which
 * sends many requests at once sets the key and needs no code of its own. The JDK's HttpClient has
 * no limit, so the key changes nothing there. An HTTP library the key does not reach stops the
 * start, so the key never seems to work while it does nothing.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ConnectionPoolTest {

  private static final int REQUESTS_AT_ONCE = 8;

  /** What Apache HttpClient 5 opens to one server at the same time if nobody says otherwise. */
  private static final int APACHE_HTTP_CLIENT_DEFAULT = 5;

  private static final Duration TIME_FOR_MORE_REQUESTS_TO_ARRIVE = Duration.ofMillis(300);

  private final MockWebServer server = new MockWebServer();

  private final HeldRequests heldRequests = new HeldRequests();

  @AfterEach
  void stop() {
    server.close();
  }

  interface LoadDriverClient extends InventoryApi {
  }

  /** The group of a client module, with its keys below a prefix of its own. */
  @Configuration
  @ImportHttpServices(group = "load-driver", types = LoadDriverClient.class)
  static class LoadDriverModule {

    @Bean
    HttpServiceGroupPrefix loadDriverKeys() {
      return HttpServiceGroupPrefix.of("load-driver", "load-driver.client");
    }

  }

  private static final Class<?>[] LOAD_DRIVER_MODULE = {
      TestApplication.class, LoadDriverModule.class
  };

  /** Starts the application and sends eight requests at once. Answers how many reached the server. */
  private int requestsWhichReachTheServerAtOnce(
      final int expected,
      final String... properties) throws InterruptedException {

    server.setDispatcher(heldRequests);
    final var allProperties = new ArrayList<String>(List.of(properties));
    allProperties.add("load-driver.client.base-url="
        + Stubs.apiUrl(server));
    // the default read timeout would end the requests the pool lets wait before the test looks
    allProperties.add("load-driver.client.read-timeout=20s");

    try (var context = new SpringApplicationBuilder(LOAD_DRIVER_MODULE)
        .web(WebApplicationType.NONE)
        .run(Starts.asArguments(allProperties.toArray(String[]::new))); var threadPerRequest = Executors
            .newVirtualThreadPerTaskExecutor()) {

      final var client = context.getBean(LoadDriverClient.class);
      final var requests = new ArrayList<CompletableFuture<?>>();
      for (int i = 0; i < REQUESTS_AT_ONCE; ++i) {
        requests.add(CompletableFuture.supplyAsync(() -> client.getItem("1"), threadPerRequest));
      }
      final var reachedTheServer = heldRequests.waitingAfter(expected, TIME_FOR_MORE_REQUESTS_TO_ARRIVE);
      heldRequests.release();
      requests.forEach(CompletableFuture::join);
      return reachedTheServer;

    }

  }

  @Test
  public void apache_http_client_opens_as_many_connections_as_the_key_says() throws InterruptedException {

    assertThat(requestsWhichReachTheServerAtOnce(REQUESTS_AT_ONCE,
        "spring.http.clients.imperative.factory=http-components",
        "load-driver.client.connection-pool-size="
            + REQUESTS_AT_ONCE))
        .isEqualTo(REQUESTS_AT_ONCE);

  }

  @Test
  public void apache_http_client_without_the_key_lets_requests_wait_beyond_its_default() throws InterruptedException {

    assertThat(requestsWhichReachTheServerAtOnce(APACHE_HTTP_CLIENT_DEFAULT,
        "spring.http.clients.imperative.factory=http-components"))
        .isEqualTo(APACHE_HTTP_CLIENT_DEFAULT);

  }

  @Test
  public void the_jdks_http_client_opens_a_connection_for_each_request_with_or_without_the_key() throws InterruptedException {

    assertThat(requestsWhichReachTheServerAtOnce(REQUESTS_AT_ONCE,
        "spring.http.clients.imperative.factory=jdk",
        "load-driver.client.connection-pool-size=2"))
        .isEqualTo(REQUESTS_AT_ONCE);

  }

  @Test
  public void a_pool_without_connections_stops_the_start() {

    assertThat(Starts.problemsOf(LOAD_DRIVER_MODULE,
        "load-driver.client.base-url=http://localhost:1/api",
        "load-driver.client.connection-pool-size=0"))
        .anyMatch(problem -> problem.contains("'load-driver.client.connection-pool-size: 0'") && problem
            .contains("Set at least 1"));

  }

  @Test
  public void an_http_library_without_a_pool_size_stops_the_start() {

    assertThat(Starts.problemsOf(LOAD_DRIVER_MODULE,
        "spring.http.clients.imperative.factory=simple",
        "load-driver.client.base-url=http://localhost:1/api",
        "load-driver.client.connection-pool-size=8"))
        .anyMatch(problem -> problem.contains("'load-driver.client.connection-pool-size'") && problem
            .contains("'spring.http.clients.imperative.factory' to 'jdk' or 'http-components'"));

  }

}
