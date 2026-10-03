package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.sun.net.httpserver.HttpServer;

import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.transport.RestTransport;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;
import io.vanillabp.integration.spi.PhaseTwoRetryLater;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the transport does with the answer the cockpit server gives when it could not store a
 * report: {@code 503 Service Unavailable}, without a {@code Retry-After}. The server does not know
 * when its database is back, so it names no moment. The entry has to come back anyway, after the
 * backoff of the outbox store.
 */
@ExtendWith(SuppressOutputExtension.class)
public class AReportTheServerCouldNotStoreTest {

  private HttpServer server;

  private RestTransport transport;

  @BeforeEach
  public void startACockpitServerWhichCannotStore() throws IOException {

    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server
        .createContext(
            "/",
            exchange -> {
              exchange.getRequestBody().readAllBytes();
              exchange.sendResponseHeaders(503, -1);
              exchange.close();
            });
    server.start();
    transport = new RestTransport(RestTransportTest.aCockpitServerAt(server.getAddress().getPort()));

  }

  @AfterEach
  public void stopTheCockpitServer() {

    server.stop(0);

  }

  @Test
  @DisplayName("A report the server could not store is repeated after the store's own backoff")
  public void theReportIsRepeatedAfterTheBackoffOfTheStore() {

    final var failure = assertThrows(
        RuntimeException.class,
        () -> transport.publishUserTaskEvent(EventFixture.userTask(UserTaskEventKind.CREATED)));

    assertFalse(
        PhaseTwoPermanentFailure.isPermanent(failure),
        "a report the server could not store was given up, and it is lost");
    assertInstanceOf(PhaseTwoRetryLater.class, failure);
    assertNull(
        PhaseTwoRetryLater.retryAfter(failure),
        "the server named no moment, so the backoff of the outbox store has to decide");

  }

}
