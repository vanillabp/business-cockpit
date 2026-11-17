package io.vanillabp.cockpit.support.quarkus.it;

import java.util.List;
import java.util.Optional;

import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.support.security.usercontext.UserDetailsImpl;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The directory of the application: a loan manager and a salesperson.
 */
@ApplicationScoped
public class UserDirectory implements UserDirectoryService {

  private static final List<UserDetails> USERS = List.of(
      new UserDetailsImpl("john", "john@example.com", "Doe, John", "Doe, J.", List.of("LOAN_MANAGER")),
      new UserDetailsImpl("jane", "jane@example.com", "Roe, Jane", "Roe, J.", List.of("SALES")));

  @Override
  public List<UserDetails> findUsers(
      final String query) {

    return USERS;

  }

  @Override
  public List<UserDetails> findUsers(
      final String query,
      final List<String> excludeUsersIds) {

    return getAllUsers(excludeUsersIds);

  }

  @Override
  public List<UserDetails> getAllUsers() {

    return USERS;

  }

  @Override
  public List<UserDetails> getAllUsers(
      final List<String> excludeUsersIds) {

    return USERS
        .stream()
        .filter(user -> !excludeUsersIds.contains(user.getId()))
        .toList();

  }

  @Override
  public Optional<UserDetails> getUser(
      final String id) {

    return USERS
        .stream()
        .filter(user -> user.getId().equals(id))
        .findFirst();

  }

}
