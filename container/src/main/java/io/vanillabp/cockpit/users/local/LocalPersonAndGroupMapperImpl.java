package io.vanillabp.cockpit.users.local;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;

/**
 * Turns the ids a workflow module reports into persons and groups for the demo users of the
 * profile {@code local}. The stored person carries the display name for the full-text search,
 * and a sort value which sorts by the first word of the name before the rest. An id the directory
 * does not know is stored as it is, without a name.
 */
public class LocalPersonAndGroupMapperImpl implements PersonAndGroupMapper {
    private final UserDetailsProvider userDetailsProvider;

    /**
     * Creates the mapper on top of the demo users.
     *
     * @param userDetailsProvider The directory of the demo users, which is asked for the name of
     *          a person by its id
     */
    public LocalPersonAndGroupMapperImpl(
            final UserDetailsProvider userDetailsProvider) {

        this.userDetailsProvider = userDetailsProvider;

    }

    @Override
    public Person toModelPerson(
            final String personId) {

        final var result = new Person();

        final var userFound = userDetailsProvider
                .getUser(personId);
        if (userFound.isEmpty()) {
            result.setId(personId);
            return result;
        }
        final var user = userFound.get();

        result.setId(user.getId());
        result.setFulltext(user.getDisplay());
        if (user.getDisplay() != null) {
            final var indexOfSpace = user.getDisplay().indexOf(' ');
            if (indexOfSpace != -1) {
                result.setSort(user.getDisplay().substring(0, indexOfSpace)
                        + "                                                                                "
                        + user.getDisplay().substring(indexOfSpace + 1));
            } else {
                result.setSort(user.getDisplay());
            }
        }
        return result;

    }

    @Override
    public Person toModelPerson(
            final UserDetails user) {

        final var result = new Person();
        result.setId(user.getId());
        result.setFulltext(user.getDisplay());
        if (user.getDisplay() != null) {
            final var indexOfSpace = user.getDisplay().indexOf(' ');
            if (indexOfSpace != -1) {
                result.setSort(user.getDisplay().substring(0, indexOfSpace)
                        + "                                                                                "
                        + user.getDisplay().substring(indexOfSpace + 1));
            } else {
                result.setSort(user.getDisplay());
            }
        }
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
