package io.vanillabp.cockpit.support.rest.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cockpit.support.rest.restclient.inventory.InventoryApi;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.ws.rs.WebApplicationException;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Credentials;

/**
 * {@code oauth.*}: a bearer token of the client credentials grant, fetched once and shared by all
 * requests of the clients at a prefix, renewed when a tenth of its lifetime is left, dropped after
 * a {@code 401}, and a refused token request names the keys to check. The same behaviour as the
 * Spring Boot module.
 */
@ExtendWith(SuppressOutputExtension.class)
public class OAuthTest {

  private static final String STUB = "oauth";

  private static final String[] PREFIXES = {
      "stock", "stock-basic", "stock-short-lifetime", "stock-retry", "stock-unreachable"
  };

  @RegisterExtension
  static final QuarkusExtensionTest application = withKeysOfAllPrefixes(new QuarkusExtensionTest()
      .withApplicationRoot(TheRestClientsOnQuarkus::withTheRestClients)
      .overrideConfigKey("stock-basic.oauth.client-authentication-method", "client_secret_basic")
      .overrideConfigKey("stock-basic.oauth.scope", "stock.read,stock.write")
      .overrideConfigKey("stock-short-lifetime.oauth.lifetime-without-expires-in", "2s")
      .overrideConfigKey("stock-retry.retry.enabled", "true")
      .overrideConfigKey("stock-unreachable.oauth.token-uri", "http://127.0.0.1:1/oauth/token"));

  private static QuarkusExtensionTest withKeysOfAllPrefixes(
      final QuarkusExtensionTest application) {

    for (final var prefix : PREFIXES) {
      application
          .overrideConfigKey(prefix
              + ".base-url", Stubs.url(STUB, "/api"))
          .overrideConfigKey(prefix
              + ".oauth.client-id", "stock-service")
          .overrideConfigKey(prefix
              + ".oauth.client-secret", "s3cret");
      if (!prefix.equals("stock-unreachable")) {
        application.overrideConfigKey(prefix
            + ".oauth.token-uri", Stubs.url(STUB, "/oauth/token"));
      }
    }
    return application;

  }

  /** Answers the token endpoint and the API, and records both. */
  static class Stub extends Dispatcher {

    final AtomicInteger issued = new AtomicInteger();

    final List<RecordedRequest> tokenRequests = new CopyOnWriteArrayList<>();

    final List<RecordedRequest> apiRequests = new CopyOnWriteArrayList<>();

    final Queue<MockResponse> apiAnswers = new ConcurrentLinkedQueue<>();

    volatile IntFunction<MockResponse> tokenAnswer = number -> token(number, 3600);

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

  @AfterAll
  static void stop() {
    server.close();
  }

  @BeforeEach
  void reset() {

    server.setDispatcher(stub);
    Stubs.started(server, STUB);
    stub.issued.set(0);
    stub.tokenRequests.clear();
    stub.apiRequests.clear();
    stub.apiAnswers.clear();
    stub.tokenAnswer = number -> token(number, 3600);

  }

  private static InventoryApi client(
      final String prefix) {
    return TheRestClientsOnQuarkus.fresh().build(InventoryApi.class, prefix);
  }

  @Test
  public void one_token_serves_all_requests_of_all_clients_at_a_prefix() {

    final var restClients = TheRestClientsOnQuarkus.fresh();
    final var stock = restClients.build(InventoryApi.class, "stock");
    final var anotherClientAtThePrefix = restClients.build(InventoryApi.class, "stock");

    stock.getItem("1");
    stock.getItem("2");
    anotherClientAtThePrefix.getItem("3");

    assertThat(stub.tokenRequests).hasSize(1);
    assertThat(stub.tokenRequests.getFirst().getBody().utf8())
        .contains("grant_type=client_credentials", "client_id=stock-service", "client_secret=s3cret");
    assertThat(stub.tokenRequests.getFirst().getHeaders().get("Authorization")).isNull();
    assertThat(stub.apiRequests)
        .extracting(request -> request.getHeaders().get("Authorization"))
        .containsOnly("Bearer token-1");

  }

  @Test
  public void client_id_and_secret_can_go_into_a_basic_login_header() {

    client("stock-basic").getItem("1");

    final var tokenRequest = stub.tokenRequests.getFirst();
    assertThat(tokenRequest.getHeaders().get("Authorization"))
        .isEqualTo(Credentials.basic("stock-service", "s3cret"));
    assertThat(tokenRequest.getBody().utf8())
        .doesNotContain("client_secret")
        .contains("scope=stock.read+stock.write");

  }

  @Test
  public void a_token_is_replaced_when_a_tenth_of_its_lifetime_is_left() throws InterruptedException {

    // lives one second, so it is replaced after 900 milliseconds, not 30 seconds before it expires
    stub.tokenAnswer = number -> token(number, 1);
    final var stock = client("stock");

    stock.getItem("1");
    stock.getItem("2");
    Thread.sleep(1000);
    stock.getItem("3");

    assertThat(stub.tokenRequests).hasSize(2);
    assertThat(stub.apiRequests.getLast().getHeaders().get("Authorization")).isEqualTo("Bearer token-2");

  }

