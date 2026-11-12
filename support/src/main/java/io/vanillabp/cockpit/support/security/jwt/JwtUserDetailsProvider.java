package io.vanillabp.cockpit.support.security.jwt;

import org.springframework.security.core.Authentication;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsProvider;

/**
 * Finds the details of a user logged in with a login token (a JSON Web Token, JWT). It needs no
 * user directory of its own. The {@link JwtAuthenticationTokenMapper} already asked the directory
 * for the user of the request, and the details read the groups, the name and the e-mail address
 * from that answer.
 */
public class JwtUserDetailsProvider implements UserDetailsProvider {

  /**
   * Creates the provider.
   */
  public JwtUserDetailsProvider() {
  }

  @Override
  public UserDetails getUserDetails(
      final Authentication authentication) {

    if (authentication == null) {
      return null;
    }
    if (authentication instanceof JwtAuthenticationToken) {
      return new JwtUserDetails((JwtAuthenticationToken) authentication);
    }
    return null;

  }

}
