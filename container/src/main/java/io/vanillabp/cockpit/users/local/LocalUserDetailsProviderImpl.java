package io.vanillabp.cockpit.users.local;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.users.UserDetailsImpl;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import java.util.List;
import java.util.Optional;

public class LocalUserDetailsProviderImpl implements UserDetailsProvider {

    private static final List<UserDetails> KNOWN_USERS = List.of(
            new UserDetailsImpl("hmu", "hmu@test.com", "Müller, Hans", "Müller, H.", List.of("TEST")),
            new UserDetailsImpl("akl", "akl@test.com", "Klein, Anne", "Klein, A.", List.of("TEST")),
            new UserDetailsImpl("rma", "rma@test.com", "Mannheimer, Rolf-Rüdiger", "Mannheimer, R.", List.of("TEST")),
            new UserDetailsImpl("est", "est@test.com", "Stockinger, Elisabeth", "Stockinger, E.", List.of("TEST")),
            new UserDetailsImpl("test", "test@test.com", "Tester, Test", "Tester, T.", List.of("TEST"))
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
                    if (user.getDisplay().toLowerCase().contains(query)) return true;
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
                    if (user.getDisplay().toLowerCase().contains(query)) return true;
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
