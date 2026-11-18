package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.util.Collection;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.devshell.simulator.usermanagement.SignedInUser;
import io.vanillabp.cockpit.gui.api.v1.OfficialWorkflowModulesApi;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModule;
import io.vanillabp.cockpit.gui.api.v1.WorkflowModules;
import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * Answers which workflow modules the user signed in sees, {@code GET
 * /official-api/gui/api/v1/workflow-module}, the way the cockpit answers it at
 * {@code /gui/api/v1/workflow-module}. A cockpit loads the parts of a module which belong to no
 * case, for example a button in its header, from the modules of this list. A dev shell offers the
 * same modules on its start page, so a developer sees which user gets which part.
 */
@RestController
@RequestMapping("/official-api/gui/api/v1")
public class OfficialApiWorkflowModuleController implements OfficialWorkflowModulesApi {

  private final SignedInUser signedInUser;

  private final UserDirectoryService userDirectory;

  private final WorkflowModuleService workflowModuleService;

  /**
   * Creates the controller. Spring creates it, nobody else does.
   *
   * @param signedInUser Reads the user from the login cookie
   * @param userDirectory The made-up users, with their groups
   * @param workflowModuleService The registered workflow modules
   */
  public OfficialApiWorkflowModuleController(
      final SignedInUser signedInUser,
      final UserDirectoryService userDirectory,
      final WorkflowModuleService workflowModuleService) {

    this.signedInUser = signedInUser;
    this.userDirectory = userDirectory;
    this.workflowModuleService = workflowModuleService;

  }

  /**
   * The workflow modules the user signed in sees. If nobody is signed in, these are the modules
   * which registered no groups and so are open to everybody.
   *
   * @return The modules, sorted by their id
   */
  @Override
  public ResponseEntity<WorkflowModules> getWorkflowModules() {

    final var modules = workflowModuleService
        .getWorkflowModulesVisibleTo(groupsOfTheUserSignedIn())
        .stream()
        .map(OfficialApiWorkflowModuleController::toApi)
        .toList();

    return ResponseEntity.ok(new WorkflowModules().modules(modules));

  }

  /**
   * One workflow module, if the user signed in sees it. A module kept out of the list is unknown
   * here as well, as in the cockpit.
   *
   * @param workflowModuleId The id of the workflow module
   * @return The module, or 404 if there is none or the user does not see it
   */
  @Override
  public ResponseEntity<WorkflowModule> getWorkflowModule(
      final String workflowModuleId) {

    return workflowModuleService
        .getWorkflowModuleVisibleTo(groupsOfTheUserSignedIn(), workflowModuleId)
        .map(OfficialApiWorkflowModuleController::toApi)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());

  }

  private Collection<String> groupsOfTheUserSignedIn() {

    return signedInUser
        .id()
        .flatMap(userDirectory::getUser)
        .map(UserDetails::getAuthorities)
        .orElse(List.of());

  }

  /**
   * The path below which a user interface reaches a module, as the cockpit answers it. The
   * browser does not call the module itself, it calls {@code /wm/<id>/} and the proxy of the
   * cockpit or of the dev server passes the request on.
   *
   * @param workflowModule The stored workflow module
   * @return The path, or {@code null} if the module registered no URI
   */
  static String proxiedUri(
      final io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule workflowModule) {

    return workflowModule.getUri() == null
        ? null
        : "/wm/"
            + workflowModule.getId();

  }

  private static WorkflowModule toApi(
      final io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule workflowModule) {

    return new WorkflowModule()
        .id(workflowModule.getId())
        .version(workflowModule.getVersion())
        .uri(proxiedUri(workflowModule));

  }

}
