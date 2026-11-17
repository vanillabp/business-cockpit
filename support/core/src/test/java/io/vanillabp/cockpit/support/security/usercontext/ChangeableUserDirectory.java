package io.vanillabp.cockpit.support.security.usercontext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A user directory a test can change while it runs, like an administrator who takes a group away.
 * It can be told to fail. The tests of both platform modules use it.
 */
public class ChangeableUserDirectory implements UserDirectoryService {

  private final Map<String, UserDetails> users = new LinkedHashMap<>();

  private RuntimeException failure;

  /**
   * Adds the user, or replaces it, with no name and no e-mail address.
   *
   * @param id     The user id
   * @param groups The groups of the user
   * @return This directory
   */
  public ChangeableUserDirectory with(
      final String id,
      final String... groups) {

    users.put(id, new UserDetailsImpl(id, null, null, null, List.of(groups)));
    return this;

  }

  /**
   * Adds the user, or replaces it.
   *
   * @param user The user
   * @return This directory
   */
  public ChangeableUserDirectory with(
      final UserDetails user) {

    users.put(user.getId(), user);
    return this;

  }

  /**
   * Removes the user.
   *
   * @param id The user id
   */
  public void remove(
      final String id) {

    users.remove(id);

  }

  /**
   * Lets every later lookup throw the failure, or none for {@code null}.
   *
   * @param failure What to throw
   */
  public void failWith(
      final RuntimeException failure) {

    this.failure = failure;

  }

  /**
   * Forgets all users and the failure.
   */
  public void clear() {

    users.clear();
    failure = null;

  }

  @Override
  public List<UserDetails> findUsers(
      final String query) {

    return getAllUsers();

  }

  @Override
  public List<UserDetails> findUsers(
      final String query,
      final List<String> excludeUsersIds) {

    return getAllUsers(excludeUsersIds);

  }

  @Override
  public List<UserDetails> getAllUsers() {

    return new ArrayList<>(users.values());

  }

  @Override
  public List<UserDetails> getAllUsers(
      final List<String> excludeUsersIds) {

    return users
        .values()
        .stream()
        .filter(user -> !excludeUsersIds.contains(user.getId()))
        .toList();

  }

  @Override
  public Optional<UserDetails> getUser(
      final String id) {

    if (failure != null) {
      throw failure;
    }
    return Optional.ofNullable(users.get(id));

  }

}
