package io.vanillabp.cockpit.users.directory;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.util.StringUtils;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * What the directories of the cockpit share: the cache of {@link #getUser(String)}, the lists which
 * leave some users out, and how the name of a user is shown.
 * <p>
 * Only {@link #getUser(String)} gives the groups of a user. The lists are for proposing a user, and
 * the cockpit reads only the id of each user in them. Giving the groups there would cost one more
 * request to the directory for every user in the list.
 */
abstract class CachingUserDirectoryService implements UserDirectoryService {

  private final CachedUsers cachedUsers;

  CachingUserDirectoryService(
      final Duration cacheDuration,
      final Clock clock) {

    this.cachedUsers = new CachedUsers(cacheDuration, clock);

  }

  /**
   * Asks the directory for one user with its groups.
   *
   * @param id The id of the user
   * @return The user, or empty if the directory does not know the id
   */
  abstract Optional<UserDetails> loadUser(
      String id);

  /**
   * Asks the directory for the users whose details contain the text. The users carry no groups.
   *
   * @param query The text, never empty
   * @return The users
   */
  abstract List<UserDetails> searchUsers(
      String query);

  /**
   * Asks the directory for all its users. The users carry no groups.
   *
   * @return The users
   */
  abstract List<UserDetails> loadAllUsers();

  @Override
  public Optional<UserDetails> getUser(
      final String id) {

    if (!StringUtils.hasText(id)) {
      return Optional.empty();
    }
    return cachedUsers.get(id, this::loadUser);

  }

  @Override
  public List<UserDetails> findUsers(
      final String query) {

    return findUsers(query, List.of());

  }

  @Override
  public List<UserDetails> findUsers(
      final String query,
      final List<String> excludeUsersIds) {

    final var users = StringUtils.hasText(query)
        ? searchUsers(query.trim())
        : loadAllUsers();
    return leaveOut(users, excludeUsersIds);

  }

  @Override
  public List<UserDetails> getAllUsers() {

    return loadAllUsers();

  }

  @Override
  public List<UserDetails> getAllUsers(
      final List<String> excludeUsersIds) {

    return leaveOut(loadAllUsers(), excludeUsersIds);

  }

  private static List<UserDetails> leaveOut(
      final List<UserDetails> users,
      final List<String> excludeUsersIds) {

    if ((excludeUsersIds == null) || excludeUsersIds.isEmpty()) {
      return users;
    }
    return users
        .stream()
        .filter(user -> !excludeUsersIds.contains(user.getId()))
        .toList();

  }

  /**
   * Builds the details of a user the way the cockpit shows them: "Doe, Jane" and "Doe, J.". A user
   * without a last name is shown by the id.
   *
   * @param id The id of the user
   * @param email The e-mail address, may be {@code null}
   * @param firstName The first name, may be {@code null}
   * @param lastName The last name, may be {@code null}
   * @param groups The groups of the user
   * @return The details
   */
  static UserDetails userOf(
      final String id,
      final String email,
      final String firstName,
      final String lastName,
      final List<String> groups) {

    final String display;
    final String displayShort;
    if (!StringUtils.hasText(lastName)) {
      display = id;
      displayShort = id;
    } else if (!StringUtils.hasText(firstName)) {
      display = lastName;
      displayShort = lastName;
    } else {
      display = lastName
          + ", "
          + firstName;
      displayShort = lastName
          + ", "
          + firstName.charAt(0)
          + ".";
    }
    return new UserDetailsImpl(id, email, display, displayShort, List.copyOf(groups));

  }

}
