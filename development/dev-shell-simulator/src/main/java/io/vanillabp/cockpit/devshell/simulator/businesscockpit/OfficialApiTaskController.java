package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.gui.api.v1.Count;
import io.vanillabp.cockpit.gui.api.v1.Counts;
import io.vanillabp.cockpit.gui.api.v1.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskCountsRequest;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;

/**
 * REST controller for the official task list API.
 * Allows retrieving UserTasks.
 */
@RestController
@RequestMapping("/official-api/v1")
public class OfficialApiTaskController implements OfficialTasklistApi {

  private final Logger log = LoggerFactory.getLogger(OfficialApiTaskController.class);

  private final TaskService taskService;

  private final OfficialApiMapper mapper;

  /**
   * Creates the controller. Spring creates it, nobody else does.
   *
   * @param taskService Keeps the user tasks
   * @param mapper Converts the user tasks for the official API
   */
  public OfficialApiTaskController(
      final TaskService taskService,
      final OfficialApiMapper mapper) {

    this.taskService = taskService;
    this.mapper = mapper;

  }

  /**
   * Retrieves a UserTask by its ID.
   *
   * @param userTaskId The ID of the UserTask to retrieve.
   * @return A ResponseEntity containing the retrieved UserTask or an error response.
   */
  @Override
  public ResponseEntity<UserTask> getUserTask(
      final String userTaskId,
      final Boolean markAsRead) {

    final var userTaskFound = taskService.getUserTask(userTaskId);
    final var apiUserTask = mapper.toApi(userTaskFound);

    log.info("Client retrieved UserTask {}", userTaskId);

    return ResponseEntity.ok(apiUserTask);

  }

  @Override
  public ResponseEntity<UserTasks> getUserTasks(
      final UserTasksRequest userTasksRequest,
      final OffsetDateTime initialTimestamp) {

    final var userTasks = taskService.getUserTasks(
        mapper.toModel(userTasksRequest.getMode()),
        userTasksRequest.getSort(),
        userTasksRequest.getSortAscending(),
        userTasksRequest.getPageNumber(),
        userTasksRequest.getPageSize());
    final var apiUserTasks = mapper.toUserTasksApi(userTasks);

    log.info("Client retrieved UserTasks, total: {}", userTasks.getTotalElements());

    return ResponseEntity.ok(apiUserTasks);
  }

  /**
   * Counts the user tasks of each filter, with the same modes as the list. Like the list, the
   * simulator reads no search queries.
   */
  @Override
  public ResponseEntity<Counts> countUserTasks(
      final UserTaskCountsRequest userTaskCountsRequest,
      final OffsetDateTime initialTimestamp) {

    final var counts = userTaskCountsRequest
        .getFilters()
        .stream()
        .map(filter -> new Count()
            .key(filter.getKey())
            .count(taskService.countUserTasks(mapper.toModel(filter.getMode()))))
        .toList();

    return ResponseEntity.ok(new Counts()
        .serverTimestamp(initialTimestamp != null ? initialTimestamp : OffsetDateTime.now())
        .counts(counts));

  }

}
