package io.vanillabp.cockpit.commons.security.usercontext;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * What the cockpit knows about a user: its id, its name and its authorities. The authorities are
 * the groups of the user plus one authority for the user itself.
 */
public interface UserDetails {

  /**
   * The id of the user. Other parts of the cockpit refer to the user by it.
   *
   * @return The id
   */
  String getId();

  /**
   * The email address of the user.
   *
   * @return The address
   */
  String getEmail();

  /**
   * The name of the user as the cockpit shows it, like {@code Doe, Jane}.
   *
   * @return The name, or {@code null} if it is not known
   */
  String getDisplay();

  /**
   * The name of the user for places with little room.
   *
   * @return The name, or {@code null} if it is not known
   */
  String getDisplayShort();

  /**
   * The authorities of the user: its groups and its own authority {@code USER_} plus the user id.
   *
   * @return The authorities
   */
  List<String> getAuthorities();

  /**
   * Tells whether the user has an authority.
   *
   * @param authority The authority
   * @return Whether the user has it
   */
  default boolean hasAuthority(
      final String authority) {

    return getAuthorities()
        .stream()
        .anyMatch(given -> Objects.equals(authority, given));

  }

  /**
   * Tells whether the user has at least one of some authorities.
   *
   * @param authorities The authorities
   * @return Whether the user has at least one of them
   */
  default boolean hasOneAuthorityOf(
      final Collection<String> authorities) {

    return getAuthorities()
        .stream()
        .anyMatch(authorities::contains);

  }

  /**
   * Tells whether the user has all of some authorities.
   *
   * @param authorities The authorities
   * @return Whether the user has each of them
   */
  default boolean hasAllAuthoritiesOf(
      final Collection<String> authorities) {

    final var given = getAuthorities();
    return authorities
        .stream()
        .allMatch(given::contains);

  }

}
