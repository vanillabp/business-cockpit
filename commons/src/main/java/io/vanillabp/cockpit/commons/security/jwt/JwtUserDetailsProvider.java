package io.vanillabp.cockpit.commons.security.jwt;

import org.springframework.security.core.Authentication;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.commons.security.usercontext.UserDetailsProvider;

/**
 * Finds the details of a user logged in with a login token (a JSON Web Token, JWT).
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
