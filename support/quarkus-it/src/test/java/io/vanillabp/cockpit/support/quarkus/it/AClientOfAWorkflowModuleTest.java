package io.vanillabp.cockpit.support.quarkus.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

/**
 * A workflow module builds the client of a service with the builder of the support jar, from keys
 * at its own prefix. Quarkus finds the builder and the client of the OAuth token endpoint in the jar
 * on its own: the client fetches a token and sends it.
 */
@QuarkusTest
@QuarkusTestResource(TheInventoryOfTheTests.class)
@ExtendWith(SuppressOutputExtension.class)
public class AClientOfAWorkflowModuleTest {

  static final MockWebServer server = new MockWebServer();

  static final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();

  @Inject
  InventoryClient inventory;

  @AfterAll
  static void stop() {
    server.close();
  }

  @Test
  public void theClientFetchesItsTokenAndCallsTheService() throws IOException {

    server.setDispatcher(new Dispatcher() {

      @Override
      public MockResponse dispatch(
          final RecordedRequest request) {

        requests.add(request);
        if (request.getTarget().startsWith("/oauth/token")) {
          return new MockResponse.Builder()
              .addHeader("Content-Type", "application/json")
              .body("""
                  {"access_token": "token-1", "expires_in": 3600}""")
              .build();
        }
        return new MockResponse.Builder().addHeader("Content-Type", "text/plain").body("Screw").build();

      }

    });
    server.start(InetAddress.getByName("127.0.0.1"), Integer.getInteger(TheInventoryOfTheTests.PORT));

    assertThat(inventory.nameOf("1")).isEqualTo("Screw");

    assertThat(requests)
        .extracting(RecordedRequest::getTarget)
        .containsExactly("/oauth/token", "/api/items/1");
    assertThat(requests.getLast().getHeaders().get("Authorization")).isEqualTo("Bearer token-1");

  }

}
