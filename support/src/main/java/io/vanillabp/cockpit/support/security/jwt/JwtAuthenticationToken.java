package io.vanillabp.cockpit.support.security.jwt;

import java.io.Serial;
import java.util.Collection;
import java.util.Optional;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * A login which Spring Security knows from a JSON Web Token (JWT). The token is kept as the
 * details of the authentication. The principal is the subject of the token, the user id.
 * <p>
 * Next to the token it may hold the user as the {@link UserDirectoryService} gave it for this
 * request. The token carries no groups, so the authorities come from that user.
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * The user as the directory gave it. It is not serialized: the login lives in the token, and
   * the next request asks the directory again.
   */
  private final transient UserDetails directoryUser;

  /**
   * Creates an authentication which counts as authenticated, because the token was checked before.
   *
   * @param jwt         The checked token
   * @param authorities The authorities of the user
   */
  public JwtAuthenticationToken(
      final Jwt jwt,
      final Collection<? extends GrantedAuthority> authorities) {

    this(jwt, authorities, null);

  }

  /**
   * Creates an authentication which counts as authenticated, because the token was checked before.
   *
   * @param jwt           The checked token
   * @param authorities   The authorities of the user
   * @param directoryUser The user as the directory gave it, or {@code null} if the directory does
   *                      not know the user
   */
  public JwtAuthenticationToken(
      final Jwt jwt,
      final Collection<? extends GrantedAuthority> authorities,
      final UserDetails directoryUser) {

    super(authorities);
    super.setDetails(jwt);
    this.directoryUser = directoryUser;
    setAuthenticated(true);

  }

  @Override
  public Object getPrincipal() {

    return getJwt().getSubject();

  }

  @Override
  public Object getCredentials() {

    return getJwt().getTokenValue();

  }

  /**
   * The token this login comes from.
   *
   * @return The token
   */
  public Jwt getJwt() {

    return (Jwt) getDetails();

  }

  /**
   * The user as the directory gave it for this request.
   *
   * @return The user, or empty if the directory does not know the user
   */
  public Optional<UserDetails> getDirectoryUser() {

    return Optional.ofNullable(directoryUser);

  }

}
