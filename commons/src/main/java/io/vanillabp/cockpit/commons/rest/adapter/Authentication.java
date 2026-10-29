package io.vanillabp.cockpit.commons.rest.adapter;

/**
 * How a REST client proves who it is to the server: HTTP basic authentication with user name and
 * password, or an OAuth token. Basic authentication wins if both are configured.
 */
public class Authentication {

  private boolean basic = false;

  private OauthAuthentication oauth;

  private String username;

  private String password;

  /**
   * Creates empty settings, which means no authentication.
   */
  public Authentication() {
  }

  /**
   * The user name for HTTP basic authentication.
   *
   * @return The user name
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the user name for HTTP basic authentication.
   *
   * @param username The user name
   */
  public void setUsername(
      String username) {
    this.username = username;
  }

  /**
   * The password for HTTP basic authentication.
   *
   * @return The password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets the password for HTTP basic authentication.
   *
   * @param password The password
   */
  public void setPassword(
      String password) {
    this.password = password;
  }

  /**
   * Tells whether HTTP basic authentication is used.
   *
   * @return Whether basic authentication is on
   */
  public boolean isBasic() {
    return basic;
  }

  /**
   * Sets whether HTTP basic authentication is used.
   *
   * @param basic Whether basic authentication is on
   */
  public void setBasic(
      boolean basic) {
    this.basic = basic;
  }

  /**
   * The settings to fetch an OAuth token, which is sent as bearer token.
   *
   * @return The OAuth settings, or {@code null} if OAuth is not used
   */
  public OauthAuthentication getOauth() {
    return oauth;
  }

  /**
   * Sets the settings to fetch an OAuth token, which is sent as bearer token.
   *
   * @param oauth The OAuth settings
   */
  public void setOauth(
      OauthAuthentication oauth) {
    this.oauth = oauth;
  }

}
