package io.vanillabp.cockpit.users;

import java.util.Collection;

public class TestUserDetailsImpl extends UserDetailsImpl {
    public TestUserDetailsImpl(String id, UserStatus status, String email, String firstName, String lastName, String avatar, Sex sex, Collection<String> roles) {
        super(id, status, email, firstName, lastName, avatar, sex, roles);
    }

}
