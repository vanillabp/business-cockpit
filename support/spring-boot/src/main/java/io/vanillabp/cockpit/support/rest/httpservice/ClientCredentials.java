package io.vanillabp.cockpit.support.rest.httpservice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.DefaultMapOAuth2AccessTokenResponseConverter;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * The bearer token of the OAuth client credentials grant for one HTTP service group, built on
 * Spring Security. Spring Security fetches the token, keeps it and adds it to each request. This
 * class sets what its defaults get wrong for a client of a service:
 * <ul>
 * <li>The registration comes from the group's keys, not from
 * {@code spring.security.oauth2.client.registration.*}, so all keys of a group live below one
 * prefix.</li>
 * <li>The principal is fixed. By default it is the logged-in user, which would fetch one token per
 * user although the grant gives the same token to everybody.</li>
 * <li>A {@code 401} drops the token, so the next request fetches a new one.</li>
 * <li>A token is replaced when a tenth of its lifetime is left, but at most
 * {@code oauth.renew-before} before it expires. Spring Security uses a fixed time.</li>
 * <li>A token without {@code expires_in} is used for {@code oauth.lifetime-without-expires-in}.
 * Spring Security would treat it as expiring after one second and fetch one for every
 * request.</li>
 * <li>Token requests use the group's request factory, so its proxy and TLS settings.</li>
 * <li>A refused token request says which key is most likely wrong.</li>
 * </ul>
 * This class is loaded only for a group with OAuth, so an application without Spring Security's
 * OAuth client does not need it.
 */
final class ClientCredentials {

  private static final String INVALID_TOKEN_RESPONSE = "invalid_token_response";

  private final String groupName;

  private final String prefix;

  /** The beginning of the OAuth keys of the group, for the messages. */
  private final String keys;

  private final HttpServiceGroupProperties.OAuth settings;

  private ClientCredentials(
      final String groupName,
      final String prefix,
      final HttpServiceGroupProperties.OAuth settings) {

    this.groupName = groupName;
    this.prefix = prefix;
    this.keys = prefix
        + ".oauth.";
    this.settings = settings;

  }

  /**
   * The interceptor which adds the token to each request of the group.
   *
   * @param requestFactory the group's own, so token requests use its proxy and TLS settings
   * @param proxyLogin     the login for the group's proxy, or {@code null}
   */
  static ClientHttpRequestInterceptor interceptor(
      final String groupName,
      final String prefix,
      final HttpServiceGroupProperties.OAuth settings,
      final ClientHttpRequestFactory requestFactory,
      final @Nullable ClientHttpRequestInterceptor proxyLogin) {

    return new ClientCredentials(groupName, prefix, settings)
        .buildInterceptor(requestFactory, proxyLogin);

  }

  private ClientHttpRequestInterceptor buildInterceptor(
      final ClientHttpRequestFactory requestFactory,
      final @Nullable ClientHttpRequestInterceptor proxyLogin) {

    final var registration = ClientRegistration.withRegistrationId(groupName)
        .clientId(settings.getClientId())
        .clientSecret(settings.getClientSecret())
        .clientAuthenticationMethod(new ClientAuthenticationMethod(settings.getClientAuthenticationMethod()))
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .tokenUri(settings.getTokenUri())
        .scope(settings.getScope())
        .build();
    final var registrations = new InMemoryClientRegistrationRepository(registration);
    final var tokens = new InMemoryOAuth2AuthorizedClientService(registrations);

    final var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, tokens);
    manager.setAuthorizedClientProvider(new Provider(tokenResponseClient(requestFactory, proxyLogin)));

    final var principal = new AnonymousAuthenticationToken(groupName, "http-service-group-"
        + groupName, AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    final var interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
    interceptor.setClientRegistrationIdResolver(request -> groupName);
    interceptor.setPrincipalResolver(request -> principal);
    interceptor.setAuthorizationFailureHandler(OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(tokens));
    return interceptor;

  }

  private RestClientClientCredentialsTokenResponseClient tokenResponseClient(
      final ClientHttpRequestFactory requestFactory,
      final @Nullable ClientHttpRequestInterceptor proxyLogin) {

    final var tokenConverter = new OAuth2AccessTokenResponseHttpMessageConverter();
    final var defaultConverter = new DefaultMapOAuth2AccessTokenResponseConverter();
    tokenConverter.setAccessTokenResponseConverter(parameters -> defaultConverter.convert(withLifetime(parameters)));

    final var restClient = RestClient.builder()
        .requestFactory(requestFactory)
        .configureMessageConverters(converters -> {
          converters.addCustomConverter(new FormHttpMessageConverter());
          converters.addCustomConverter(tokenConverter);
        })
        .defaultStatusHandler(HttpStatusCode::isError, (
            request,
            response) -> {
          throw refused(response);
        });
    if (proxyLogin != null) {
      restClient.requestInterceptor(proxyLogin);
    }
    final var client = new RestClientClientCredentialsTokenResponseClient();
    client.setRestClient(restClient.build());
    return client;

  }

