package io.vanillabp.cockpit.commons.rest.adapter;

/**
 * The settings to fetch an OAuth token with the client credentials grant. The base URL is the
 * token endpoint. The other client settings apply to the requests sent to that endpoint.
 */
public class OauthAuthentication extends Client {

  private String clientId;

  private String clientSecret;

  private boolean basic;

  /**
   * Creates the settings with their defaults. OAuth stays off until a base URL is set.
   */
  public OauthAuthentication() {
  }

  /**
   * The id of the OAuth client.
   *
   * @return The client id
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * Sets the id of the OAuth client.
   *
   * @param clientId The client id
   */
  public void setClientId(
      String clientId) {
    this.clientId = clientId;
  }

  /**
   * The secret of the OAuth client.
   *
   * @return The client secret
   */
  public String getClientSecret() {
    return clientSecret;
  }

  /**
   * Sets the secret of the OAuth client.
   *
   * @param clientSecret The client secret
   */
  public void setClientSecret(
      String clientSecret) {
    this.clientSecret = clientSecret;
  }

  /**
   * Tells how the client id and secret reach the token endpoint: as an HTTP basic authentication
   * header, or as fields in the request body.
   *
   * @return {@code true} for the header, {@code false} for the body
   */
  public boolean isBasic() {
    return basic;
  }

  /**
   * Sets how the client id and secret reach the token endpoint: as an HTTP basic authentication
   * header, or as fields in the request body.
   *
   * @param basic {@code true} for the header, {@code false} for the body
   */
  public void setBasic(
      boolean basic) {
    this.basic = basic;
  }

}
