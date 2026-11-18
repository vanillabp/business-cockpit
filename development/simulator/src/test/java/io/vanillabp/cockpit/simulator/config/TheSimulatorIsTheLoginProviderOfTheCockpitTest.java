package io.vanillabp.cockpit.simulator.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;

import io.vanillabp.cockpit.devshell.simulator.config.Properties;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The simulator plays the OpenID Connect provider which the module {@code container} logs in at
 * in its profile {@code local}. The test logs a person in the way the cockpit and a browser do
 * together: the browser asks the authorization endpoint, logs in with the form, and brings the
 * code back to the cockpit. The cockpit then trades the code for the ID token. The client, its
 * secret, its address and the users are the ones of {@code config/application.yaml}.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheSimulatorIsTheLoginProviderOfTheCockpitTest {

  private static final String CLIENT_ID = "business-cockpit";
  private static final String CLIENT_SECRET = "business-cockpit-secret";
  private static final String COCKPIT_REDIRECT = "http://localhost:8080/login/oauth2/code/simulator";

  /** A code verifier of PKCE, which the cockpit makes up for every login. */
  private static final String CODE_VERIFIER = "a-code-verifier-which-is-long-enough-for-pkce-0123456789";

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EnableConfigurationProperties(Properties.class)
  @Import(AuthorizationServerConfiguration.class)
  static class TheProviderOnly {
  }

  private ConfigurableApplicationContext simulator;

  private URI baseUri;

  private final CookieManager cookies = new CookieManager();

  private final HttpClient browser = HttpClient
      .newBuilder()
      .cookieHandler(cookies)
      .followRedirects(HttpClient.Redirect.NEVER)
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  @BeforeEach
  void startTheSimulator() {

    simulator = new SpringApplicationBuilder(TheProviderOnly.class)
        .run("--server.port=0", "--spring.main.banner-mode=off");
    baseUri = URI.create("http://localhost:"
        + ((WebServerApplicationContext) simulator).getWebServer().getPort());

  }

  @AfterEach
  void stopTheSimulator() {

    simulator.close();

  }

  @Test
  void aPersonLogsInWithTheFormAndTheCockpitGetsAnIdTokenNamingThePerson() throws Exception {

    final var discovery = get(baseUri.resolve("/.well-known/openid-configuration"), "application/json");
    assertThat(discovery.statusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(JsonPath.<String>read(discovery.body(), "$.issuer")).isEqualTo(baseUri.toString());

    final var code = logIn("test", "test");

    final var tokens = post(baseUri.resolve("/oauth2/token"), Map.of(
        "grant_type", "authorization_code",
        "code", code,
        "redirect_uri", COCKPIT_REDIRECT,
        "code_verifier", CODE_VERIFIER), basicAuthenticationOfTheCockpit());
    assertThat(tokens.statusCode()).isEqualTo(HttpStatus.OK.value());

    final var idToken = SignedJWT.parse(JsonPath.read(tokens.body(), "$.id_token"));
    final var keys = JWKSet.parse(get(baseUri.resolve("/oauth2/jwks"), "application/json").body());
    assertThat(idToken.verify(new RSASSAVerifier(keys.getKeys().get(0).toRSAKey()))).isTrue();
    assertThat(idToken.getJWTClaimsSet().getSubject()).isEqualTo("test");
    assertThat(idToken.getJWTClaimsSet().getAudience()).containsExactly(CLIENT_ID);
    assertThat(idToken.getJWTClaimsSet().getIssuer()).isEqualTo(baseUri.toString());
    assertThat(idToken.getJWTClaimsSet().getStringClaim("nonce")).isEqualTo("a-nonce");

  }

  /**
   * The simulator ends the session of the person once the code is issued. So the next login asks
   * for a user name and a password again, and a developer can log in as somebody else.
   */
  @Test
  void theNextLoginAsksAgain() throws Exception {

    logIn("test", "test");

    final var nextLogin = get(authorizationRequest(), "text/html");
    assertThat(nextLogin.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    assertThat(location(nextLogin).getPath()).isEqualTo(AuthorizationServerConfiguration.LOGIN_PAGE);

  }

  @Test
  void aWrongPasswordGivesNoCode() throws Exception {

    final var toLogin = get(authorizationRequest(), "text/html");
    final var loginPage = get(location(toLogin), "text/html");

    final var login = post(baseUri.resolve(AuthorizationServerConfiguration.LOGIN_PAGE), Map.of(
        "username", "test",
        "password", "wrong",
        "_csrf", csrfTokenOf(loginPage.body())), null);

    assertThat(login.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    assertThat(location(login).getPath()).isEqualTo(AuthorizationServerConfiguration.LOGIN_PAGE);
    assertThat(location(login).getQuery()).isEqualTo("error");

  }

  /**
   * The cockpit sends a browser without a session at the simulator to the authorization endpoint.
   * The simulator sends it on to its login form, and after the login back to the cockpit with a
   * code.
   */
  private String logIn(
      final String user,
      final String password) throws Exception {

    final var toLogin = get(authorizationRequest(), "text/html");
    assertThat(toLogin.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    assertThat(location(toLogin).getPath()).isEqualTo(AuthorizationServerConfiguration.LOGIN_PAGE);
    assertThat(cookies.getCookieStore().getCookies())
        .as("the session cookie must not be the one of the cockpit")
        .anySatisfy(cookie -> assertThat(cookie.getName()).isEqualTo("SIMULATOR_SESSION"))
        .noneSatisfy(cookie -> assertThat(cookie.getName()).isEqualTo("JSESSIONID"));

    final var loginPage = get(location(toLogin), "text/html");
    assertThat(loginPage.statusCode()).isEqualTo(HttpStatus.OK.value());

    final var login = post(baseUri.resolve(AuthorizationServerConfiguration.LOGIN_PAGE), Map.of(
        "username", user,
        "password", password,
        "_csrf", csrfTokenOf(loginPage.body())), null);
    assertThat(login.statusCode()).isEqualTo(HttpStatus.FOUND.value());

    final var backToTheCockpit = get(location(login), "text/html");
    assertThat(backToTheCockpit.statusCode()).isEqualTo(HttpStatus.FOUND.value());
    final var redirect = location(backToTheCockpit);
    assertThat(redirect.toString()).startsWith(COCKPIT_REDIRECT
        + "?");
    final var query = queryOf(redirect);
    assertThat(query).containsEntry("state", "a-state");
    return query.get("code");

  }

  private URI authorizationRequest() throws NoSuchAlgorithmException {

    final var challenge = Base64
        .getUrlEncoder()
        .withoutPadding()
        .encodeToString(MessageDigest
            .getInstance("SHA-256")
            .digest(CODE_VERIFIER.getBytes(StandardCharsets.US_ASCII)));
    final var parameters = new LinkedHashMap<String, String>();
    parameters.put("response_type", "code");
    parameters.put("client_id", CLIENT_ID);
    parameters.put("scope", "openid profile");
    parameters.put("redirect_uri", COCKPIT_REDIRECT);
    parameters.put("state", "a-state");
    parameters.put("nonce", "a-nonce");
    parameters.put("code_challenge", challenge);
    parameters.put("code_challenge_method", "S256");
    return baseUri.resolve("/oauth2/authorize?"
        + formOf(parameters));

  }

  private String basicAuthenticationOfTheCockpit() {

    return "Basic "
        + Base64
            .getEncoder()
            .encodeToString((CLIENT_ID
                + ":"
                + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

  }

  private static String csrfTokenOf(
      final String loginPage) {

    final var matcher = Pattern
        .compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
        .matcher(loginPage);
    assertThat(matcher.find()).as("the login page carries a CSRF token: %s", loginPage).isTrue();
    return matcher.group(1);

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

  private HttpResponse<String> post(
      final URI uri,
      final Map<String, String> form,
      final String authorization) throws IOException, InterruptedException {

    final var request = HttpRequest
        .newBuilder(uri)
        .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(formOf(form)));
    if (authorization != null) {
      request.header(HttpHeaders.AUTHORIZATION, authorization);
    }
    return browser.send(request.build(), HttpResponse.BodyHandlers.ofString());

  }

  private static String formOf(
      final Map<String, String> parameters) {

    return parameters
        .entrySet()
        .stream()
        .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
            + "="
            + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));

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

}
