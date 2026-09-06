package io.vanillabp.cockpit.users;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import java.util.List;

public class UserDetailsImpl implements UserDetails {
    private String id;
    private String email;
    private String display;
    private String displayShort;
    private List<String> authorities;

    public UserDetailsImpl(String id, String email, String display, String displayShort, List<String> authorities) {
        this.id = id;
        this.email = email;
        this.display = display;
        this.displayShort = displayShort;
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
    public String getDisplay() {
        return display;
    }

    @Override
    public String getDisplayShort() {
        return displayShort;
    }

    @Override
    public List<String> getAuthorities() {
        return authorities;
    }
};
