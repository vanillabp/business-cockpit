package io.vanillabp.cockpit.support.rest.restclient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/**
 * The bearer token of the OAuth client credentials grant for all REST clients at one prefix. It
 * behaves like its twin in the Spring Boot module:
 * <ul>
 * <li>The token is fetched once and shared by all requests of all clients at the prefix. The grant
 * gives the same token to everybody, so there is no token per user.</li>
 * <li>A token is replaced when a tenth of its lifetime is left, but at most
 * {@code oauth.renew-before} before it expires.</li>
 * <li>A token without {@code expires_in} is used for {@code oauth.lifetime-without-expires-in}.</li>
 * <li>A {@code 401} drops the token, so the next request fetches a new one.</li>
 * <li>Token requests use the timeouts, the TLS settings and the proxy of the clients, see
 * {@link TokenEndpoint}.</li>
 * <li>A refused token request says which key is most likely wrong.</li>
 * </ul>
 * The extension {@code quarkus-oidc-client} could fetch the token as well, but it reads its keys
 * below {@code quarkus.oidc-client}, and a library would have to switch it on in every application.
 * <p>
 * A missing token is fetched without blocking: the filter suspends the request and resumes it when
 * the answer is there. Waiting would block the thread which has to read that answer.
 */
final class ClientCredentials implements ResteasyReactiveClientRequestFilter, ClientResponseFilter {

  /** The request property which remembers the token a request was sent with. */
  private static final String TOKEN_OF_THE_REQUEST = ClientCredentials.class.getName()
      + ".token";

  private final String prefix;

  /** The beginning of the OAuth keys of the prefix, for the messages. */
  private final String keys;

  private final RestClientKeys.OAuth settings;

  private final TokenEndpoint tokenEndpoint;

  private Token current;

  private CompletableFuture<Token> pending;

  /**
   * A token and when it expires.
   *
   * @param value     the token
   * @param issuedAt  when it was asked for
   * @param expiresAt when it expires
   */
  record Token(
               String value,
               Instant issuedAt,
               Instant expiresAt) {

    /**
     * Whether the token is due for renewal: a tenth of its lifetime is left, or {@code renewBefore},
     * whichever is shorter.
     */
    boolean isDue(
        final Instant now,
        final Duration renewBefore) {

      final var tenthOfLifetime = Duration.between(issuedAt, expiresAt).dividedBy(10);
      final var margin = tenthOfLifetime.compareTo(renewBefore) < 0 ? tenthOfLifetime : renewBefore;
      return !now.isBefore(expiresAt.minus(margin));

    }

  }

  ClientCredentials(
      final String prefix,
      final RestClientKeys.OAuth settings,
      final TokenEndpoint tokenEndpoint) {

    this.prefix = prefix;
    this.keys = prefix
        + ".oauth.";
    this.settings = settings;
    this.tokenEndpoint = tokenEndpoint;

  }

  @Override
  public void filter(
      final ResteasyReactiveClientRequestContext request) {

    final var token = tokenWhichIsNotDue();
    if (token != null) {
      authorize(request, token);
      return;
    }
    request.suspend();
    nextToken().whenComplete((
        next,
        failure) -> {
      if (failure != null) {
        request.resume(failure instanceof CompletionException ? failure.getCause() : failure);
        return;
      }
      authorize(request, next);
      request.resume();
    });

  }

  @Override
  public void filter(
      final ClientRequestContext request,
      final ClientResponseContext response) {

    if (response.getStatus() != Response.Status.UNAUTHORIZED.getStatusCode()) {
      return;
    }
    synchronized (this) {
      if (current == request.getProperty(TOKEN_OF_THE_REQUEST)) {
        current = null;
      }
    }

  }

