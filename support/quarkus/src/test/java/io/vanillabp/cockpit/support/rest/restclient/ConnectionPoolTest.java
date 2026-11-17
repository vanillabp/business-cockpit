package io.vanillabp.cockpit.support.rest.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.rest.restclient.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockWebServer;

/**
 * {@code connection-pool-size} says how many connections the clients at a prefix open at most at
 * the same time. Further requests wait for a free connection. Without the key Quarkus opens up to
 * 50, so a client which sends many requests at once needs no code of its own.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ConnectionPoolTest {

  private static final String STUB = "connection-pool";

  private static final int REQUESTS_AT_ONCE = 8;

  private static final int SMALL_POOL = 2;

  private static final Duration TIME_FOR_MORE_REQUESTS_TO_ARRIVE = Duration.ofMillis(300);

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(TheRestClientsOnQuarkus::withTheRestClients)
      .overrideConfigKey("small-pool.base-url", Stubs.url(STUB, "/api"))
      .overrideConfigKey("small-pool.connection-pool-size", Integer.toString(SMALL_POOL))
      // the default read timeout would end the requests the pool lets wait before the test looks
      .overrideConfigKey("small-pool.read-timeout", "20s")
      .overrideConfigKey("default-pool.base-url", Stubs.url(STUB, "/api"))
      .overrideConfigKey("default-pool.read-timeout", "20s");

  static final MockWebServer server = new MockWebServer();

  @AfterAll
  static void stop() {
    server.close();
  }

  /** Sends eight requests at once and answers how many of them reached the server. */
  private static int requestsWhichReachTheServerAtOnce(
      final String prefix,
      final int expected) throws InterruptedException {

    final var heldRequests = new HeldRequests();
    Stubs.started(server, STUB).setDispatcher(heldRequests);
    final var inventory = TheRestClientsOnQuarkus.fresh().build(InventoryApi.class, prefix);

    try (var threadPerRequest = Executors.newVirtualThreadPerTaskExecutor()) {
      final var requests = new ArrayList<CompletableFuture<?>>();
      for (int i = 0; i < REQUESTS_AT_ONCE; ++i) {
        requests.add(CompletableFuture.supplyAsync(() -> inventory.getItem("1"), threadPerRequest));
      }
      final var reachedTheServer = heldRequests.waitingAfter(expected, TIME_FOR_MORE_REQUESTS_TO_ARRIVE);
      heldRequests.release();
      requests.forEach(CompletableFuture::join);
      return reachedTheServer;
    }

  }

  @Test
  public void the_clients_open_no_more_connections_than_the_key_says() throws InterruptedException {

    assertThat(requestsWhichReachTheServerAtOnce("small-pool", SMALL_POOL)).isEqualTo(SMALL_POOL);

  }

  @Test
  public void without_the_key_the_clients_open_a_connection_for_each_of_many_requests() throws InterruptedException {

    assertThat(requestsWhichReachTheServerAtOnce("default-pool", REQUESTS_AT_ONCE)).isEqualTo(REQUESTS_AT_ONCE);

  }

}
