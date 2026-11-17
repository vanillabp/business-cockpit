package io.vanillabp.cockpit.support.rest.httpservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.service.registry.ImportHttpServices;

import io.vanillabp.cockpit.support.rest.httpservice.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Credentials;

/**
 * {@code oauth.*}: a bearer token of the client credentials grant, fetched by Spring Security,
 * shared by all requests of the group, replaced before it expires and dropped after a {@code 401}.
 * <p>
 * Each test starts an application of its own, so no token of an earlier test is left.
 */
@ExtendWith(SuppressOutputExtension.class)
public class OAuthTest {

  /** Answers the token endpoint itself and the API from a queue. */
  static class Stub extends Dispatcher {

    final AtomicInteger issued = new AtomicInteger();

    final List<RecordedRequest> tokenRequests = new CopyOnWriteArrayList<>();

    final BlockingQueue<MockResponse> apiAnswers = new LinkedBlockingQueue<>();

    final List<RecordedRequest> apiRequests = new CopyOnWriteArrayList<>();

    volatile IntFunction<MockResponse> tokenAnswer;

    @Override
    public MockResponse dispatch(
        final RecordedRequest request) {

      if (request.getTarget().startsWith("/oauth/token")) {
        tokenRequests.add(request);
        return tokenAnswer.apply(issued.incrementAndGet());
      }
      apiRequests.add(request);
      final var answer = apiAnswers.poll();
      return answer == null ? Stubs.item("1") : answer;

    }

  }

  static MockResponse token(
      final int number,
      final int expiresIn) {

    return Stubs.json(200, """
        {"access_token": "token-%d", "token_type": "Bearer", "expires_in": %d}"""
        .formatted(number, expiresIn));

  }

  static MockResponse tokenWithoutExpiresIn(
      final int number) {

    return Stubs.json(200, """
        {"access_token": "token-%d", "token_type": "Bearer"}"""
        .formatted(number));

  }

  static final MockWebServer server = new MockWebServer();

  static final Stub stub = new Stub();

  interface StockClient extends InventoryApi {
  }

  @Configuration
  @ImportHttpServices(group = "stock", types = StockClient.class)
  static class Clients {

    @Bean
    HttpServiceGroupPrefix stockKeys() {
      return HttpServiceGroupPrefix.of("stock", "warehouse.stock");
    }

  }

  @AfterAll
  static void stop() {
    server.close();
  }

  @BeforeEach
  void reset() {

    server.setDispatcher(stub);
    stub.issued.set(0);
    stub.tokenRequests.clear();
    stub.apiRequests.clear();
    stub.apiAnswers.clear();
    stub.tokenAnswer = number -> token(number, 3600);

  }

  private ConfigurableApplicationContext start(
      final String... properties) {

    final var keys = new ArrayList<>(List.of(
        "warehouse.stock.base-url="
            + Stubs.apiUrl(server),
        "warehouse.stock.oauth.token-uri="
            + server.url("/oauth/token"),
        "warehouse.stock.oauth.client-id=stock-service",
        "warehouse.stock.oauth.client-secret=s3cret"));
    keys.addAll(List.of(properties));
    return new SpringApplicationBuilder(TestApplication.class, Clients.class)
        .web(WebApplicationType.NONE)
        .properties(keys.toArray(String[]::new))
        .run();

  }

  @Test
  public void one_token_serves_all_requests() {

    try (var context = start()) {
      final var stock = context.getBean(StockClient.class);

      stock.getItem("1");
      stock.getItem("2");

      assertThat(stub.tokenRequests).hasSize(1);
      assertThat(stub.tokenRequests.getFirst().getBody().utf8())
          .contains("grant_type=client_credentials", "client_id=stock-service", "client_secret=s3cret");
      assertThat(stub.apiRequests)
          .extracting(request -> request.getHeaders().get("Authorization"))
          .containsOnly("Bearer token-1");
    }

  }

  @Test
  public void client_id_and_secret_can_go_into_a_basic_login_header() {

    try (var context = start("warehouse.stock.oauth.client-authentication-method=client_secret_basic",
        "warehouse.stock.oauth.scope=stock.read,stock.write")) {

      context.getBean(StockClient.class).getItem("1");

      final var tokenRequest = stub.tokenRequests.getFirst();
      assertThat(tokenRequest.getHeaders().get("Authorization"))
          .isEqualTo(Credentials.basic("stock-service", "s3cret"));
      assertThat(tokenRequest.getBody().utf8())
          .doesNotContain("client_secret")
          .contains("scope=stock.read+stock.write");
    }

  }

  @Test
  public void a_token_is_replaced_when_a_tenth_of_its_lifetime_is_left() throws InterruptedException {

    // lives one second, so it is replaced after 900 milliseconds, not 30 seconds before it expires
    stub.tokenAnswer = number -> token(number, 1);
    try (var context = start()) {
      final var stock = context.getBean(StockClient.class);

      stock.getItem("1");
      stock.getItem("2");
      Thread.sleep(1000);
      stock.getItem("3");

      assertThat(stub.tokenRequests).hasSize(2);
    }

  }

