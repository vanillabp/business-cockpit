package io.vanillabp.cockpit.support.security.usercontext;

import java.util.List;
import java.util.Optional;

/**
 * The directory of the users a cockpit knows. The application provides it, because only the
 * application knows where its users are kept. The cockpit uses it to suggest users, for example
 * when a task is assigned, and to find the groups of a user. A workflow module can use it as
 * well.
 *
 * <p>Do not mix it up with {@link UserDetailsProvider}. That one says who is calling in the
 * current request. This one knows all users, including those who are not logged in.
 */
public interface UserDirectoryService {

  /**
   * Finds the users whose details contain the given text.
   *
   * @param query The text to look for
   * @return The users found
   */
  List<UserDetails> findUsers(
      String query);

  /**
   * Finds the users whose details contain the given text, leaving out some users.
   *
   * @param query The text to look for
   * @param excludeUsersIds The ids of the users to leave out
   * @return The users found
   */
  List<UserDetails> findUsers(
      String query,
      List<String> excludeUsersIds);

  /**
   * All users of the directory.
   *
   * @return The users
   */
  List<UserDetails> getAllUsers();

  /**
   * All users of the directory, leaving out some users.
   *
   * @param excludeUsersIds The ids of the users to leave out
   * @return The users
   */
  List<UserDetails> getAllUsers(
      List<String> excludeUsersIds);

  /**
   * One user of the directory.
   * <p>
   * The cockpit calls this on every request of a logged-in user, because the login token carries
   * no groups. The groups of the user are the {@link UserDetails#getAuthorities() authorities} of
   * the result. So a group taken away here is gone on the next request. An implementation which
   * asks a remote system, like an identity provider or LDAP, should cache the result for a few
   * minutes. A group taken away then is gone once the cache expires.
   * <p>
   * The authorities are the groups the user is assigned to. A group hierarchy, if the application
   * has one, is resolved by the cockpit and does not belong here.
   * <p>
   * The id is the subject of the login token, which is the name of the authentication at the
   * login. If this method throws, the request of the user fails.
   *
   * @param id The id of the user
   * @return The user, or empty if the directory does not know the id. Such a user keeps its login
   *         but has no groups.
   */
  Optional<UserDetails> getUser(
      String id);

}
