package io.vanillabp.cockpit.support.security.usercontext;

import org.springframework.security.core.Authentication;

/**
 * Finds the details of a user out of a Spring Security authentication. Each way to log in brings
 * its own provider.
 *
 * <p>It knows only the user who is calling. To look up any other user, use the
 * {@link UserDirectoryService}.
 */
public interface UserDetailsProvider {

  /**
   * Finds the details of the user of an authentication.
   *
   * @param authentication The authentication
   * @return The details, or {@code null} if this provider does not know the kind of authentication
   */
  UserDetails getUserDetails(
      Authentication authentication);

}