  private Map<String, Object> withLifetime(
      final Map<String, Object> parameters) {

    if (parameters.containsKey(OAuth2ParameterNames.EXPIRES_IN)) {
      return parameters;
    }
    final var completed = new HashMap<>(parameters);
    completed.put(OAuth2ParameterNames.EXPIRES_IN, settings.getLifetimeWithoutExpiresIn().toSeconds());
    return completed;

  }

  /**
   * Whether a token is due for renewal: a tenth of its lifetime is left, or {@code renewBefore},
   * whichever is shorter.
   */
  static boolean isDue(
      final OAuth2AccessToken token,
      final Instant now,
      final Duration renewBefore) {

    final var expiresAt = token.getExpiresAt();
    final var issuedAt = token.getIssuedAt();
    if ((expiresAt == null) || (issuedAt == null)) {
      return false;
    }
    final var tenthOfLifetime = Duration.between(issuedAt, expiresAt).dividedBy(10);
    final var margin = tenthOfLifetime.compareTo(renewBefore) < 0 ? tenthOfLifetime : renewBefore;
    return !now.isBefore(expiresAt.minus(margin));

  }

  /** Fetches a token if there is none or if it is due for renewal. */
  private class Provider implements OAuth2AuthorizedClientProvider {

    private final RestClientClientCredentialsTokenResponseClient tokenResponseClient;

    Provider(
        final RestClientClientCredentialsTokenResponseClient tokenResponseClient) {
      this.tokenResponseClient = tokenResponseClient;
    }

    @Override
    public @Nullable OAuth2AuthorizedClient authorize(
        final OAuth2AuthorizationContext context) {

      final var current = context.getAuthorizedClient();
      if ((current != null) && !isDue(current.getAccessToken(), Instant.now(), settings.getRenewBefore())) {
        return null;
      }
      final var registration = context.getClientRegistration();
      try {
        final var response = tokenResponseClient
            .getTokenResponse(new OAuth2ClientCredentialsGrantRequest(registration));
        return new OAuth2AuthorizedClient(registration, context.getPrincipal().getName(), response.getAccessToken());
      } catch (TokenRequestRefused refused) {
        throw refused;
      } catch (OAuth2AuthorizationException e) {
        throw explained(e);
      }

    }

  }

  /** The token endpoint could not be reached, or its answer was no token. */
  private OAuth2AuthorizationException explained(
      final OAuth2AuthorizationException e) {

    final String message;
    if (e.getCause() instanceof ResourceAccessException unreachable) {
      message = """
          The OAuth token endpoint '%s' of the HTTP service group '%s' could not be reached: %s. \
          Check '%stoken-uri' and, if a proxy is needed, the keys '%s.proxy.*'."""
          .formatted(settings.getTokenUri(), groupName, unreachable.getMessage(), keys, prefix);
    } else {
      message = """
          The OAuth token endpoint '%s' of the HTTP service group '%s' answered, but not with a \
          token: %s. Check that '%stoken-uri' is the token endpoint of the OAuth server."""
          .formatted(settings.getTokenUri(), groupName, e.getError().getDescription(), keys);
    }
    return new OAuth2AuthorizationException(new OAuth2Error(INVALID_TOKEN_RESPONSE, message, null), message, e);

  }

  /**
   * Turns an error answer of the token endpoint (RFC 6749, section 5.2) into an exception whose
   * message names the key which is most likely wrong.
   */
  private TokenRequestRefused refused(
      final ClientHttpResponse response) throws IOException {

    final var status = response.getStatusCode().value();
    final var body = response.getBody().readAllBytes();
    final var error = oauthErrorOf(body);
    final var code = error == null ? null : error.getErrorCode();
    final var description = error == null ? null : error.getDescription();

    final var answer = """
        The OAuth token endpoint '%s' refused a token for the client '%s' of the HTTP service \
        group '%s' with status %d"""
        .formatted(settings.getTokenUri(), settings.getClientId(), groupName, status)
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
    final var message = answer + hint;
    return new TokenRequestRefused(new OAuth2Error(code == null ? INVALID_TOKEN_RESPONSE
        : code, message, null), message);

  }

  /** The OAuth error in an answer's body, or {@code null} if the body holds none. */
  private static @Nullable OAuth2Error oauthErrorOf(
      final byte[] body) {

    try {
      final var error = new OAuth2ErrorHttpMessageConverter().read(OAuth2Error.class, new HttpInputMessage() {

        @Override
        public InputStream getBody() {
          return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
          final var headers = new HttpHeaders();
          headers.setContentType(MediaType.APPLICATION_JSON);
          return headers;
        }

      });
      return (error.getErrorCode() == null) || error.getErrorCode().isBlank() ? null : error;
    } catch (Exception notAnOAuthError) {
      return null;
    }

  }

  /** A refusal which is already explained. */
  private static class TokenRequestRefused extends OAuth2AuthorizationException {

    private static final long serialVersionUID = 1L;

    TokenRequestRefused(
        final OAuth2Error error,
        final String message) {
      super(error, message);
    }

  }

}
