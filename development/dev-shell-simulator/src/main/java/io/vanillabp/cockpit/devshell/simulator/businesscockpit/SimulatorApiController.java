package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.devshell.simulator.api.SimulatorApi;
import io.vanillabp.cockpit.devshell.simulator.api.VisibleWorkflowModule;
import io.vanillabp.cockpit.devshell.simulator.api.VisibleWorkflowModules;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowModule;
import io.vanillabp.cockpit.support.security.usercontext.UserDetails;
import io.vanillabp.cockpit.support.security.usercontext.UserDirectoryService;

/**
 * What only the simulator answers, and no cockpit does. A workflow module reports more about a user
 * task than the official API shows, for example who must not work on it, who may see it although
 * he is no candidate, and how notifications are delivered. The cockpit uses these values itself
 * and has no reason to send them to a user interface. A developer still wants to see what his
 * module reports, so the simulator shows it here. The same goes for the workflow modules a user
 * sees: a test names the user instead of signing him in. The path and the answer are described in the
 * simulator's own OpenAPI file, {@code dev-shell-simulator-api.yaml}.
 */
@RestController
public class SimulatorApiController implements SimulatorApi {

  @Autowired
  private TaskService taskService;

  @Autowired
  private WorkflowModuleService workflowModuleService;

  @Autowired
  private UserDirectoryService userDirectory;

  /**
   * Creates the controller. Spring creates it, nobody else does.
   */
  public SimulatorApiController() {
  }

  /**
   * The last report about a user task which created, updated or ended it, as the workflow module
   * sent it. A suspension or an activation does not replace it.
   *
   * @param userTaskId The id of the user task
   * @return The report as JSON, or 404 if the simulator holds no user task of this id
   */
  @Override
  public ResponseEntity<Map<String, Object>> getLastReportOfUserTask(
      final String userTaskId) {

    return taskService
        .getUserTask(userTaskId)
        .map(UserTask::getLastReport)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());

  }

  /**
   * The workflow modules a user sees, the same list the official GUI API answers at
   * {@code /official-api/gui/api/v1/workflow-module} when this user is signed in.
   *
   * @param user The id of the user
   * @return The modules, sorted by their id, or 404 if the simulator does not know the user
   */
  @Override
  public ResponseEntity<VisibleWorkflowModules> getWorkflowModulesOfUser(
      final String user) {

    return Optional
        .ofNullable(user)
        .flatMap(userDirectory::getUser)
        .map(UserDetails::getAuthorities)
        .map(workflowModuleService::getWorkflowModulesVisibleTo)
        .map(modules -> modules
            .stream()
            .map(SimulatorApiController::toApi)
            .toList())
        .map(modules -> new VisibleWorkflowModules().modules(modules))
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());

  }

  private static VisibleWorkflowModule toApi(
      final WorkflowModule workflowModule) {

    return new VisibleWorkflowModule()
        .id(workflowModule.getId())
        .version(workflowModule.getVersion())
        .uri(OfficialApiWorkflowModuleController.proxiedUri(workflowModule));

  }

}
