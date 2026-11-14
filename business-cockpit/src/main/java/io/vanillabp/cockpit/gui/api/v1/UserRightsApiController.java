package io.vanillabp.cockpit.gui.api.v1;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.support.security.BcUnauthorizedException;
import io.vanillabp.cockpit.support.security.jwt.JwtAuthenticationToken;
import io.vanillabp.cockpit.support.security.usercontext.UserContext;
import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;
import io.vanillabp.cockpit.workflowmodules.GroupHierarchyService;

/**
 * Tells the user signed in where each of their groups and rights comes from: from the
 * organisation, which gives groups in the user directory, or from an entry of a workflow module's
 * group hierarchy. The cockpit uses the merged hierarchies for every permission, so without this
 * answer a user only sees the result and not the reason.
 * <p>
 * Only the user signed in is answered. The path names no user, so nobody can ask about somebody
 * else.
 */
@RestController
@RequestMapping(path = "/gui/api/v1")
public class UserRightsApiController implements UserRightsApi {

  private final UserContext userContext;

  private final GroupHierarchyService groupHierarchyService;

  private final ObjectProvider<UserDirectoryService> userDirectory;

  /**
   * Creates the controller.
   *
   * @param userContext           Says who is signed in and which groups they have
   * @param groupHierarchyService Holds the hierarchy of every workflow module
   * @param userDirectory         Gives the groups of the organisation, if the application has a
   *                              user directory
   */
  public UserRightsApiController(
      final UserContext userContext,
      final GroupHierarchyService groupHierarchyService,
      final ObjectProvider<UserDirectoryService> userDirectory) {

    this.userContext = userContext;
    this.groupHierarchyService = groupHierarchyService;
    this.userDirectory = userDirectory;

  }

  @Override
  public ResponseEntity<GroupHierarchyOfUser> getGroupHierarchyOfCurrentUser() {

    final var user = userContext.getUserLoggedInDetails();
    if (user == null) {
      throw new BcUnauthorizedException("No user signed in to ask for their group hierarchy");
    }

    // the authorities are what every permission check of the cockpit uses, so they decide
    // which entries the user has
    final Set<String> groupsOfTheUser = Set.copyOf(user.getAuthorities());
    final var givenByOrganisation = groupsGivenByTheOrganisation(user);

    final var modules = groupHierarchyService
        .getGroupHierarchies()
        .entrySet()
        .stream()
        .filter(module -> (module.getValue() != null) && !module.getValue().isEmpty())
        .map(module -> new GroupHierarchyOfWorkflowModule()
            .workflowModuleId(module.getKey())
            .entries(entriesOfTheUser(module.getValue(), groupsOfTheUser, givenByOrganisation)))
        .toList();

    return ResponseEntity.ok(new GroupHierarchyOfUser()
        .givenByOrganisation(givenByOrganisation.stream().sorted().toList())
        .modules(modules));

  }

  private static List<GroupHierarchyEntryOfUser> entriesOfTheUser(
      final Map<String, Collection<String>> hierarchy,
      final Set<String> groupsOfTheUser,
      final Set<String> givenByOrganisation) {

    return hierarchy
        .entrySet()
        .stream()
        .filter(entry -> groupsOfTheUser.contains(entry.getKey()))
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> new GroupHierarchyEntryOfUser()
            .group(entry.getKey())
            .givenByOrganisation(givenByOrganisation.contains(entry.getKey()))
            .targets(Optional
                .ofNullable(entry.getValue())
                .stream()
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .toList()))
        .toList();

  }

  /**
   * The groups the user directory gives the user, before any hierarchy is applied. The cockpit's
   * own sign-in keeps the directory's answer with the authentication of the request, so the
   * directory is asked again only for a sign-in an application wrote itself. Without a directory
   * the cockpit cannot tell, and the answer says that no group comes from the organisation.
   */
  private Set<String> groupsGivenByTheOrganisation(
      final UserDetails user) {

    final var authentication = SecurityContextHolder.getContext().getAuthentication();
    final Optional<UserDetails> directoryUser;
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
      directoryUser = jwtAuthentication.getDirectoryUser();
    } else {
      directoryUser = Optional
          .ofNullable(userDirectory.getIfAvailable())
          .flatMap(directory -> directory.getUser(user.getId()));
    }
    return directoryUser
        .map(UserDetails::getAuthorities)
        .map(Set::copyOf)
        .orElse(Set.of());

  }

}
