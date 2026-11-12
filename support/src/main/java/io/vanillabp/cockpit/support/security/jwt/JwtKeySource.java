package io.vanillabp.cockpit.support.security.jwt;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Where the key comes from which signs a login token and checks it. {@link JwtMapper} asks this
 * interface and does not know the key itself.
 * <p>
 * Today there is one implementation, {@link HmacJwtKeySource}. It uses the shared secret of the
 * settings, so the cockpit and every workflow module behind it sign and check with the same
 * secret. A later version may check tokens against the public keys of an identity provider
 * instead. That version adds an implementation of this interface, and the code which uses
 * {@link JwtMapper} stays as it is.
 */
public interface JwtKeySource {

  /**
   * Signs the claims of a new token.
   *
   * @param claimsSet The claims of the token
   * @return The signed token, as it is written to the cookie
   */
  String sign(
      JwtClaimsSet claimsSet);

  /**
   * Checks the signature and the expiry of a token and reads its claims.
   *
   * @param token The token as sent in the cookie
   * @return The checked token
   * @throws JwtException If the signature is wrong, the token has expired or cannot be read
   */
  Jwt check(
      String token) throws JwtException;

}
