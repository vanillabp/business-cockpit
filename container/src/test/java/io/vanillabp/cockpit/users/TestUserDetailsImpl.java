package io.vanillabp.cockpit.users;

import java.util.List;

public class TestUserDetailsImpl extends UserDetailsImpl {
    public TestUserDetailsImpl(String id, boolean active, String email, String firstName, String lastName, String avatar, Boolean female, List<String> authorities) {
        super(id, active, email, firstName, lastName, avatar, female, authorities);
    }

}
