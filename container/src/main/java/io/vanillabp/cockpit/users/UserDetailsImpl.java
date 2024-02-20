package io.vanillabp.cockpit.users;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;

import java.util.List;

class UserDetailsImpl implements UserDetails {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String avatar;
    private boolean active;
    private Boolean female;
    private List<String> authorities;

    public UserDetailsImpl(String id, boolean active, String email, String firstName, String lastName, String avatar, Boolean female, List<String> authorities) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatar = avatar;
        this.authorities = authorities;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isActive() {
        return active;
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
    public Boolean isFemale() {
        return female;
    }

    @Override
    public List<String> getAuthorities() {
        return authorities;
    }
};
