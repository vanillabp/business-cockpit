package io.vanillabp.cockpit.users;

import java.util.Collection;

public interface UserDetails {

    enum UserStatus { Active, Inactive };

    enum Sex { Male, Female, Other };

    String getId();

    UserStatus getStatus();

    String getEmail();

    String getFirstName();

    String getLastName();

    String getAvatar();

    Sex getSex();

    Collection<String> getRoles();

}
