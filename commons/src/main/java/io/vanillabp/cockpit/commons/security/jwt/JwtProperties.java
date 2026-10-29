package io.vanillabp.cockpit.commons.security.jwt;

import java.util.Base64;

import javax.crypto.KeyGenerator;

import org.springframework.util.StringUtils;

/**
 * The settings of the login token (a JSON Web Token, JWT): the cookie which carries it and the key
 * it is signed with.
 */
public class JwtProperties {

  private JwtCookie cookie = new JwtCookie();

  private String hmacSHA256Base64;

  /**
   * Creates the settings with the default cookie settings and without a signing key.
   */
  public JwtProperties() {
  }

  /**
   * The settings of the cookie which carries the token.
   *
   * @return The cookie settings
   */
  public JwtCookie getCookie() {
    return cookie;
  }

  /**
   * Sets the settings of the cookie which carries the token.
   *
   * @param cookie The cookie settings
   */
  public void setCookie(
      JwtCookie cookie) {
    this.cookie = cookie;
  }

  /**
   * The key the tokens are signed with (HMAC with SHA-256), Base64 encoded. There is no default. If
   * it is missing, this method fails with a message which suggests a newly generated key.
   *
   * @return The key, Base64 encoded
   */
  public String getHmacSHA256Base64() {
    if (!StringUtils.hasText(hmacSHA256Base64)) {
      final String hmacKey;
      try {
        final var key = KeyGenerator
            .getInstance("HmacSha256")
            .generateKey();
        hmacKey = Base64
            .getEncoder()
            .encodeToString(key.getEncoded());
      } catch (Exception e) {
        throw new RuntimeException(
            "No property 'business-cockpit.jwt.hmacSHA256-base64' set! "
                + "Additionally, on generating a key to be used an error occured.", e);
      }
      throw new RuntimeException(
          "No property 'business-cockpit.jwt.hmacSHA256-base64' set! "
              + "One could use this generated key: '"
              + hmacKey
              + "'.");
    }
    return hmacSHA256Base64;
  }

  /**
   * Sets the key the tokens are signed with (HMAC with SHA-256), Base64 encoded. Every instance of
   * the cockpit needs the same key, otherwise one instance does not accept the tokens of another.
   *
   * @param hmacSHA256Base64 The key, Base64 encoded
   */
  public void setHmacSHA256Base64(
      String hmacSHA256Base64) {
    this.hmacSHA256Base64 = hmacSHA256Base64;
  }

  /**
   * The key the tokens are signed with, as bytes.
   *
   * @return The key
   */
  public byte[] getHmacSHA256() {

    return Base64.getDecoder().decode(getHmacSHA256Base64());

  }

}
