package io.vanillabp.cockpit.commons.security.jwt;

import org.springframework.boot.web.server.Cookie.SameSite;

/**
 * The settings of the cookie which carries the login token (a JSON Web Token, JWT) to the
 * browser, and how long the token and the login live.
 */
public class JwtCookie {

  private String name = "bc";

  private String domain;

  private String path = "/";

  private SameSite sameSite;

  private boolean secure;

  /**
   * How long a token lives after its last renewal. A request made in the second half of that time
   * gets a new token, see {@link JwtRenewalFilter}.
   */
  private String expiresDuration = "PT12H";

  /**
   * How long a login lasts at most, however often its token is renewed. It counts from the
   * login, so a stolen cookie does not stay valid forever.
   */
  private String maxLoginDuration = "P7D";

  /**
   * Creates the settings with their defaults: the cookie is called {@code bc}, holds for the whole
   * site, and a token lives twelve hours within a login of at most seven days.
   */
  public JwtCookie() {
  }

  /**
   * The domain the browser sends the cookie to.
   *
   * @return The domain, or {@code null} for the host which set the cookie
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Sets the domain the browser sends the cookie to.
   *
   * @param domain The domain
   */
  public void setDomain(
      String domain) {
    this.domain = domain;
  }

  /**
   * The path below which the browser sends the cookie.
   *
   * @return The path
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the path below which the browser sends the cookie.
   *
   * @param path The path
   */
  public void setPath(
      String path) {
    this.path = path;
  }

  /**
   * The SameSite attribute of the cookie: whether the browser sends it with requests started by
   * other sites.
   *
   * @return The attribute, or {@code null} to leave it to the browser
   */
  public SameSite getSameSite() {
    return sameSite;
  }

  /**
   * Sets the SameSite attribute of the cookie.
   *
   * @param sameSite The attribute
   */
  public void setSameSite(
      SameSite sameSite) {
    this.sameSite = sameSite;
  }

  /**
   * Whether the browser sends the cookie over HTTPS only.
   *
   * @return Whether the cookie is secure
   */
  public boolean isSecure() {
    return secure;
  }

  /**
   * Sets whether the browser sends the cookie over HTTPS only.
   *
   * @param secure Whether the cookie is secure
   */
  public void setSecure(
      boolean secure) {
    this.secure = secure;
  }

  /**
   * The name of the cookie.
   *
   * @return The name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the cookie.
   *
   * @param name The name
   */
  public void setName(
      String name) {
    this.name = name;
  }

  /**
   * How long a token lives after its last renewal.
   *
   * @return The time as an ISO-8601 duration
   */
  public String getExpiresDuration() {
    return expiresDuration;
  }

  /**
   * Sets how long a token lives after its last renewal.
   *
   * @param expiresDuration The time as an ISO-8601 duration
   */
  public void setExpiresDuration(
      String expiresDuration) {
    this.expiresDuration = expiresDuration;
  }

  /**
   * How long a login lasts at most, counted from the login.
   *
   * @return The time as an ISO-8601 duration
   */
  public String getMaxLoginDuration() {
    return maxLoginDuration;
  }

  /**
   * Sets how long a login lasts at most, counted from the login.
   *
   * @param maxLoginDuration The time as an ISO-8601 duration
   */
  public void setMaxLoginDuration(
      String maxLoginDuration) {
    this.maxLoginDuration = maxLoginDuration;
  }

}
