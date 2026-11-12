package io.vanillabp.cockpit.devshell.simulator.usermanagement;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.util.StringUtils;

import io.vanillabp.cockpit.devshell.simulator.config.Properties;
import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * The made-up users of the configuration as a user directory. The login token carries no groups,
 * so whoever reads a token of the dev shell asks a directory for them. The users are read from the
 * properties on each call, which is cheap, so there is no cache.
 */
public class SimulatedUserDirectoryService implements UserDirectoryService {

  private final Properties properties;

  /**
   * Creates the directory.
   *
   * @param properties The properties which list the users
   */
  public SimulatedUserDirectoryService(
      final Properties properties) {

    this.properties = properties;

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

    return getAllUsers(excludeUsersIds)
        .stream()
        .filter(user -> (query == null) || contains(user, query))
        .toList();

  }

  @Override
  public List<UserDetails> getAllUsers() {

    return getAllUsers(List.of());

  }

  @Override
  public List<UserDetails> getAllUsers(
      final List<String> excludeUsersIds) {

    return Optional
        .ofNullable(properties.getUsers())
        .orElse(List.of())
        .stream()
        .filter(user -> (excludeUsersIds == null) || !excludeUsersIds.contains(user.getId()))
        .map(SimulatedUserDirectoryService::toUserDetails)
        .toList();

  }

  @Override
  public Optional<UserDetails> getUser(
      final String id) {

    return Optional
        .ofNullable(properties.getUser(id))
        .map(SimulatedUserDirectoryService::toUserDetails);

  }

  private static boolean contains(
      final UserDetails user,
      final String query) {

    final var lowerCaseQuery = query.toLowerCase();
    return List
        .of(user.getId(), Objects.toString(user.getDisplay(), ""), Objects.toString(user.getEmail(), ""))
        .stream()
        .anyMatch(value -> value.toLowerCase().contains(lowerCaseQuery));

  }

  private static UserDetails toUserDetails(
      final User user) {

    final String display;
    final String displayShort;
    if (!StringUtils.hasText(user.getLastName())) {
      display = user.getId();
      displayShort = user.getId();
    } else if (!StringUtils.hasText(user.getFirstName())) {
      display = user.getLastName();
      displayShort = user.getLastName();
    } else {
      display = user.getLastName()
          + ", "
          + user.getFirstName();
      displayShort = user.getLastName()
          + ", "
          + user.getFirstName().charAt(0)
          + ".";
    }

    return new UserDetailsImpl(
        user.getId(), user.getEmail(), display, displayShort, Optional.ofNullable(user.getGroups()).orElse(List.of()));

  }

}
