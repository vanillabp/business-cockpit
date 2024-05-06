package io.vanillabp.cockpit.users;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import java.util.List;

public class UserDetailsImpl implements UserDetails {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> authorities;

    public UserDetailsImpl(String id, String email, String firstName, String lastName, List<String> authorities) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.authorities = authorities;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public List<String> getAuthorities() {
        return authorities;
    }
};