  private static void authorize(
      final ClientRequestContext request,
      final Token token) {

    request.setProperty(TOKEN_OF_THE_REQUEST, token);
    request.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, "Bearer "
        + token.value());

  }

  private synchronized Token tokenWhichIsNotDue() {

    if ((current == null) || current.isDue(Instant.now(), settings.renewBefore())) {
      return null;
    }
    return current;

  }

  /** The token being fetched. Requests which need a token at the same time share one request. */
  private synchronized CompletableFuture<Token> nextToken() {

    if (pending != null) {
      return pending;
    }
    final var next = new CompletableFuture<Token>();
    pending = next;
    fetch().whenComplete((
        token,
        failure) -> {
      synchronized (this) {
        if (token != null) {
          current = token;
        }
        pending = null;
      }
      if (failure != null) {
        next.completeExceptionally(failure instanceof CompletionException ? failure.getCause() : failure);
      } else {
        next.complete(token);
      }
    });
    return next;

  }

  private CompletionStage<Token> fetch() {

    final var form = new Form().param("grant_type", "client_credentials");
    if (!settings.scope().isEmpty()) {
      form.param("scope", String.join(" ", settings.scope()));
    }
    final var clientId = settings.clientId().orElseThrow();
    final var clientSecret = settings.clientSecret().orElseThrow();
    String basicLogin = null;
    if (RestClientKeys.CLIENT_SECRET_BASIC.equals(settings.clientAuthenticationMethod())) {
      // RFC 6749, section 2.3.1: both are form encoded before they are joined
      basicLogin = "Basic "
          + Base64.getEncoder().encodeToString((formEncoded(clientId)
              + ":"
              + formEncoded(clientSecret)).getBytes(StandardCharsets.UTF_8));
    } else {
      form.param("client_id", clientId).param("client_secret", clientSecret);
    }

    final var askedAt = Instant.now();
    final CompletionStage<Response> answer;
    try {
      answer = tokenEndpoint.requestToken(form, basicLogin);
    } catch (RuntimeException notSent) {
      return CompletableFuture.failedFuture(unreachable(notSent));
    }
    return answer.handle((
        response,
        failure) -> {
      if (failure != null) {
        throw unreachable(failure instanceof CompletionException ? failure.getCause() : failure);
      }
      try (response) {
        final var body = response.hasEntity() ? response.readEntity(String.class) : "";
        if (response.getStatus() >= 400) {
          throw refused(response.getStatus(), body);
        }
        return tokenOf(body, askedAt);
      }
    });

  }

  private Token tokenOf(
      final String body,
      final Instant askedAt) {

    final JsonObject answer;
    try {
      answer = new JsonObject(body);
    } catch (DecodeException | ClassCastException e) {
      throw notAToken("the answer is no JSON object");
    }
    final var token = answer.getValue("access_token");
    if (!(token instanceof String value) || value.isBlank()) {
      throw notAToken("it has no 'access_token'");
    }
    final var expiresIn = answer.getValue("expires_in");
    final Duration lifetime;
    if (expiresIn == null) {
      lifetime = settings.lifetimeWithoutExpiresIn();
    } else {
      try {
        lifetime = Duration.ofSeconds(Long.parseLong(expiresIn.toString()));
      } catch (NumberFormatException e) {
        throw notAToken("its 'expires_in' is no number");
      }
    }
    return new Token(value, askedAt, askedAt.plus(lifetime));

  }

  /**
   * The failure is not kept as the cause: Quarkus' REST client would throw a cause of the kind
   * {@code ProcessingException} instead of this explanation.
   */
  private TokenRequestFailed unreachable(
      final Throwable failure) {

    return new TokenRequestFailed("""
        The OAuth token endpoint '%s' of the REST clients at '%s' could not be reached: %s. Check \
        '%stoken-uri' and, if a proxy is needed, the keys '%s.proxy.*'."""
        .formatted(settings.tokenUri().orElseThrow(), prefix, messageOf(failure), keys, prefix));

  }

  private TokenRequestFailed notAToken(
      final String why) {

    return new TokenRequestFailed("""
        The OAuth token endpoint '%s' of the REST clients at '%s' answered, but not with a token: \
        %s. Check that '%stoken-uri' is the token endpoint of the OAuth server."""
        .formatted(settings.tokenUri().orElseThrow(), prefix, why, keys));

  }

  /**
   * Turns an error answer of the token endpoint (RFC 6749, section 5.2) into an exception whose
   * message names the key which is most likely wrong. The texts are those of the Spring Boot
   * module.
   */
  private TokenRequestFailed refused(
      final int status,
      final String body) {

    String code = null;
    String description = null;
    try {
      final var error = new JsonObject(body);
      if ((error.getValue("error") instanceof String errorCode) && !errorCode.isBlank()) {
        code = errorCode;
        description = error.getValue("error_description") instanceof String text ? text : null;
      }
    } catch (DecodeException | ClassCastException notAnOAuthError) {
      // no OAuth error, the hint below says so
    }

    final var answer = """
        The OAuth token endpoint '%s' refused a token for the client '%s' of the REST clients at \
        '%s' with status %d"""
        .formatted(settings.tokenUri().orElseThrow(), settings.clientId().orElseThrow(), prefix, status)
        + (code == null ? "" : ", error '"
            + code
            + "'")
        + (description == null ? "" : ": "
            + description)
        + ". ";
    final var hint = switch (code == null ? "" : code) {
      case "" -> "This is no OAuth error. Check that '%stoken-uri' is the token endpoint of the OAuth server."
          .formatted(keys);
      case "invalid_client" -> """
          Check '%sclient-id' and '%sclient-secret'. Some servers want them in an HTTP basic \
          authentication header and others in the form. If the values are right, change \
          '%sclient-authentication-method'."""
          .formatted(keys, keys, keys);
      case "unauthorized_client", "unsupported_grant_type" -> """
          Allow the grant type 'client_credentials' for this client on the OAuth server, or check \
          that '%sclient-id' names the right client."""
          .formatted(keys);
      case "invalid_request" -> """
          Check '%sclient-authentication-method'. It says whether the client id and secret go into \
          an HTTP basic authentication header or into the form."""
          .formatted(keys);
      case "invalid_scope" -> """
          Check '%sscope', or give the client a default scope on the OAuth server and leave the key out."""
          .formatted(keys);
      default -> "Check '%sclient-id', '%sclient-secret' and '%sclient-authentication-method'."
          .formatted(keys, keys, keys);
    };
    return new TokenRequestFailed(answer + hint);

  }

  private static String formEncoded(
      final String value) {

    return URLEncoder.encode(value, StandardCharsets.UTF_8);

  }

  private static String messageOf(
      final Throwable failure) {

    for (var cause = failure; cause != null; cause = cause.getCause()) {
      if ((cause.getMessage() != null) && !cause.getMessage().isBlank()) {
        return cause.getMessage();
      }
      if (cause.getCause() == cause) {
        break;
      }
    }
    return failure.getClass().getSimpleName();

  }

}
