package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Nobody may work on a user task whose process instance is suspended. Taking it, giving it back,
 * proposing it to somebody and taking a candidate off it are refused with 409, for one task and for
 * a list of ids alike, and nothing is written. A list which holds a suspended task among open ones
 * changes none of them. The body of the answer says why and names no user. Setting the follow-up
 * date, writing the note and marking as read still work. Once the task is activated again, the
 * work goes on.
 *
 * <p>The answers come in a fixed order: 404 for a task the user may not see, 409 for a task which
 * has ended, 409 for a task which is suspended, then 400 for giving back a task another user
 * claimed.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ASuspendedTaskRefusesWorkOnItTest extends ItestBase {

  private static final int THE_TASK_IS_IN_A_STATE_WHICH_REFUSES_IT = HttpStatus.CONFLICT.value();

  private static final int NOT_FOUND = HttpStatus.NOT_FOUND.value();

  private static final int DONE = HttpStatus.OK.value();

  private static final String SUSPENDED = "is suspended";

  private static final String ENDED = "has ended";

  private static final List<String> ACTIONS_ON_ONE_TASK = List.of(
      "/claim",
      "/claim?unclaim=true",
      "/assign?userId="
          + USER_PETRA,
      "/assign?unassign=true&userId="
          + USER_MARTIN);

  private static final List<String> ACTIONS_ON_A_LIST = List.of(
      "/usertask/claim",
      "/usertask/claim?unclaim=true",
      "/usertask/assign?userId="
          + USER_PETRA,
      "/usertask/assign?unassign=true&userId="
          + USER_MARTIN);

  private String moduleId;
  private String token;
  private String cookie;

  @BeforeEach
  void registerModuleAndLogin() {
    moduleId = unique("suspended-module");
    token = unique("token");
    registerWorkflowModule(moduleId, "http://localhost:65000");
    cookie = loginToGui(USER_MARTIN);
  }

  private String reportTaskClaimedBy(
      final String assignee) {

    return reportUserTask(moduleId, token, unique("workflow"), """
        "assignee": "%s", "candidateUsers": [ "martin", "petra" ]
        """.formatted(assignee));

  }

  private void report(
      final String userTaskId,
      final String what) {

    final var response = bpmsV1_1("/usertask/"
        + userTaskId
        + "/"
        + what, """
            {
              "id": "%s",
              "userTaskId": "%s",
              "timestamp": "%s",
              "initiator": "olivia"
            }
            """.formatted(unique("event"), userTaskId, isoNow()));
    assertThat(response.statusCode()).as(what).isEqualTo(DONE);

  }

  private String reportSuspendedTaskClaimedBy(
      final String assignee) {

    final var userTaskId = reportTaskClaimedBy(assignee);
    report(userTaskId, "suspended");
    return userTaskId;

  }

  private String stored(
      final String userTaskId) {

    final var response = guiGet(cookie, "/usertask/"
        + userTaskId);
    assertThat(response.statusCode()).isEqualTo(DONE);
    return response.body();

  }

  private HttpResponse<String> onOne(
      final String session,
      final String userTaskId,
      final String action) {

    return guiPatch(session, "/usertask/"
        + userTaskId
        + action, null);

  }

  private HttpResponse<String> onList(
      final String session,
      final String action,
      final String... userTaskIds) {

    return guiPatch(session, action, """
        { "userTaskIds": [ "%s" ] }
        """.formatted(String.join("\", \"", userTaskIds)));

  }

  @Test
  void everyActionOnOneSuspendedTaskIsAnsweredWith409WhichSaysWhyAndChangesNothing() {

    final var suspended = reportSuspendedTaskClaimedBy(USER_MARTIN);
    final var before = stored(suspended);

    for (final var action : ACTIONS_ON_ONE_TASK) {
      final var response = onOne(cookie, suspended, action);
      assertThat(response.statusCode()).as(action).isEqualTo(THE_TASK_IS_IN_A_STATE_WHICH_REFUSES_IT);
      assertThat(response.body())
          .as(action)
          .contains(suspended)
          .contains(SUSPENDED)
          .doesNotContain("olivia")
          .doesNotContain(USER_MARTIN);
      assertThat(stored(suspended)).as(action).isEqualTo(before);
    }

  }

  @Test
  void aListWithOneSuspendedTaskIsAnsweredWith409AndChangesNoTaskOfIt() {

    final var open = reportTaskClaimedBy(USER_MARTIN);
    final var suspended = reportSuspendedTaskClaimedBy(USER_MARTIN);
    final var openBefore = stored(open);
    final var suspendedBefore = stored(suspended);

    for (final var action : ACTIONS_ON_A_LIST) {
      final var response = onList(cookie, action, open, suspended);
      assertThat(response.statusCode()).as(action).isEqualTo(THE_TASK_IS_IN_A_STATE_WHICH_REFUSES_IT);
      assertThat(response.body()).as(action).contains(suspended).contains(SUSPENDED);
      assertThat(stored(open)).as(action).isEqualTo(openBefore);
      assertThat(stored(suspended)).as(action).isEqualTo(suspendedBefore);
    }

  }

  @Test
  void anEndedTaskComesBeforeASuspendedOne() {

    final var suspendedAndEnded = reportSuspendedTaskClaimedBy(USER_MARTIN);
    reportUserTaskCompleted(moduleId, token, suspendedAndEnded);
    final var suspended = reportSuspendedTaskClaimedBy(USER_MARTIN);

    for (final var action : ACTIONS_ON_ONE_TASK) {
      final var response = onOne(cookie, suspendedAndEnded, action);
      assertThat(response.statusCode()).as(action).isEqualTo(THE_TASK_IS_IN_A_STATE_WHICH_REFUSES_IT);
      assertThat(response.body()).as(action).contains(ENDED);
    }
    // the ended task decides, wherever it stands in the list
    for (final var action : ACTIONS_ON_A_LIST) {
      final var response = onList(cookie, action, suspended, suspendedAndEnded);
      assertThat(response.statusCode()).as(action).isEqualTo(THE_TASK_IS_IN_A_STATE_WHICH_REFUSES_IT);
      assertThat(response.body()).as(action).contains(suspendedAndEnded).contains(ENDED);
    }

  }

  @Test
  void aSuspendedTaskComesBeforeGivingBackATaskOfAnotherUser() {

    final var petrasSuspended = reportSuspendedTaskClaimedBy(USER_PETRA);
    final var petrasOpen = reportTaskClaimedBy(USER_PETRA);

    final var one = onOne(cookie, petrasSuspended, "/claim?unclaim=true");
    assertThat(one.statusCode()).isEqualTo(THE_TASK_IS_IN_A_STATE_WHICH_REFUSES_IT);
    assertThat(one.body()).contains(SUSPENDED).doesNotContain(USER_PETRA);

    final var list = onList(cookie, "/usertask/claim?unclaim=true", petrasOpen, petrasSuspended);
    assertThat(list.statusCode()).isEqualTo(THE_TASK_IS_IN_A_STATE_WHICH_REFUSES_IT);
    assertThat(list.body()).contains(petrasSuspended).contains(SUSPENDED);

    assertThat(json(guiGet(cookie, "/usertask/"
        + petrasOpen)).read("$.assignee.id", String.class)).isEqualTo(USER_PETRA);

  }

  @Test
  void aSuspendedTaskTheUserMayNotSeeIsAnsweredWith404() {

    final var hidden = reportUserTask(moduleId, token, unique("workflow"), """
        "assignee": "petra", "candidateUsers": [ "petra" ]
        """);
    report(hidden, "suspended");

    for (final var action : ACTIONS_ON_ONE_TASK) {
      assertThat(onOne(cookie, hidden, action).statusCode()).as(action).isEqualTo(NOT_FOUND);
    }
    // a list leaves out what the user may not see, so nothing is left to refuse
    for (final var action : ACTIONS_ON_A_LIST) {
      assertThat(onList(cookie, action, hidden).statusCode()).as(action).isEqualTo(DONE);
    }

  }

  @Test
  void theFollowUpDateTheNoteAndTheReadMarkerOfASuspendedTaskStillChange() {

    final var suspended = reportSuspendedTaskClaimedBy(USER_MARTIN);

    assertThat(guiPatch(cookie, "/usertask/"
        + suspended
        + "/follow-up-date", """
            { "timestamp": "2027-01-04T08:00:00Z" }
            """).statusCode()).isEqualTo(DONE);
    assertThat(json(guiGet(cookie, "/usertask/"
        + suspended)).read("$.followUpDate", String.class)).isNotNull();

    assertThat(guiPatch(cookie, "/usertask/"
        + suspended
        + "/note", """
            { "text": "Waits for the customer" }
            """).statusCode()).isEqualTo(DONE);
    assertThat(guiPatch(cookie, "/usertask/note", """
        { "userTaskIds": [ "%s" ], "text": "Still waits for the customer" }
        """.formatted(suspended)).statusCode()).isEqualTo(DONE);
    assertThat(json(guiGet(cookie, "/usertask/"
        + suspended)).read("$.note", String.class)).isEqualTo("Still waits for the customer");

    assertThat(guiPatch(cookie, "/usertask/"
        + suspended
        + "/mark-as-read", null).statusCode()).isEqualTo(DONE);
    assertThat(json(guiGet(cookie, "/usertask/"
        + suspended)).read("$.read", String.class)).isNotNull();
    assertThat(onList(cookie, "/usertask/mark-as-read?unread=true", suspended).statusCode())
        .isEqualTo(DONE);
    assertThat(json(guiGet(cookie, "/usertask/"
        + suspended)).read("$.read", String.class)).isNull();

  }

  @Test
  void anActivatedTaskTakesTheWorkAgain() {

    final var task = reportSuspendedTaskClaimedBy(USER_MARTIN);
    report(task, "activated");

    assertThat(onOne(cookie, task, "/assign?userId="
        + USER_PETRA).statusCode()).isEqualTo(DONE);
    assertThat(onList(cookie, "/usertask/claim?unclaim=true", task).statusCode()).isEqualTo(DONE);
    assertThat(json(guiGet(cookie, "/usertask/"
        + task)).read("$.assignee", Object.class)).isNull();
    assertThat(onOne(cookie, task, "/claim").statusCode()).isEqualTo(DONE);
    assertThat(json(guiGet(cookie, "/usertask/"
        + task)).read("$.assignee.id", String.class)).isEqualTo(USER_MARTIN);

  }

}
