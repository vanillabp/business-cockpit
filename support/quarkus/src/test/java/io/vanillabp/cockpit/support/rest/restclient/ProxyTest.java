package io.vanillabp.cockpit.support.rest.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.rest.restclient.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Credentials;

/**
 * A proxy: {@code proxy.host}, {@code proxy.port}, {@code proxy.username} and
 * {@code proxy.password}. The login reaches the proxy, and token requests go through the proxy as
 * well.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ProxyTest {

  private static final String STUB = "proxy";

  @RegisterExtension
  static final QuarkusExtensionTest application = new QuarkusExtensionTest()
      .withApplicationRoot(TheRestClientsOnQuarkus::withTheRestClients)
      // the hosts do not exist, only the proxy knows how to reach them
      .overrideConfigKey("proxied.base-url", "http://inventory.example/api")
      .overrideConfigKey("proxied.proxy.host", "127.0.0.1")
      .overrideConfigKey("proxied.proxy.port", Integer.toString(Stubs.portOf(STUB)))
      .overrideConfigKey("proxied.proxy.username", "scott")
      .overrideConfigKey("proxied.proxy.password", "tiger")
      .overrideConfigKey("proxied.oauth.token-uri", "http://login.example/oauth/token")
      .overrideConfigKey("proxied.oauth.client-id", "stock-service")
      .overrideConfigKey("proxied.oauth.client-secret", "s3cret");

  /** The stub plays the proxy. It sees the full URL of the target in the request line. */
  static final MockWebServer proxy = new MockWebServer();

  static final Stub stub = new Stub();

  /** Refuses requests without the login, answers the token endpoint and the API. */
  static class Stub extends Dispatcher {

    final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();

    @Override
    public MockResponse dispatch(
        final RecordedRequest request) {

      requests.add(request);
      if (request.getHeaders().get("Proxy-Authorization") == null) {
        return new MockResponse.Builder()
            .code(407)
            .addHeader("Proxy-Authenticate", "Basic realm=\"corporate\"")
            .build();
      }
      if (request.getRequestLine().contains("/oauth/token")) {
        return Stubs.json(200, """
            {"access_token": "token-1", "token_type": "Bearer", "expires_in": 3600}""");
      }
      return Stubs.item("1");

    }

  }

  @AfterAll
  static void stop() {
    proxy.close();
  }

  @BeforeEach
  void startTheProxy() {

    proxy.setDispatcher(stub);
    Stubs.started(proxy, STUB);
    stub.requests.clear();

  }

  @Test
  public void requests_and_token_requests_go_through_the_proxy_with_its_login() {

    final var proxied = TheRestClientsOnQuarkus.fresh().build(InventoryApi.class, "proxied");

    assertThat(proxied.getItem("1").getId()).isEqualTo("1");

    assertThat(stub.requests)
        .extracting(RecordedRequest::getRequestLine)
        .containsExactly(
            "POST http://login.example/oauth/token HTTP/1.1",
            "GET http://inventory.example/api/items/1 HTTP/1.1");
    assertThat(stub.requests)
        .extracting(request -> request.getHeaders().get("Proxy-Authorization"))
        .containsOnly(Credentials.basic("scott", "tiger"));
    assertThat(stub.requests.getLast().getHeaders().get("Authorization")).isEqualTo("Bearer token-1");

  }

}
