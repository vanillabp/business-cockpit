package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.devshell.simulator.usermanagement.SignedInUser;
import io.vanillabp.cockpit.gui.api.v1.Group;
import io.vanillabp.cockpit.gui.api.v1.OfficialCurrentUserApi;
import io.vanillabp.cockpit.gui.api.v1.User;
import io.vanillabp.cockpit.support.security.usercontext.GroupHierarchyResolver;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * Answers who is signed in, {@code GET /official-api/gui/api/v1/app/current-user}, the way the cockpit
 * answers it at {@code /gui/api/v1/app/current-user}. A workflow module asks it with the generated
 * client of the official GUI API and the user's cookie, for example to tell whether another user
 * claimed a task. So it gets the same answer from the simulator as from the cockpit.
 */
@RestController
@RequestMapping("/official-api/gui/api/v1")
public class OfficialApiCurrentUserController implements OfficialCurrentUserApi {

  private final SignedInUser signedInUser;

  private final UserDirectoryService userDirectory;

  private final WorkflowModuleService workflowModuleService;

  /**
   * Creates the controller. Spring creates it, nobody else does.
   *
   * @param signedInUser Reads the user from the login cookie
   * @param userDirectory The made-up users, with their names and groups
   * @param workflowModuleService The registered workflow modules, with their group hierarchies
   */
  public OfficialApiCurrentUserController(
      final SignedInUser signedInUser,
      final UserDirectoryService userDirectory,
      final WorkflowModuleService workflowModuleService) {

    this.signedInUser = signedInUser;
    this.userDirectory = userDirectory;
    this.workflowModuleService = workflowModuleService;

  }

  /**
   * The user signed in, with the groups of the configuration completed by the group hierarchies
   * of all registered workflow modules, as the cockpit does it. Like the cockpit, the simulator
   * answers 200 without a body if nobody is signed in. A user who is no longer in the
   * configuration counts as nobody.
   *
   * @return The user signed in, or no body
   */
  @Override
  public ResponseEntity<User> currentUser() {

    final var userFound = signedInUser
        .id()
        .flatMap(userDirectory::getUser);
    if (userFound.isEmpty()) {
      return ResponseEntity.ok().build();
    }

    final var user = userFound.get();
    final var groups = GroupHierarchyResolver
        .resolveGroups(workflowModuleService.getGroupHierarchies(), user.getAuthorities())
        .stream()
        // the cockpit of the module container shows a group by its id, and so does the simulator
        .map(group -> new Group()
            .id(group)
            .display(group))
        .toList();

    return ResponseEntity.ok(new User()
        .id(user.getId())
        .email(user.getEmail())
        .display(user.getDisplay())
        .displayShort(user.getDisplayShort())
        .groups(groups));

  }

}
