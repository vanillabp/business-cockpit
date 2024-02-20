package io.vanillabp.cockpit.users;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;

import java.util.List;
import java.util.Optional;

public interface UserDetailsProvider {

    List<UserDetails> findUsers(String query);

    List<UserDetails> getAllUsers();

    Optional<UserDetails> getUser(String id);

}
