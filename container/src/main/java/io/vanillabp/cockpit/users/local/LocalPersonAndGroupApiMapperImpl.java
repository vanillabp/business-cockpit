package io.vanillabp.cockpit.users.local;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.gui.api.v1.Group;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;

public class LocalPersonAndGroupApiMapperImpl implements PersonAndGroupApiMapper {
    private final UserDetailsProvider userDetailsProvider;

    public LocalPersonAndGroupApiMapperImpl(
            final UserDetailsProvider userDetailsProvider) {

        this.userDetailsProvider = userDetailsProvider;

    }

    @Override
    public io.vanillabp.cockpit.gui.api.v1.Person personToApiPerson(
            final Person person) {

        if (person == null) {
            return null;
        }
        return toApiPerson(person.getId());

    }

    @Override
    public io.vanillabp.cockpit.gui.api.v1.Person userToApiPerson(
            final UserDetails user) {

        if (user == null) {
            return null;
        }
        return new io.vanillabp.cockpit.gui.api.v1.Person()
                .id(user.getId())
                .email(user.getEmail())
                .display(user.getDisplay())
                .displayShort(user.getDisplayShort());

    }

    @Override
    public io.vanillabp.cockpit.gui.api.v1.Person toApiPerson(
            final String personId) {

        final var userFound = userDetailsProvider
                .getUser(personId);
        return userFound
                .map(this::userToApiPerson)
                .orElse(null);

    }

    @Override
    public Group groupToApiGroup(
            final io.vanillabp.cockpit.users.model.Group group) {

        return authorityToApiGroup(group.getId());

    }

    @Override
    public Group authorityToApiGroup(
            final String authority) {

        return new Group()
                .id(authority)
                .display(authority);

    }

}
