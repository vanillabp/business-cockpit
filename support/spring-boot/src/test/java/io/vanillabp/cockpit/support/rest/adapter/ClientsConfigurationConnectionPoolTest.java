package io.vanillabp.cockpit.support.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.support.rest.httpservice.HeldRequests;
import io.vanillabp.cockpit.support.rest.httpservice.Stubs;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.MockWebServer;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * {@code connection-pool-size} says how many idle connections a client keeps open for its next
 * requests. A client which sends many requests at the same time keeps as many as it is told, and one
 * which does not set the key keeps the five OkHttp keeps by default.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ClientsConfigurationConnectionPoolTest {

  private static final int REQUESTS_AT_ONCE = 8;

  private final MockWebServer server = new MockWebServer();

  private final HeldRequests heldRequests = new HeldRequests();

  @AfterEach
  void stop() {
    server.close();
  }

  private static class TestConfiguration extends ClientsConfigurationBase {
  }

  /** Sends eight requests at the same time, so the client opens eight connections. */
  private OkHttpClient afterEightRequestsAtOnce(
      final Client properties) throws InterruptedException {

    server.setDispatcher(heldRequests);
    final var builder = new OkHttpClient.Builder();
    new TestConfiguration().configureOkHttpClient(ClientsConfigurationConnectionPoolTest.class, properties, builder,
        null);
    final var httpClient = builder.build();

    try (var threadPerRequest = Executors.newVirtualThreadPerTaskExecutor()) {
      final var requests = new ArrayList<CompletableFuture<Void>>();
      for (int i = 0; i < REQUESTS_AT_ONCE; ++i) {
        requests.add(CompletableFuture.runAsync(() -> send(httpClient), threadPerRequest));
      }
      assertThat(heldRequests.waitingAfter(REQUESTS_AT_ONCE, Duration.ZERO)).isEqualTo(REQUESTS_AT_ONCE);
      heldRequests.release();
      requests.forEach(CompletableFuture::join);
    }
    return httpClient;

  }

  private void send(
      final OkHttpClient httpClient) {

    final var request = new Request.Builder().url(Stubs.apiUrl(server)
        + "/items/1").build();
    try (var response = httpClient.newCall(request).execute()) {
      response.body().string();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

  }

  /** The idle connections once OkHttp has closed those beyond the size of its pool. */
  private static int idleConnectionsOf(
      final OkHttpClient httpClient,
      final int expected) throws InterruptedException {

    final var giveUpAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while ((httpClient.connectionPool().idleConnectionCount() != expected) && (System.nanoTime() < giveUpAt)) {
      Thread.sleep(10);
    }
    return httpClient.connectionPool().idleConnectionCount();

  }

  @Test
  public void testTheClientKeepsAsManyIdleConnectionsAsConfigured() throws InterruptedException {

    final var properties = new Client();
    properties.setConnectionPoolSize(REQUESTS_AT_ONCE);

    final var httpClient = afterEightRequestsAtOnce(properties);

    assertThat(idleConnectionsOf(httpClient, REQUESTS_AT_ONCE)).isEqualTo(REQUESTS_AT_ONCE);

  }

  @Test
  public void testWithoutTheKeyTheClientKeepsFiveIdleConnectionsAsBefore() throws InterruptedException {

    final var properties = new Client();

    final var httpClient = afterEightRequestsAtOnce(properties);

    assertThat(properties.getConnectionPoolSize()).isEqualTo(Client.DEFAULT_CONNECTION_POOL_SIZE).isEqualTo(5);
    assertThat(idleConnectionsOf(httpClient, Client.DEFAULT_CONNECTION_POOL_SIZE))
        .isEqualTo(Client.DEFAULT_CONNECTION_POOL_SIZE);

  }

}
