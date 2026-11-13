package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask;

/**
 * What only the simulator answers, and no cockpit does. A workflow module reports more about a user
 * task than the official API shows, for example who must not work on it, who may see it although
 * he is no candidate, and how notifications are delivered. The cockpit uses these values itself
 * and has no reason to send them to a user interface. A developer still wants to see what his
 * module reports, so the simulator shows it here.
 */
@RestController
@RequestMapping("/simulator-api")
public class SimulatorApiController {

  @Autowired
  private TaskService taskService;

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
  @GetMapping(value = "/usertask/{userTaskId}/report", produces = "application/json")
  public ResponseEntity<Map<String, Object>> getLastReportOfUserTask(
      @PathVariable("userTaskId") final String userTaskId) {

    return taskService
        .getUserTask(userTaskId)
        .map(UserTask::getLastReport)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());

  }

}
