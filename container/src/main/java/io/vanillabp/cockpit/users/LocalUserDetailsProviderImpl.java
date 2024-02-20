package io.vanillabp.cockpit.users;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;

import java.util.List;
import java.util.Optional;

public class LocalUserDetailsProviderImpl implements UserDetailsProvider {

    private static final List<UserDetails> KNOWN_USERS = List.of(
            new UserDetailsImpl("hmu", true, "hmu@test.com", "Hans", "Müller", null, Boolean.FALSE, List.of("TEST")),
            new UserDetailsImpl("akl", true, "akl@test.com", "Anne", "Klein", null, Boolean.TRUE, List.of("TEST")),
            new UserDetailsImpl("rma", true, "rma@test.com", "Rolf-Rüdiger", "Mannheimer", null, Boolean.FALSE, List.of("TEST")),
            new UserDetailsImpl("est", true, "est@test.com", "Elisabeth", "Stockinger", null, null, List.of("TEST")),
            new UserDetailsImpl("test", true, "test@test.com", "Test", "Tester", null, null, List.of("TEST"))
    );

    @Override
    public Optional<UserDetails> getUser(
            final String id) {

        return KNOWN_USERS
                .stream()
                .filter(user -> user.getId().equals(id))
                .findFirst();

    }

    @Override
    public List<UserDetails> findUsers(String query) {

        return KNOWN_USERS
                .stream()
                .filter(user -> {
                    if (user.getFirstName().toLowerCase().contains(query)) return true;
                    if (user.getLastName().toLowerCase().contains(query)) return true;
                    if (user.getEmail().toLowerCase().contains(query)) return true;
                    return false;
                })
                .toList();

    }

    @Override
    public List<UserDetails> findUsers(String query, List<String> excludeUsersIds) {

        return KNOWN_USERS
                .stream()
                .filter(user -> !excludeUsersIds.contains(user.getId()))
                .filter(user -> {
                    if (user.getFirstName().toLowerCase().contains(query)) return true;
                    if (user.getLastName().toLowerCase().contains(query)) return true;
                    if (user.getEmail().toLowerCase().contains(query)) return true;
                    return false;
                })
                .toList();

    }

    @Override
    public List<UserDetails> getAllUsers() {

        return KNOWN_USERS;

    }

    @Override
    public List<UserDetails> getAllUsers(List<String> excludeUsersIds) {

        return KNOWN_USERS
                .stream()
                .filter(user -> !excludeUsersIds.contains(user.getId()))
                .toList();

    }

}
