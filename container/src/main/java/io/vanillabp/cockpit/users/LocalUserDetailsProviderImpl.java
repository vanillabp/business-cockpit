package io.vanillabp.cockpit.users;

import java.util.List;
import java.util.Optional;

public class LocalUserDetailsProviderImpl implements UserDetailsProvider {

    private static final List<UserDetails> KNOWN_USERS = List.of(
            new UserDetailsImpl("hmu", UserDetails.UserStatus.Active, "hmu@test.com", "Hans", "Müller", null, UserDetails.Sex.Male, List.of("TEST")),
            new UserDetailsImpl("akl", UserDetails.UserStatus.Active, "akl@test.com", "Anne", "Klein", null, UserDetails.Sex.Female, List.of("TEST")),
            new UserDetailsImpl("rma", UserDetails.UserStatus.Active, "rma@test.com", "Rolf-Rüdiger", "Mannheimer", null, UserDetails.Sex.Male, List.of("TEST")),
            new UserDetailsImpl("est", UserDetails.UserStatus.Active, "est@test.com", "Elisabeth", "Stockinger", null, UserDetails.Sex.Other, List.of("TEST")),
            new UserDetailsImpl("test", UserDetails.UserStatus.Active, "test@test.com", "Test", "Tester", null, UserDetails.Sex.Other, List.of("TEST"))
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
    public List<UserDetails> getAllUsers() {

        return KNOWN_USERS;

    }

}
