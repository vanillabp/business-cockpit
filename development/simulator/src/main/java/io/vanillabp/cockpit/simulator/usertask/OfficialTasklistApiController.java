package io.vanillabp.cockpit.simulator.usertask;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devskiller.jfairy.Fairy;

import io.vanillabp.cockpit.gui.api.v1.Count;
import io.vanillabp.cockpit.gui.api.v1.Counts;
import io.vanillabp.cockpit.gui.api.v1.OfficialTasklistApi;
import io.vanillabp.cockpit.gui.api.v1.UserTask;
import io.vanillabp.cockpit.gui.api.v1.UserTaskCountsRequest;
import io.vanillabp.cockpit.gui.api.v1.UserTaskRetrieveMode;
import io.vanillabp.cockpit.simulator.common.FairyHelper;
import io.vanillabp.cockpit.simulator.usertask.testdata.UserTaskTestDataGenerator;

/**
 * A fake of the Business Cockpit's official task list API. It answers every task id with a random
 * user task and keeps it, so the next request for the same id returns the same task. It counts the
 * tasks it keeps. It has no list of its own.
 */
@RestController
@RequestMapping(path = "/official-api/v1")
public class OfficialTasklistApiController implements OfficialTasklistApi {

  /**
   * Creates the controller. Spring creates it, nobody else does.
   */
  public OfficialTasklistApiController() {
  }

  private Map<String, UserTask> userTasks = new HashMap<>();

  private final static Random random = new Random(System.currentTimeMillis());

  private final static Map<String, Fairy> fairies = new HashMap<>();

  static {
    fairies.put("de", FairyHelper.buildFairy("de"));
    fairies.put("en", FairyHelper.buildFairy("en"));
  }

  @Autowired
  private OfficialTasklistApiMapper mapper;

  @Override
  public ResponseEntity<UserTask> getUserTask(
      final String userTaskId,
      final Boolean markAsRead) {

    final var existingUserTask = userTasks.get(userTaskId);
    if (existingUserTask != null) {
      return ResponseEntity.ok(existingUserTask);
    }

    final var createdEvent = UserTaskTestDataGenerator
        .buildCreatedEvent(random, fairies, null, null, null);

    final var result = mapper.toApi(createdEvent);
    result.setId(userTaskId);

    userTasks.put(userTaskId, result);

    return ResponseEntity.ok(result);

  }

  /**
   * Counts the tasks the simulator keeps, for each filter by its mode. Like the cockpit, a filter
   * without a mode counts the open tasks. The simulator reads no search queries.
   */
  @Override
  public ResponseEntity<Counts> countUserTasks(
      final UserTaskCountsRequest userTaskCountsRequest,
      final OffsetDateTime initialTimestamp) {

    final var now = OffsetDateTime.now();
    final var counts = userTaskCountsRequest
        .getFilters()
        .stream()
        .map(filter -> new Count()
            .key(filter.getKey())
            .count(userTasks
                .values()
                .stream()
                .filter(userTask -> shows(filter.getMode(), userTask, now))
                .count()))
        .toList();

    return ResponseEntity.ok(new Counts()
        .serverTimestamp(initialTimestamp != null ? initialTimestamp : now)
        .counts(counts));

  }

  /**
   * Whether a list of the given mode holds the task, by the rules of the cockpit.
   *
   * @param mode The mode, or {@code null} for the open tasks
   * @param userTask The task
   * @param now The time to compare the follow-up date with
   * @return Whether the list holds the task
   */
  static boolean shows(
      final UserTaskRetrieveMode mode,
      final UserTask userTask,
      final OffsetDateTime now) {

    final var open = userTask.getEndedAt() == null;
    final var followUpDate = userTask.getFollowUpDate();
    final var followUpIsDue = (followUpDate == null) || !followUpDate.isAfter(now);
    return switch (mode == null ? UserTaskRetrieveMode.OPENTASKSWITHFOLLOWUP : mode) {
      case ALL -> true;
      case OPENTASKSWITHFOLLOWUP -> open;
      case OPENTASKSWITHOUTFOLLOWUP -> open && followUpIsDue;
      case OPENTASKONLYFOLLOWUP -> open && !followUpIsDue;
      case CLOSEDTASKSONLY -> !open;
    };

  }

}