  @Test
  public void a_token_without_expires_in_is_kept_for_the_configured_lifetime() throws InterruptedException {

    stub.tokenAnswer = OAuthTest::tokenWithoutExpiresIn;
    final var stock = client("stock-short-lifetime");

    stock.getItem("1");
    stock.getItem("2");
    assertThat(stub.tokenRequests).hasSize(1);

    Thread.sleep(2000);
    stock.getItem("3");
    assertThat(stub.tokenRequests).hasSize(2);

  }

  @Test
  public void a_token_without_expires_in_is_kept_for_an_hour_by_default() {

    stub.tokenAnswer = OAuthTest::tokenWithoutExpiresIn;
    final var stock = client("stock");

    stock.getItem("1");
    stock.getItem("2");

    assertThat(stub.tokenRequests).hasSize(1);

  }

  @Test
  public void after_a_401_the_token_is_dropped_and_the_retry_brings_a_new_one() {

    stub.apiAnswers.add(new MockResponse.Builder().code(401).build());

    assertThat(client("stock-retry").getItem("1").getId()).isEqualTo("1");

    assertThat(stub.tokenRequests).hasSize(2);
    assertThat(stub.apiRequests)
        .extracting(request -> request.getHeaders().get("Authorization"))
        .containsExactly("Bearer token-1", "Bearer token-2");

  }

  @Test
  public void without_retry_a_401_still_drops_the_token_for_the_next_call() {

    final var stock = client("stock");
    stub.apiAnswers.add(new MockResponse.Builder().code(401).build());

    assertThatThrownBy(() -> stock.getItem("1")).isInstanceOf(WebApplicationException.class);
    stock.getItem("1");

    assertThat(stub.tokenRequests).hasSize(2);

  }

  @Test
  public void a_refused_token_request_names_the_keys_to_check_and_is_not_retried() {

    stub.tokenAnswer = number -> Stubs.json(401, """
        {"error": "invalid_client", "error_description": "Bad client secret"}""");

    assertThatThrownBy(() -> client("stock-retry").getItem("1"))
        .hasStackTraceContaining(
            "refused a token for the client 'stock-service' of the REST clients at 'stock-retry' with status 401")
        .hasStackTraceContaining("error 'invalid_client': Bad client secret")
        .hasStackTraceContaining("Check 'stock-retry.oauth.client-id' and 'stock-retry.oauth.client-secret'");
    assertThat(stub.tokenRequests).hasSize(1);
    assertThat(stub.apiRequests).isEmpty();

  }

  @Test
  public void each_oauth_error_gets_its_own_hint() {

    stub.tokenAnswer = number -> Stubs.json(400, """
        {"error": "unauthorized_client"}""");
    assertThatThrownBy(() -> client("stock").getItem("1"))
        .hasStackTraceContaining("Allow the grant type 'client_credentials'");

    stub.tokenAnswer = number -> Stubs.json(400, """
        {"error": "invalid_request"}""");
    assertThatThrownBy(() -> client("stock").getItem("1"))
        .hasStackTraceContaining("Check 'stock.oauth.client-authentication-method'");

    stub.tokenAnswer = number -> Stubs.json(400, """
        {"error": "invalid_scope"}""");
    assertThatThrownBy(() -> client("stock").getItem("1")).hasStackTraceContaining("Check 'stock.oauth.scope'");

    stub.tokenAnswer = number -> Stubs.json(400, """
        {"error": "temporarily_unavailable"}""");
    assertThatThrownBy(() -> client("stock").getItem("1"))
        .hasStackTraceContaining("Check 'stock.oauth.client-id', 'stock.oauth.client-secret'");

    stub.tokenAnswer = number -> new MockResponse.Builder().code(404).body("<html>Not found</html>").build();
    assertThatThrownBy(() -> client("stock").getItem("1"))
        .hasStackTraceContaining("status 404")
        .hasStackTraceContaining("This is no OAuth error. Check that 'stock.oauth.token-uri'");

  }

  @Test
  public void an_answer_without_a_token_names_the_token_endpoint_key() {

    stub.tokenAnswer = number -> Stubs.json(200, """
        {"token_type": "Bearer"}""");
    assertThatThrownBy(() -> client("stock").getItem("1"))
        .hasStackTraceContaining("answered, but not with a token: it has no 'access_token'")
        .hasStackTraceContaining("Check that 'stock.oauth.token-uri' is the token endpoint");

    stub.tokenAnswer = number -> Stubs.json(200, "[]");
    assertThatThrownBy(() -> client("stock").getItem("1"))
        .hasStackTraceContaining("the answer is no JSON object");

    stub.tokenAnswer = number -> Stubs.json(200, """
        {"access_token": "token", "expires_in": "soon"}""");
    assertThatThrownBy(() -> client("stock").getItem("1"))
        .hasStackTraceContaining("its 'expires_in' is no number");

  }

  @Test
  public void a_token_endpoint_nobody_answers_names_its_key_and_the_proxy_keys() {

    assertThatThrownBy(() -> client("stock-unreachable").getItem("1"))
        .hasStackTraceContaining(
            "'http://127.0.0.1:1/oauth/token' of the REST clients at 'stock-unreachable' could not be reached")
        .hasStackTraceContaining(
            "Check 'stock-unreachable.oauth.token-uri' and, if a proxy is needed, the keys 'stock-unreachable.proxy.*'");

  }

}
