package io.vanillabp.cockpit.commons.utils;

import java.util.List;

public interface UserDetails {

    String getId();

    boolean isActive();

    String getEmail();

    String getLastName();

    String getFirstName();

    /**
     * @return TRUE = woman, FALSE = man, null = OTHER
     */
    Boolean isFemale();

    List<String> getAuthorities();

}
