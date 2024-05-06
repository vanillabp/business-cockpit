package io.vanillabp.cockpit.users;

import java.util.List;

public class TestUserDetailsImpl extends UserDetailsImpl {
    public TestUserDetailsImpl(String id, String email, String firstName, String lastName, List<String> authorities) {
        super(id, email, firstName, lastName, authorities);
    }

}
