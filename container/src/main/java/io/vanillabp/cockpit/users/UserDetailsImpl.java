package io.vanillabp.cockpit.users;

import java.util.Collection;

class UserDetailsImpl implements UserDetails {
    private String id;
    private UserStatus status;
    private String email;
    private String firstName;
    private String lastName;
    private String avatar;
    private Sex sex;
    private Collection<String> roles;

    public UserDetailsImpl(String id, UserStatus status, String email, String firstName, String lastName, String avatar, Sex sex, Collection<String> roles) {
        this.id = id;
        this.status = status;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.avatar = avatar;
        this.sex = sex;
        this.roles = roles;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public UserStatus getStatus() {
        return status;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getAvatar() {
        return avatar;
    }

    @Override
    public Sex getSex() {
        return sex;
    }

    @Override
    public Collection<String> getRoles() {
        return roles;
    }
};
