package io.vanillabp.cockpit.commons.security.usercontext;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import io.vanillabp.cockpit.commons.exceptions.BcUnauthorizedException;

/**
 * Tells who is logged in for the current request.
 */
public class UserContext {

  private final UserDetailsProvider userDetailsProvider;

  /**
   * Creates the context.
   *
   * @param userDetailsProvider Finds the details of the user logged in
   */
  public UserContext(
      final UserDetailsProvider userDetailsProvider) {

    this.userDetailsProvider = userDetailsProvider;

  }

  /**
   * The id of the user logged in.
   *
   * @return The user id, or {@code null} for an anonymous login or if the details of the user are
   *         not known
   * @throws BcUnauthorizedException If the request has no authentication at all, or one which is not
   *                                 authenticated
   */
  public String getUserLoggedIn() throws BcUnauthorizedException {

    final var authentication = SecurityContextHolder
        .getContext()
        .getAuthentication();

    if (authentication == null) {
      throw new BcUnauthorizedException("No security context");
    }
    if (!authentication.isAuthenticated()) {
      throw new BcUnauthorizedException("User anonymous");
    }
    if (authentication instanceof AnonymousAuthenticationToken) {
      return null;
    }

    final var userDetails = userDetailsProvider
        .getUserDetails(authentication);
    if (userDetails != null) {
      return userDetails.getId();
    }
    return null;

  }

  /**
   * The details of the user logged in.
   *
   * @return The details, or {@code null} for an anonymous login or if they are not known
   * @throws BcUnauthorizedException If the request has no authentication at all, or one which is not
   *                                 authenticated
   */
  public UserDetails getUserLoggedInDetails() throws BcUnauthorizedException {

    final var authentication = SecurityContextHolder
        .getContext()
        .getAuthentication();

    if (authentication == null) {
      throw new BcUnauthorizedException("No security context");
    }
    if (!authentication.isAuthenticated()) {
      throw new BcUnauthorizedException("User anonymous");
    }
    if (authentication instanceof AnonymousAuthenticationToken) {
      return null;
    }

    return userDetailsProvider
        .getUserDetails(authentication);

  }

}