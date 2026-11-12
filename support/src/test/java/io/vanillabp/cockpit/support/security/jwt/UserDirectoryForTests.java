package io.vanillabp.cockpit.support.security.jwt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * A user directory a test can change while it runs, like an administrator who takes a group away.
 * It counts how often it was asked for a user, and it can be told to fail.
 */
class UserDirectoryForTests implements UserDirectoryService {

  private final Map<String, UserDetails> users = new LinkedHashMap<>();

  private RuntimeException failure;

  int lookups;

  UserDirectoryForTests with(
      final String id,
      final String... groups) {

    users.put(id, new UserDetailsImpl(id, null, null, null, List.of(groups)));
    return this;

  }

  UserDirectoryForTests with(
      final UserDetails user) {

    users.put(user.getId(), user);
    return this;

  }

  void remove(
      final String id) {

    users.remove(id);

  }

  void failWith(
      final RuntimeException failure) {

    this.failure = failure;

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

    lookups++;
    if (failure != null) {
      throw failure;
    }
    return Optional.ofNullable(users.get(id));

  }

}
