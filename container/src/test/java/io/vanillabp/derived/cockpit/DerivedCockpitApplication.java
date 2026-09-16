package io.vanillabp.derived.cockpit;

import io.vanillabp.cockpit.commons.security.usercontext.UserDetails;
import io.vanillabp.cockpit.tasklist.UserTaskVisibility;
import io.vanillabp.cockpit.tasklist.api.v1.AbstractUserTaskListGuiApiController;
import io.vanillabp.cockpit.users.UserDetailsImpl;
import io.vanillabp.cockpit.users.UserDetailsProvider;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.WorkflowVisibility;
import io.vanillabp.cockpit.workflowlist.api.v1.AbstractWorkflowListGuiApiController;
import io.vanillabp.cockpit.workflowmodules.WorkflowModuleVisibility;
import io.vanillabp.cockpit.workflowmodules.api.v1.AbstractWorkflowModulesGuiApiController;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A cockpit application written the way the wiki describes one. It has the dependency on
 * {@code business-cockpit}, {@code @SpringBootApplication}, a {@code main} method and the handful
 * of beans the library leaves to the application. There is no base class, no cockpit annotation
 * and no component scan over {@code io.vanillabp.cockpit}.

 *
 * <p>The package matters. It is outside {@code io.vanillabp.cockpit}, so the component scan of
 * {@code @SpringBootApplication} reaches nothing of the library, and the same goes for the package
 * Spring Boot looks in for Spring Data repositories. An application in the library's own package
 * would find those classes by accident, which is what the delivered {@code container} does and why
 * its tests cannot prove this.
 *
 * <p>The user directory answers two users out of a list, which stands in for an identity provider.
 * The visibilities are the ones the delivered application uses, so a difference in what the lists
 * return is a difference in the wiring and not in the rules.
 */
@SpringBootApplication
@Import({
    DerivedCockpitApplication.TaskList.class,
    DerivedCockpitApplication.WorkflowList.class,
    DerivedCockpitApplication.ModuleList.class
})
public class DerivedCockpitApplication {

    public static final String USER = "martin";

    public static final String OTHER_USER = "petra";

    public static final String GROUP_OF_USER = "accounting";

    private static final List<UserDetails> USERS = List.of(
            new UserDetailsImpl(USER, "martin@example.com", "Martin Meier", "M. Meier",
                    List.of(GROUP_OF_USER, "bc-users")),
            new UserDetailsImpl(OTHER_USER, "petra@example.com", "Petra Huber", "P. Huber",
                    List.of("sales", "bc-users")));

    public static void main(final String... args) {

        SpringApplication.run(DerivedCockpitApplication.class, args);

    }

    @Bean
    public UserDetailsProvider theUsersOfThisApplication() {

        return new UserDetailsProvider() {

            @Override
            public List<UserDetails> findUsers(
                    final String query) {

                return findUsers(query, List.of());

            }

            @Override
            public List<UserDetails> findUsers(
                    final String query,
                    final List<String> excludeUsersIds) {

                return getAllUsers(excludeUsersIds)
                        .stream()
                        .filter(user -> query == null || user.getDisplay().contains(query))
                        .toList();

            }

            @Override
            public List<UserDetails> getAllUsers() {

                return USERS;

            }

            @Override
            public List<UserDetails> getAllUsers(
                    final List<String> excludeUsersIds) {

                return USERS
                        .stream()
                        .filter(user -> !excludeUsersIds.contains(user.getId()))
                        .toList();

            }

            @Override
            public Optional<UserDetails> getUser(
                    final String id) {

                return USERS
                        .stream()
                        .filter(user -> user.getId().equals(id))
                        .findFirst();

            }

        };

    }

    @Bean
    public PersonAndGroupApiMapper personsAndGroupsOfTheApi(
            final UserDetailsProvider users) {

        return new PersonAndGroupApiMapper() {

            @Override
            public io.vanillabp.cockpit.gui.api.v1.Person userToApiPerson(
                    final UserDetails user) {

                return user == null
                        ? null
                        : new io.vanillabp.cockpit.gui.api.v1.Person()
                                .id(user.getId())
                                .email(user.getEmail())
                                .display(user.getDisplay())
                                .displayShort(user.getDisplayShort());

            }

            @Override
            public io.vanillabp.cockpit.gui.api.v1.Person personToApiPerson(
                    final Person person) {

                return person == null ? null : toApiPerson(person.getId());

            }

            @Override
            public io.vanillabp.cockpit.gui.api.v1.Person toApiPerson(
                    final String personId) {

                return users
                        .getUser(personId)
                        .map(this::userToApiPerson)
                        .orElse(null);

            }

            @Override
            public io.vanillabp.cockpit.gui.api.v1.Group groupToApiGroup(
                    final Group group) {

                return authorityToApiGroup(group.getId());

            }

            @Override
            public io.vanillabp.cockpit.gui.api.v1.Group authorityToApiGroup(
                    final String authority) {

                return new io.vanillabp.cockpit.gui.api.v1.Group()
                        .id(authority)
                        .display(authority);

            }

        };

    }

    @Bean
    public PersonAndGroupMapper personsAndGroupsOfTheModel() {

        return new PersonAndGroupMapper() {

            @Override
            public Person toModelPerson(
                    final String personId) {

                final var person = new Person();
                person.setId(personId);
                person.setFulltext(personId);
                person.setSort(personId);
                return person;

            }

            @Override
            public Person toModelPerson(
                    final UserDetails user) {

                final var person = new Person();
                person.setId(user.getId());
                person.setFulltext(user.getDisplay());
                person.setSort(user.getDisplay());
                return person;

            }

            @Override
            public Group toModelGroup(
                    final String groupId) {

                final var group = new Group();
                group.setId(groupId);
                group.setFulltext(groupId);
                group.setSort(groupId);
                return group;

            }

        };

    }

    @RestController
    @RequestMapping(path = "/gui/api/v1")
    static class TaskList extends AbstractUserTaskListGuiApiController {

        @Override
        protected UserTaskVisibility userTasksVisibleTo(
                final UserDetails currentUser) {

            return UserTaskVisibility.everythingTheUserMayWorkOn(currentUser);

        }

    }

    @RestController
    @RequestMapping(path = "/gui/api/v1")
    static class WorkflowList extends AbstractWorkflowListGuiApiController {

        @Override
        protected WorkflowVisibility workflowsVisibleTo(
                final UserDetails currentUser) {

            return WorkflowVisibility.workflowsAddressedTo(currentUser);

        }

    }

    @RestController
    @RequestMapping(path = "/gui/api/v1")
    static class ModuleList extends AbstractWorkflowModulesGuiApiController {

        @Override
        protected WorkflowModuleVisibility workflowModulesVisibleTo(
                final UserDetails currentUser) {

            return WorkflowModuleVisibility.modulesAddressedTo(currentUser);

        }

    }

}
