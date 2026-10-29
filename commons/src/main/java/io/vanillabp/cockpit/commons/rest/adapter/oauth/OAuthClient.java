package io.vanillabp.cockpit.commons.rest.adapter.oauth;

import org.apache.oltu.oauth2.client.HttpClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

/**
 * An OAuth client which sets the content type of a token request to form encoding, unless the
 * request already has one.
 */
public class OAuthClient extends org.apache.oltu.oauth2.client.OAuthClient {

  /**
   * Creates the client.
   *
   * @param oauthClient The HTTP client which sends the token requests
   */
  public OAuthClient(
      final HttpClient oauthClient) {

    super(oauthClient);

  }

  @Override
  public <T extends OAuthAccessTokenResponse> T accessToken(
      final OAuthClientRequest request,
      final String requestMethod,
      final Class<T> responseClass) throws OAuthSystemException, OAuthProblemException {

    var contentType = request.getHeader(OAuth.HeaderType.CONTENT_TYPE);
    if (contentType == null) {
      request.addHeader(OAuth.HeaderType.CONTENT_TYPE, OAuth.ContentType.URL_ENCODED);
    }

    return httpClient.execute(request, request.getHeaders(), requestMethod, responseClass);
  }

}
