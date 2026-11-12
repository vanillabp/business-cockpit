package io.vanillabp.cockpit.support.rest.adapter.oauth;

import java.time.Duration;
import java.time.Instant;

import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest.TokenRequestBuilder;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;

import io.vanillabp.cockpit.support.rest.adapter.Authentication;
import io.vanillabp.cockpit.support.rest.adapter.bearer.BearerTokenBasedAuthInterceptor;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;

/**
 * Adds a bearer token to the &quot;Authorization&quot; header. The token comes from the token
 * endpoint of an OAuth server, by the client credentials grant.
 * <p>
 * The token is used until shortly before the time the server gives in {@code expires_in}. Then a
 * new one is fetched. A token without {@code expires_in} is used until a request is answered with
 * 401. See {@code OauthBearerTokenHandlerTest}.
 */
public class OauthBearerTokenHandler extends BearerTokenBasedAuthInterceptor {

  private final OAuthClient oauthClient;

  private final TokenRequestBuilder tokenRequestBuilder;

  private final String basicHeader;

  /**
   * Creates the handler. The requests to the token endpoint use the client settings found in
   * {@code authenticationProperties.getOauth()}.
   *
   * @param authenticationProperties The authentication settings of the REST client
   */
  public OauthBearerTokenHandler(
      final Authentication authenticationProperties) {

    final var properties = authenticationProperties.getOauth();

    // apply common configuration (proxy, logging, etc.) to OAuth client
    final var httpClientBuilder = new OkHttpClient.Builder();
    super.configureOkHttpClient(this.getClass(), properties, httpClientBuilder, null);
    final var oauthHttpClient = new OAuthOkHttpClient(httpClientBuilder.build());

    oauthClient = new OAuthClient(oauthHttpClient);
    tokenRequestBuilder = OAuthClientRequest
        .tokenLocation(properties.getBaseUrl())
        .setGrantType(GrantType.CLIENT_CREDENTIALS);
    if (properties.isBasic()) {
      basicHeader = Credentials.basic(
          properties.getClientId(),
          properties.getClientSecret());
    } else {
      basicHeader = null;
      tokenRequestBuilder
          .setClientId(properties.getClientId())
          .setClientSecret(properties.getClientSecret());
    }

  }

  @Override
  protected String createToken() {

    return createBearerToken().value();

  }

  @Override
  protected BearerToken createBearerToken() {

    try {

      final OAuthClientRequest request;
      if (basicHeader != null) {
        request = this.tokenRequestBuilder.buildQueryMessage();
        request.setBody("");
        request.addHeader("Authorization", basicHeader);
      } else {
        request = this.tokenRequestBuilder.buildBodyMessage();
      }
      final var fetchedAt = Instant.now();
      final var tokenResponse = oauthClient.accessToken(
          request,
          OAuthJSONAccessTokenResponse.class);
      final var expiresIn = tokenResponse.getExpiresIn();
      return expiresIn == null
          ? new BearerToken(tokenResponse.getAccessToken(), null)
          : BearerToken.expiringAfter(
              tokenResponse.getAccessToken(), fetchedAt, Duration.ofSeconds(expiresIn));

    } catch (Exception e) {
      throw new RuntimeException("Could not create token!", e);
    }

  }

}
