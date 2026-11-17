package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Credentials;

/**
 * A proxy: {@code proxy.host}, {@code proxy.port}, {@code proxy.username} and
 * {@code proxy.password}. Spring Boot has no proxy key. The login is sent with every request, so
 * the proxy does not have to ask, and the bearer token for the server behind it is kept. Token
 * requests go through the proxy as well.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = {
        TestApplication.class, ProxyTest.Clients.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        // the hosts do not exist, only the proxy knows how to reach them
        "spring.http.serviceclient.proxied.base-url=http://inventory.example/api", "spring.http.serviceclient.proxied.proxy.username=scott", "spring.http.serviceclient.proxied.proxy.password=tiger", "spring.http.serviceclient.proxied.oauth.token-uri=http://login.example/oauth/token", "spring.http.serviceclient.proxied.oauth.client-id=stock-service", "spring.http.serviceclient.proxied.oauth.client-secret=s3cret",
    })
@DirtiesContext
public class ProxyTest {

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

  @DynamicPropertySource
  static void urls(
      final DynamicPropertyRegistry registry) {

    proxy.setDispatcher(stub);
    registry.add("spring.http.serviceclient.proxied.proxy.host", () -> Stubs.started(proxy).getHostName());
    registry.add("spring.http.serviceclient.proxied.proxy.port", proxy::getPort);

  }

  @AfterAll
  static void stop() {
    proxy.close();
  }

  interface ProxiedClient extends InventoryApi {
  }

  @Configuration
  @ImportHttpServices(group = "proxied", types = ProxiedClient.class)
  static class Clients {
  }

  @Autowired
  ProxiedClient proxied;

  @BeforeEach
  void forgetEarlierRequests() {
    stub.requests.clear();
  }

  @Test
  public void requests_and_token_requests_go_through_the_proxy_with_its_login() {

    assertThat(proxied.getItem("1").getBody().getId()).isEqualTo("1");

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
