package io.vanillabp.cockpit.support.security.usercontext;

import java.util.Optional;
import java.util.Set;

/**
 * The user of a valid login token, as {@link DirectoryUserLookup} found it for one request.
 *
 * @param id            The user id, the subject of the token
 * @param directoryUser The user as the directory gave it, or empty if the directory does not know
 *                      the user
 * @param authorities   The authority {@code USER_} plus the user id, and the groups of the user
 */
public record LoggedInUser(
                           String id,
                           Optional<UserDetails> directoryUser,
                           Set<String> authorities) {

  /**
   * Keeps an unmodifiable copy of the authorities.
   *
   * @param id            The user id
   * @param directoryUser The user as the directory gave it, or empty
   * @param authorities   The authorities of the user
   */
  public LoggedInUser {

    authorities = Set.copyOf(authorities);

  }

}
