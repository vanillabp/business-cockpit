package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.testcontainers.mongodb.MongoDBContainer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.vanillabp.cockpit.BusinessCockpitStandaloneApplication;
import io.vanillabp.integration.test.utils.ContainerImages;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A person logs in to the application at an OpenID Connect provider, the default login of the
 * profile {@code local}, and ends up with the cockpit's own JWT cookie.
 *
 * <p>A test server plays the provider. It answers the endpoints the cockpit uses: the discovery
 * document, authorization, token and keys, and its discovery document names the end of a session
 * for the logout. The test walks the redirects of the login one by one, the way a
 * browser does, with a cookie store of its own. The simulator of the development tools plays the
 * same provider for a developer, and its own test shows that it answers like this one.
 *
 * <p>Like {@link ProxyAfterRestartTest}, the test starts the application itself, with its own
 * MongoDB. The suite of {@link ItestBase} runs with the profile {@code basic-auth}, and this test
 * needs the default login without it.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ALoginAtAnOpenIdConnectProviderTest {

  private static final String CLIENT_ID = "business-cockpit";
  private static final String CLIENT_SECRET = "business-cockpit-secret";
  private static final String USER = "martin";
  private static final String CODE = "a-code-for-martin";
  private static final String END_OF_SESSION = "/oauth2/logout";

  private static final MongoDBContainer MONGODB = new MongoDBContainer(ContainerImages.MONGODB)
      .withReplicaSet();

  private static HttpServer usersStub;
  private static HttpServer provider;
  private static RSAKey signingKey;
  private static ConfigurableApplicationContext cockpit;
  private static int port;

  /** The nonce of the last authorization request, which the ID token has to repeat. */
  private static final AtomicReference<String> nonce = new AtomicReference<>();

  /** The last ID token the provider handed out. */
  private static final AtomicReference<String> idToken = new AtomicReference<>();

  /** How the cockpit authenticated at the token endpoint. */
  private static final AtomicReference<String> clientAuthentication = new AtomicReference<>();

  private static final HttpClient HTTP = HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  private final CookieManager cookies = new CookieManager();

  private final HttpClient browser = HttpClient
      .newBuilder()
      .cookieHandler(cookies)
      .followRedirects(HttpClient.Redirect.NEVER)
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  @BeforeAll
  static void startTheCockpitAndItsProvider() throws Exception {

    MONGODB.start();
    signingKey = new RSAKeyGenerator(2048).keyID("provider-key").generate();
    usersStub = startUsersStub();
    provider = startProvider();

    final var providerUri = "http://localhost:"
        + provider.getAddress().getPort();
    cockpit = new SpringApplicationBuilder(BusinessCockpitStandaloneApplication.class)
        .run(
            "--server.port=0",
            "--spring.main.banner-mode=off",
            "--spring.profiles.active=local",
            "--workerId=oidc-login-itest",
            "--spring.mongodb.uri="
                + MONGODB.getReplicaSetUrl("business-cockpit"),
            "--dev-shell-simulator.users-uri=http://localhost:"
                + usersStub.getAddress().getPort()
                + "/dev-shell/user",
            // the discovery document of the issuer names the end of a session, for the logout
            "--spring.security.oauth2.client.provider.simulator.issuer-uri="
                + providerUri,
            "--spring.security.oauth2.client.provider.simulator.authorization-uri="
                + providerUri
                + "/oauth2/authorize",
            "--spring.security.oauth2.client.provider.simulator.token-uri="
                + providerUri
                + "/oauth2/token",
            "--spring.security.oauth2.client.provider.simulator.jwk-set-uri="
                + providerUri
                + "/oauth2/jwks");
    port = ((WebServerApplicationContext) cockpit).getWebServer().getPort();

  }

  @AfterAll
  static void stopEverything() {

    if (cockpit != null) {
      cockpit.close();
    }
    provider.stop(0);
    usersStub.stop(0);
    MONGODB.stop();

  }

  @Test
  void aPersonLogsInAtTheProviderAndGetsTheCookieOfTheCockpit() throws Exception {

    // a page without a login goes to the login of the provider
    final var page = get(cockpitUri("/tasks"), "text/html");
    assertThat(page.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    final var startOfLogin = location(page);
    assertThat(startOfLogin.getPath()).isEqualTo("/oauth2/authorization/simulator");

    final var toProvider = get(startOfLogin, "text/html");
    assertThat(toProvider.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    final var authorization = location(toProvider);
    assertThat(authorization.getPort()).isEqualTo(provider.getAddress().getPort());
    assertThat(queryOf(authorization))
        .containsEntry("client_id", CLIENT_ID)
        .containsEntry("redirect_uri", cockpitUri("/login/oauth2/code/simulator").toString())
        .containsEntry("code_challenge_method", "S256")
        .containsKey("code_challenge");

    // the provider logs the person in and sends the browser back with a code
    final var backFromProvider = get(authorization, "text/html");
    assertThat(backFromProvider.statusCode()).isEqualTo(HttpStatus.FOUND.value());

    // the cockpit trades the code for the tokens of the provider and hands out its own cookie
    final var loggedIn = get(location(backFromProvider), "text/html");
    assertThat(loggedIn.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    assertThat(location(loggedIn).getPath()).isEqualTo("/tasks");
    assertThat(clientAuthentication.get())
        .isEqualTo("Basic "
            + Base64.getEncoder().encodeToString(
                (CLIENT_ID
                    + ":"
                    + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8)));
    final var loginCookie = cookies
        .getCookieStore()
        .getCookies()
        .stream()
        .filter(cookie -> cookie.getName().equals("bc"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("the cockpit handed out no cookie 'bc'"));

    // the cookie alone is the login of every following request. The session of the login plays
    // no part, so this request carries nothing else.
    final var currentUser = HTTP.send(HttpRequest
        .newBuilder(cockpitUri("/gui/api/v1/app/current-user"))
        .header(HttpHeaders.COOKIE, "bc="
            + loginCookie.getValue())
        .GET()
        .build(), HttpResponse.BodyHandlers.ofString());
    assertThat(currentUser.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(currentUser.body()).contains("\""
        + USER
        + "\"");
    assertThat(get(cockpitUri("/gui/api/v1/workflow-module"), "application/json").statusCode())
        .isEqualTo(HttpStatus.OK.value());

    // a logout removes both cookies and ends the session at the provider as well. Without that,
    // the provider would log the person in again on the next request, without a question
    final var logout = get(cockpitUri("/logout"), "text/html");
    assertThat(logout.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    final var endOfSession = location(logout);
    assertThat(endOfSession.getPort()).isEqualTo(provider.getAddress().getPort());
    assertThat(endOfSession.getPath()).isEqualTo(END_OF_SESSION);
    assertThat(queryOf(endOfSession))
        .containsEntry("id_token_hint", idToken.get())
        .containsEntry("client_id", CLIENT_ID)
        .containsEntry("post_logout_redirect_uri", cockpitUri("/").toString());
    assertThat(cookies.getCookieStore().getCookies())
        .noneMatch(cookie -> cookie.getName().equals("bc"))
        .noneMatch(cookie -> cookie.getName().equals("bc-id-token"));
    assertThat(get(cockpitUri("/gui/api/v1/workflow-module"), "application/json").statusCode())
        .isEqualTo(HttpStatus.UNAUTHORIZED.value());

  }

  /**
   * A program gets 401 and not the redirect to the provider. A {@code fetch} of a user interface
   * would follow the redirect and fail at the provider.
   */
  @Test
  void theGuiApiAnswers401WithoutALogin() throws Exception {

    final var response = get(cockpitUri("/gui/api/v1/workflow-module"), "application/json");

    assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(response.headers().firstValue(HttpHeaders.LOCATION)).isEmpty();

  }

  private HttpResponse<String> get(
      final URI uri,
      final String accept) throws IOException, InterruptedException {

    return browser.send(HttpRequest
        .newBuilder(uri)
        .header(HttpHeaders.ACCEPT, accept)
        .GET()
        .build(), HttpResponse.BodyHandlers.ofString());

  }

  private static URI cockpitUri(
      final String path) {

    return URI.create("http://localhost:"
        + port
        + path);

  }

  private static URI location(
      final HttpResponse<?> response) {

    final var location = response
        .headers()
        .firstValue(HttpHeaders.LOCATION)
        .orElseThrow(() -> new AssertionError("no Location header in "
            + response));
    return response.uri().resolve(location);

  }

  private static Map<String, String> queryOf(
      final URI uri) {

    return Arrays
        .stream(uri.getRawQuery().split("&"))
        .map(parameter -> parameter.split("=", 2))
        .collect(Collectors.toMap(
            parameter -> URLDecoder.decode(parameter[0], StandardCharsets.UTF_8),
            parameter -> parameter.length > 1
                ? URLDecoder.decode(parameter[1], StandardCharsets.UTF_8)
                : ""));

  }

  private static HttpServer startUsersStub() throws IOException {

    final var server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/dev-shell/user/all", exchange -> answer(exchange, HttpStatus.OK,
        "application/json", """
            [
              { "id": "martin", "email": "martin@example.com", "firstName": "Martin",
                "lastName": "Meier", "groups": [ "accounting", "bc-users" ] }
            ]
            """));
    server.start();
    return server;

  }

  /**
   * The provider. It logs every person in as {@value #USER} without asking, because the login
   * form is the provider's business and not the cockpit's.
   */
  private static HttpServer startProvider() throws IOException {

    final var server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/oauth2/authorize", exchange -> {
      final var query = queryOf(exchange.getRequestURI());
      nonce.set(query.get("nonce"));
      final var redirect = query.get("redirect_uri")
          + "?code="
          + CODE
          + "&state="
          + URLEncoder.encode(query.get("state"), StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add(HttpHeaders.LOCATION, redirect);
      exchange.sendResponseHeaders(HttpStatus.FOUND.value(), -1);
      exchange.close();
    });
    server.createContext("/oauth2/token", exchange -> {
      clientAuthentication.set(exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
      final var form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      if (!form.contains("code="
          + CODE) || !form.contains("code_verifier=")) {
        answer(exchange, HttpStatus.BAD_REQUEST, "application/json",
            "{ \"error\": \"invalid_grant\" }");
        return;
      }
      answer(exchange, HttpStatus.OK, "application/json", """
          { "access_token": "an-access-token", "token_type": "Bearer", "expires_in": 300,
            "scope": "openid profile", "id_token": "%s" }
          """.formatted(idToken(exchange)));
    });
    server.createContext("/.well-known/openid-configuration", exchange -> {
      final var issuer = "http://localhost:"
          + exchange.getLocalAddress().getPort();
      answer(exchange, HttpStatus.OK, "application/json", """
          { "issuer": "%1$s",
            "authorization_endpoint": "%1$s/oauth2/authorize",
            "token_endpoint": "%1$s/oauth2/token",
            "jwks_uri": "%1$s/oauth2/jwks",
            "end_session_endpoint": "%1$s%2$s",
            "response_types_supported": [ "code" ],
            "subject_types_supported": [ "public" ],
            "id_token_signing_alg_values_supported": [ "RS256" ] }
          """.formatted(issuer, END_OF_SESSION));
    });
    server.createContext("/oauth2/jwks", exchange -> answer(exchange, HttpStatus.OK,
        "application/json", new JWKSet(signingKey.toPublicJWK()).toString()));
    server.start();
    return server;

  }

  private static String idToken(
      final HttpExchange exchange) {

    final var now = Instant.now();
    final var claims = new JWTClaimsSet.Builder()
        .issuer("http://localhost:"
            + exchange.getLocalAddress().getPort())
        .subject(USER)
        .audience(CLIENT_ID)
        .issueTime(Date.from(now))
        .expirationTime(Date.from(now.plusSeconds(300)))
        .claim("nonce", nonce.get())
        .build();
    try {
      final var token = new SignedJWT(
          new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
      token.sign(new RSASSASigner(signingKey));
      idToken.set(token.serialize());
      return idToken.get();
    } catch (JOSEException e) {
      throw new IllegalStateException("Could not sign the ID token", e);
    }

  }

  private static void answer(
      final HttpExchange exchange,
      final HttpStatus status,
      final String contentType,
      final String body) throws IOException {

    final var bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
    exchange.sendResponseHeaders(status.value(), bytes.length);
    try (var out = exchange.getResponseBody()) {
      out.write(bytes);
    }

  }

}