  @Test
  public void a_token_without_expires_in_is_kept_for_the_configured_lifetime() throws InterruptedException {

    stub.tokenAnswer = OAuthTest::tokenWithoutExpiresIn;
    try (var context = start("warehouse.stock.oauth.lifetime-without-expires-in=2s")) {
      final var stock = context.getBean(StockClient.class);

      stock.getItem("1");
      stock.getItem("2");
      assertThat(stub.tokenRequests).hasSize(1);

      Thread.sleep(2000);
      stock.getItem("3");
      assertThat(stub.tokenRequests).hasSize(2);
    }

  }

  @Test
  public void a_token_without_expires_in_is_kept_for_an_hour_by_default() {

    stub.tokenAnswer = OAuthTest::tokenWithoutExpiresIn;
    try (var context = start()) {
      final var stock = context.getBean(StockClient.class);

      stock.getItem("1");
      stock.getItem("2");

      assertThat(stub.tokenRequests).hasSize(1);
    }

  }

  @Test
  public void after_a_401_the_token_is_dropped_and_the_retry_brings_a_new_one() {

    try (var context = start("warehouse.stock.retry.enabled=true")) {
      stub.apiAnswers.add(new MockResponse.Builder().code(401).build());

      assertThat(context.getBean(StockClient.class).getItem("1").getBody().getId()).isEqualTo("1");

      assertThat(stub.tokenRequests).hasSize(2);
      assertThat(stub.apiRequests)
          .extracting(request -> request.getHeaders().get("Authorization"))
          .containsExactly("Bearer token-1", "Bearer token-2");
    }

  }

  @Test
  public void without_retry_a_401_still_drops_the_token_for_the_next_call() {

    try (var context = start()) {
      final var stock = context.getBean(StockClient.class);
      stub.apiAnswers.add(new MockResponse.Builder().code(401).build());

      assertThatThrownBy(() -> stock.getItem("1")).hasMessageContaining("401");
      stock.getItem("1");

      assertThat(stub.tokenRequests).hasSize(2);
    }

  }

  @Test
  public void a_refused_token_request_names_the_keys_to_check() {

    stub.tokenAnswer = number -> Stubs.json(401, """
        {"error": "invalid_client", "error_description": "Bad client secret"}""");
    try (var context = start()) {

      assertThatThrownBy(() -> context.getBean(StockClient.class).getItem("1"))
          .isInstanceOf(OAuth2AuthorizationException.class)
          .hasMessageContaining(
              "refused a token for the client 'stock-service' of the HTTP service group 'stock' with status 401")
          .hasMessageContaining("error 'invalid_client': Bad client secret")
          .hasMessageContaining("Check 'warehouse.stock.oauth.client-id' and 'warehouse.stock.oauth.client-secret'");
    }

  }

  @Test
  public void each_oauth_error_gets_its_own_hint() {

    try (var context = start()) {
      final var stock = context.getBean(StockClient.class);

      stub.tokenAnswer = number -> Stubs.json(400, """
          {"error": "unauthorized_client"}""");
      assertThatThrownBy(() -> stock.getItem("1")).hasMessageContaining("Allow the grant type 'client_credentials'");

      stub.tokenAnswer = number -> Stubs.json(400, """
          {"error": "invalid_request"}""");
      assertThatThrownBy(() -> stock.getItem("1"))
          .hasMessageContaining("Check 'warehouse.stock.oauth.client-authentication-method'");

      stub.tokenAnswer = number -> Stubs.json(400, """
          {"error": "invalid_scope"}""");
      assertThatThrownBy(() -> stock.getItem("1")).hasMessageContaining("Check 'warehouse.stock.oauth.scope'");

      stub.tokenAnswer = number -> Stubs.json(400, """
          {"error": "temporarily_unavailable"}""");
      assertThatThrownBy(() -> stock.getItem("1"))
          .hasMessageContaining("Check 'warehouse.stock.oauth.client-id', 'warehouse.stock.oauth.client-secret'");

      stub.tokenAnswer = number -> new MockResponse.Builder().code(404).body("<html>Not found</html>").build();
      assertThatThrownBy(() -> stock.getItem("1"))
          .hasMessageContaining("status 404")
          .hasMessageContaining("This is no OAuth error. Check that 'warehouse.stock.oauth.token-uri'");
    }

  }

  @Test
  public void an_answer_without_a_token_names_the_token_endpoint_key() {

    stub.tokenAnswer = number -> Stubs.json(200, """
        {"token_type": "Bearer"}""");
    try (var context = start()) {

      assertThatThrownBy(() -> context.getBean(StockClient.class).getItem("1"))
          .hasMessageContaining("answered, but not with a token")
          .hasMessageContaining("Check that 'warehouse.stock.oauth.token-uri' is the token endpoint");
    }

  }

  @Test
  public void a_token_endpoint_nobody_answers_names_its_key_and_the_proxy_keys() {

    try (var context = start("warehouse.stock.oauth.token-uri=http://localhost:1/oauth/token")) {

      assertThatThrownBy(() -> context.getBean(StockClient.class).getItem("1"))
          .hasMessageContaining(
              "'http://localhost:1/oauth/token' of the HTTP service group 'stock' could not be reached")
          .hasMessageContaining(
              "Check 'warehouse.stock.oauth.token-uri' and, if a proxy is needed, the keys 'warehouse.stock.proxy.*'");
    }

  }

}
