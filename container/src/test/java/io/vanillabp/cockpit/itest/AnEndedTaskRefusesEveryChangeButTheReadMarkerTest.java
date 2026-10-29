package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A user task which has ended is documentation. Taking it over, giving it back, proposing it to
 * somebody and taking a candidate off it are refused with 409, for one task and for a list of
 * ids alike, and nothing is written. A list which holds an ended task among open ones changes
 * none of them. Marking as read is the one change an ended task still takes, because it only
 * switches off the bold row in the list. A task the user may not see is answered with 404 first,
 * whether it ended or not.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class AnEndedTaskRefusesEveryChangeButTheReadMarkerTest extends ItestBase {

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
    moduleId = unique("ride-module");
    token = unique("token");
    registerWorkflowModule(moduleId, "http://localhost:65000");
    cookie = loginToGui(USER_MARTIN);
  }

  private String reportMartinsTask() {

    return reportUserTask(moduleId, token, unique("workflow"), """
        "assignee": "martin", "candidateUsers": [ "martin" ]
        """);

  }

  private String reportMartinsEndedTask() {

    final var userTaskId = reportMartinsTask();
    reportUserTaskCompleted(moduleId, token, userTaskId);
    return userTaskId;

  }

  private String stored(
      final String userTaskId) {

    final var response = guiGet(cookie, "/usertask/"
        + userTaskId);
    assertThat(response.statusCode()).isEqualTo(200);
    return response.body();

  }

  @Test
  void everyActionOnOneEndedTaskIsAnsweredWith409AndChangesNothing() {

    final var ended = reportMartinsEndedTask();
    final var before = stored(ended);

    for (final var action : ACTIONS_ON_ONE_TASK) {
      final var response = guiPatch(cookie, "/usertask/"
          + ended
          + action, null);
      assertThat(response.statusCode()).as(action).isEqualTo(409);
      assertThat(stored(ended)).as(action).isEqualTo(before);
    }

  }

  @Test
  void aListWithOneEndedTaskIsAnsweredWith409AndChangesNoTaskOfIt() {

    final var open = reportUserTask(moduleId, token, unique("workflow"), """
        "candidateUsers": [ "martin" ]
        """);
    final var ended = reportMartinsEndedTask();
    final var openBefore = stored(open);
    final var endedBefore = stored(ended);

    for (final var action : ACTIONS_ON_A_LIST) {
      final var response = guiPatch(cookie, action, """
          { "userTaskIds": [ "%s", "%s" ] }
          """.formatted(open, ended));
      assertThat(response.statusCode()).as(action).isEqualTo(409);
      assertThat(stored(open)).as(action).isEqualTo(openBefore);
      assertThat(stored(ended)).as(action).isEqualTo(endedBefore);
    }

  }

  @Test
  void aListOfOpenTasksIsStillChanged() {

    final var open = reportUserTask(moduleId, token, unique("workflow"), """
        "candidateUsers": [ "martin" ]
        """);

    assertThat(guiPatch(cookie, "/usertask/claim", """
        { "userTaskIds": [ "%s" ] }
        """.formatted(open)).statusCode()).isEqualTo(200);

    assertThat(json(guiGet(cookie, "/usertask/"
        + open)).read("$.assignee.id", String.class)).isEqualTo(USER_MARTIN);

  }

  @Test
  void anEndedTaskTheUserMayNotSeeIsAnsweredWith404() {

    final var ended = reportMartinsEndedTask();
    final var petra = loginToGui(USER_PETRA);

    for (final var action : ACTIONS_ON_ONE_TASK) {
      assertThat(guiPatch(petra, "/usertask/"
          + ended
          + action, null).statusCode()).as(action).isEqualTo(404);
    }
    // a list leaves out what the user may not see, so nothing is left to refuse
    for (final var action : ACTIONS_ON_A_LIST) {
      assertThat(guiPatch(petra, action, """
          { "userTaskIds": [ "%s" ] }
          """.formatted(ended)).statusCode()).as(action).isEqualTo(200);
    }
    assertThat(json(guiGet(cookie, "/usertask/"
        + ended)).read("$.assignee.id", String.class)).isEqualTo(USER_MARTIN);

  }

  @Test
  void anEndedTaskIsStillMarkedAsReadAndUnread() {

    final var one = reportMartinsEndedTask();
    final var other = reportMartinsEndedTask();

    assertThat(guiPatch(cookie, "/usertask/"
        + one
        + "/mark-as-read", null).statusCode()).isEqualTo(200);
    assertThat(json(guiGet(cookie, "/usertask/"
        + one)).read("$.read", String.class)).isNotNull();

    assertThat(guiPatch(cookie, "/usertask/"
        + one
        + "/mark-as-read?unread=true", null).statusCode()).isEqualTo(200);
    assertThat(json(guiGet(cookie, "/usertask/"
        + one)).read("$.read", String.class)).isNull();

    assertThat(guiPatch(cookie, "/usertask/mark-as-read", """
        { "userTaskIds": [ "%s" ] }
        """.formatted(other)).statusCode()).isEqualTo(200);
    assertThat(json(guiGet(cookie, "/usertask/"
        + other)).read("$.read", String.class)).isNotNull();

  }

  @Test
  void openingAnEndedTaskMarksItAsRead() {

    final var ended = reportMartinsEndedTask();

    final var response = guiGet(cookie, "/usertask/"
        + ended
        + "?markAsRead=true");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(json(response).read("$.read", String.class)).isNotNull();

  }

  /**
   * A refused action writes nothing, so the change stream has nothing to report. To prove that
   * no event comes, a new task is reported after the refused actions. Once its event has arrived,
   * an event about the ended task would have arrived as well, because both go through the same
   * ticks in the order they were written. A task reported before the refused actions marks the
   * point from which on the events are counted.
   */
  @Test
  void aRefusedActionSendsNoEventToTheUpdateStream() {

    final var ended = reportMartinsEndedTask();
    final var lines = openEventStream(cookie);
    await().untilAsserted(() -> assertThat(lines).anyMatch(line -> line.contains("ping")));

    final var before = reportMartinsTask();
    await().untilAsserted(() -> assertThat(eventsAbout(before, lines)).isPositive());
    final var eventsAboutTheEndedTask = eventsAbout(ended, lines);

    for (final var action : ACTIONS_ON_ONE_TASK) {
      assertThat(guiPatch(cookie, "/usertask/"
          + ended
          + action, null).statusCode()).as(action).isEqualTo(409);
    }
    for (final var action : ACTIONS_ON_A_LIST) {
      assertThat(guiPatch(cookie, action, """
          { "userTaskIds": [ "%s" ] }
          """.formatted(ended)).statusCode()).as(action).isEqualTo(409);
    }

    final var after = reportMartinsTask();
    await().untilAsserted(() -> assertThat(eventsAbout(after, lines)).isPositive());
    assertThat(eventsAbout(ended, lines)).isEqualTo(eventsAboutTheEndedTask);

  }

}
