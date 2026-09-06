package io.vanillabp.cockpit.users.model;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.gui.api.v1.Group;
import io.vanillabp.cockpit.gui.api.v1.Person;

public interface PersonAndGroupApiMapper {
    Person userToApiPerson(UserDetails user);
    Person personToApiPerson(io.vanillabp.cockpit.users.model.Person person);
    Person toApiPerson(String personId);
    Group groupToApiGroup(io.vanillabp.cockpit.users.model.Group group);
    Group authorityToApiGroup(String authority);
}
