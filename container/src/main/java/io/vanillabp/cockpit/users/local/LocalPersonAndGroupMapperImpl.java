package io.vanillabp.cockpit.users.local;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;

public class LocalPersonAndGroupMapperImpl implements PersonAndGroupMapper {
    private final UserDetailsProvider userDetailsProvider;

    public LocalPersonAndGroupMapperImpl(
            final UserDetailsProvider userDetailsProvider) {

        this.userDetailsProvider = userDetailsProvider;

    }

    @Override
    public Person toModelPerson(
            final String personId) {

        final var userFound = userDetailsProvider
                .getUser(personId);
        if (userFound.isEmpty()) {
            return null;
        }
        final var user = userFound.get();

        final var result = new Person();
        result.setId(user.getId());
        result.setFulltext(user.getLastName() + ", " + user.getFirstName());
        result.setSort(user.getLastName()
                + "                                                                                                    "
                + user.getFirstName());
        return result;

    }

    @Override
    public Person toModelPerson(
            final UserDetails user) {

        final var result = new Person();
        result.setId(user.getId());
        result.setFulltext(user.getLastName() + ", " + user.getFirstName());
        result.setSort(user.getLastName()
                + "                                                                                                    "
                + user.getFirstName());
        return result;

    }

    @Override
    public Group toModelGroup(
            final String groupId) {

        final var result = new Group();
        result.setId(groupId);
        result.setFulltext(groupId);
        result.setSort(groupId);
        return result;

    }

}
