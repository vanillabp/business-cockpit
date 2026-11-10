package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.vanillabp.cockpit.gui.api.v1.Count;
import io.vanillabp.cockpit.gui.api.v1.Counts;
import io.vanillabp.cockpit.gui.api.v1.NoteRequest;
import io.vanillabp.cockpit.gui.api.v1.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskCountsRequest;
import io.vanillabp.cockpit.gui.api.v1.UserTasks;
import io.vanillabp.cockpit.gui.api.v1.UserTasksNoteRequest;
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
   * Reads one user task. Like the cockpit, the simulator answers 404 for an id it does not hold.
   *
   * @param userTaskId The id of the user task
   * @param markAsRead Ignored, the simulator keeps no read marks
   * @return The user task, or 404 if there is none of that id
   */
  @Override
  public ResponseEntity<UserTask> getUserTask(
      final String userTaskId,
      final Boolean markAsRead) {

    final var userTaskFound = taskService.getUserTask(userTaskId);
    if (userTaskFound.isEmpty()) {
      log.info("Client asked for UserTask {}, which the simulator does not hold", userTaskId);
      return ResponseEntity.notFound().build();
    }

    log.info("Client retrieved UserTask {}", userTaskId);

    return ResponseEntity.ok(mapper.toApi(userTaskFound.get()));

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

  /**
   * Sets the note of a user task the way the cockpit does: 404 for an unknown task and 409 for
   * an ended one.
   */
  @Override
  public ResponseEntity<UserTask> setUserTaskNote(
      final String userTaskId,
      final NoteRequest noteRequest) {

    try {
      final var userTask = taskService.setNote(
          userTaskId, noteRequest == null ? null : noteRequest.getText());
      return userTask == null
          ? ResponseEntity.notFound().build()
          : ResponseEntity.ok(mapper.toApi(userTask));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

  }

  /**
   * Sets the same note on several user tasks. Unlike the cockpit, the simulator changes the open
   * tasks before it meets an ended one, which is good enough for a developer's machine.
   */
  @Override
  public ResponseEntity<Void> setUserTasksNote(
      final UserTasksNoteRequest userTasksNoteRequest) {

    try {
      userTasksNoteRequest
          .getUserTaskIds()
          .forEach(userTaskId -> taskService.setNote(userTaskId, userTasksNoteRequest.getText()));
      return ResponseEntity.ok().build();
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

  }

}
