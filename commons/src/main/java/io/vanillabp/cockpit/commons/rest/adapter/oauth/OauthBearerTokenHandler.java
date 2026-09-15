package io.vanillabp.cockpit.commons.rest.adapter.oauth;

import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest.TokenRequestBuilder;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import io.vanillabp.cockpit.commons.rest.adapter.Authentication;
import io.vanillabp.cockpit.commons.rest.adapter.bearer.BearerTokenBasedAuthInterceptor;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;

/**
 * Adds a bearer token to the &quot;Authorization&quot; header.
 * <p>
 * An expired token is renewed with the refresh token. Where that fails, and where there is no
 * token yet, a new one is fetched.
 * <p>
 * Each thread holds its own token, so two threads fetching or renewing at the same time do not
 * get in each other's way.
 */
public class OauthBearerTokenHandler extends BearerTokenBasedAuthInterceptor {
    
    private final OAuthClient oauthClient;

    private final TokenRequestBuilder tokenRequestBuilder;

    private final String basicHeader;

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

        try {

            final OAuthClientRequest request;
            if (basicHeader != null) {
                request = this.tokenRequestBuilder.buildQueryMessage();
                request.setBody("");
                request.addHeader("Authorization", basicHeader);
            } else {
                request = this.tokenRequestBuilder.buildBodyMessage();
            }
            final var tokenResponse = oauthClient.accessToken(
                    request,
                    OAuthJSONAccessTokenResponse.class);
            return tokenResponse.getAccessToken();

        } catch (Exception e) {
            throw new RuntimeException("Could not create token!", e);
        }

    }
    
}
