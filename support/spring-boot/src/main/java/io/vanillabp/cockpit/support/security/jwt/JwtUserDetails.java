package io.vanillabp.cockpit.support.security.jwt;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import io.vanillabp.cockpit.support.security.usercontext.DirectoryUserLookup;
import io.vanillabp.cockpit.support.security.usercontext.UserDetails;

/**
 * The details of a user logged in by a login token (a JSON Web Token, JWT). The id is the subject
 * of the token. The name and the e-mail address come from the user directory. If the directory
 * does not know the user or has no value, they come from the claims of the token. The authorities
 * are those of the authentication, so the groups the directory gave for this request.
 * <p>
 * The name and the e-mail address are read by {@link TokenUserDetails} of the support's core, the
 * same a Quarkus workflow module reads.
 */
public class JwtUserDetails implements UserDetails {

  /**
   * The start of the authority each user has for itself, followed by the user id. With it, a check
   * for a list of authorities can name single users as well as groups.
   */
  public static final String USER_AUTHORITY_PREFIX = DirectoryUserLookup.USER_AUTHORITY_PREFIX;

  private final JwtAuthenticationToken authenticationToken;

  /**
   * Creates the details out of an authentication.
   *
   * @param authenticationToken The authentication, which holds the token
   */
  public JwtUserDetails(
      final JwtAuthenticationToken authenticationToken) {

    this.authenticationToken = authenticationToken;

  }

  private TokenUserDetails fromTheToken() {

    return new TokenUserDetails(
        authenticationToken.getJwt().getClaims(), authenticationToken.getDirectoryUser(), getAuthorities());

  }

  @Override
  public String getId() {

    return authenticationToken.getJwt().getSubject();

  }

  @Override
  public String getDisplay() {

    return fromTheToken().getDisplay();

  }

  @Override
  public String getDisplayShort() {

    return fromTheToken().getDisplayShort();

  }

  /**
   * The e-mail address the directory knows, or else the claim {@code email} of the token. The
   * subject is the user id and no e-mail address.
   *
   * @return The e-mail address, or {@code null} if neither knows one
   */
  @Override
  public String getEmail() {

    return fromTheToken().getEmail();

  }

  @Override
  public List<String> getAuthorities() {

    return authenticationToken
        .getAuthorities()
        .stream()
        .map(GrantedAuthority::getAuthority)
        .toList();

  }

}
