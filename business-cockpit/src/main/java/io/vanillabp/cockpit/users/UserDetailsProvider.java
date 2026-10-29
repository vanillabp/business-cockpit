package io.vanillabp.cockpit.users;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;

import java.util.List;
import java.util.Optional;

/**
 * The directory of the users a cockpit knows. The application provides it, because only the
 * application knows where its users are kept. The cockpit uses it to suggest users, for example
 * when a task is assigned, and to find the groups of a user.
 */
public interface UserDetailsProvider {

    /**
     * Finds the users whose details contain the given text.
     *
     * @param query The text to look for
     * @return The users found
     */
    List<UserDetails> findUsers(String query);

    /**
     * Finds the users whose details contain the given text, leaving out some users.
     *
     * @param query The text to look for
     * @param excludeUsersIds The ids of the users to leave out
     * @return The users found
     */
    List<UserDetails> findUsers(String query, List<String> excludeUsersIds);

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
    List<UserDetails> getAllUsers(List<String> excludeUsersIds);

    /**
     * One user of the directory.
     *
     * @param id The id of the user
     * @return The user, or empty if the directory does not know the id
     */
    Optional<UserDetails> getUser(String id);

}
