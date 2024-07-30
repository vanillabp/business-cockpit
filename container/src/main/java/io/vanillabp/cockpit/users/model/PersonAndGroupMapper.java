package io.vanillabp.cockpit.users.model;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;

public interface PersonAndGroupMapper {
    Person toModelPerson(String personId);
    Group toModelGroup(String groupId);
    Person toModelPerson(UserDetails user);
}
