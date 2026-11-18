package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Only the assignee can give a user task back. Giving back a task which another user claimed is
 * answered with 400, for one task and for a list of ids alike, and nothing is written. A list which
 * holds such a task among the user's own tasks changes none of them. The body of the answer says
 * why, and so does the body of the 409 for an ended task. The answers come in a fixed order: 404 for
 * a task the user may not see, then 409 for a task which has ended, then 400.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class GivingBackSomebodyElsesTaskIsRefusedTest extends ItestBase {

  private static final int SOMEBODY_ELSE_CLAIMED_IT = HttpStatus.BAD_REQUEST.value();

  private static final int THE_TASK_HAS_ENDED = HttpStatus.CONFLICT.value();

  private static final int NOT_FOUND = HttpStatus.NOT_FOUND.value();

  private static final int DONE = HttpStatus.OK.value();

  private static final String UNCLAIM = "/claim?unclaim=true";

  private String moduleId;
  private String token;
  private String cookie;

  @BeforeEach
  void registerModuleAndLogin() {
    moduleId = unique("unclaim-module");
    token = unique("token");
    registerWorkflowModule(moduleId, "http://localhost:65000");
    cookie = loginToGui(USER_MARTIN);
  }

  /** A task both Martin and Petra may work on, claimed by the given user or by nobody. */
  private String reportTaskClaimedBy(
      final String assignee) {

    final var assigneeField = assignee == null
        ? ""
        : "\"assignee\": \"%s\", ".formatted(assignee);
    return reportUserTask(moduleId, token, unique("workflow"), assigneeField
        + "\"candidateUsers\": [ \"martin\", \"petra\" ]");

  }

  private String stored(
      final String userTaskId) {

    final var response = guiGet(cookie, "/usertask/"
        + userTaskId);
    assertThat(response.statusCode()).isEqualTo(DONE);
    return response.body();

  }

  private String assigneeOf(
      final String userTaskId) {

    return json(guiGet(cookie, "/usertask/"
        + userTaskId)).read("$.assignee.id", String.class);

  }

  private HttpResponse<String> unclaimOne(
      final String userTaskId) {

    return guiPatch(cookie, "/usertask/"
        + userTaskId
        + UNCLAIM, null);

  }

  private HttpResponse<String> unclaimList(
      final String... userTaskIds) {

    return guiPatch(cookie, "/usertask"
        + UNCLAIM, """
            { "userTaskIds": [ "%s" ] }
            """.formatted(String.join("\", \"", userTaskIds)));

  }

  @Test
  void givingBackOneTaskOfAnotherUserIsAnsweredWith400AndChangesNothing() {

    final var petras = reportTaskClaimedBy(USER_PETRA);
    final var before = stored(petras);

    final var response = unclaimOne(petras);

    assertThat(response.statusCode()).isEqualTo(SOMEBODY_ELSE_CLAIMED_IT);
    assertThat(response.body())
        .contains(petras)
        .contains("claimed by another user")
        .doesNotContain(USER_PETRA);
    assertThat(stored(petras)).isEqualTo(before);

  }

  @Test
  void aListWithOneTaskOfAnotherUserIsAnsweredWith400AndChangesNoTaskOfIt() {

    final var martins = reportTaskClaimedBy(USER_MARTIN);
    final var petras = reportTaskClaimedBy(USER_PETRA);
    final var martinsBefore = stored(martins);
    final var petrasBefore = stored(petras);

    final var response = unclaimList(martins, petras);

    assertThat(response.statusCode()).isEqualTo(SOMEBODY_ELSE_CLAIMED_IT);
    assertThat(response.body()).contains(petras).contains("claimed by another user");
    assertThat(stored(martins)).isEqualTo(martinsBefore);
    assertThat(stored(petras)).isEqualTo(petrasBefore);

  }

  @Test
  void anEndedTaskOfAnotherUserIsAnsweredWith409AndTheBodySaysWhy() {

    final var petras = reportTaskClaimedBy(USER_PETRA);
    reportUserTaskCompleted(moduleId, token, petras);
    final var before = stored(petras);

    final var one = unclaimOne(petras);
    assertThat(one.statusCode()).isEqualTo(THE_TASK_HAS_ENDED);
    assertThat(one.body()).contains(petras).contains("has ended");

    // a list with an ended task and a task of another user: the ended task decides
    final var petrasOpen = reportTaskClaimedBy(USER_PETRA);
    final var list = unclaimList(petrasOpen, petras);
    assertThat(list.statusCode()).isEqualTo(THE_TASK_HAS_ENDED);
    assertThat(list.body()).contains(petras).contains("has ended");

    assertThat(stored(petras)).isEqualTo(before);
    assertThat(assigneeOf(petrasOpen)).isEqualTo(USER_PETRA);

  }

  @Test
  void aTaskOfAnotherUserWhichTheUserMayNotSeeIsAnsweredWith404() {

    final var hidden = reportUserTask(moduleId, token, unique("workflow"), """
        "assignee": "petra", "candidateUsers": [ "petra" ]
        """);

    assertThat(unclaimOne(hidden).statusCode()).isEqualTo(NOT_FOUND);
    // a list leaves out what the user may not see, so nothing is left to refuse
    assertThat(unclaimList(hidden).statusCode()).isEqualTo(DONE);

    final var petra = loginToGui(USER_PETRA);
    assertThat(json(guiGet(petra, "/usertask/"
        + hidden)).read("$.assignee.id", String.class)).isEqualTo(USER_PETRA);

  }

  @Test
  void givingBackOwnTasksAndTasksWithoutAssigneeStillWorks() {

    final var martins = reportTaskClaimedBy(USER_MARTIN);
    final var nobodys = reportTaskClaimedBy(null);

    assertThat(unclaimOne(martins).statusCode()).isEqualTo(DONE);
    assertThat(unclaimOne(nobodys).statusCode()).isEqualTo(DONE);
    assertThat(json(guiGet(cookie, "/usertask/"
        + martins)).read("$.assignee", Object.class)).isNull();

    final var martinsAgain = reportTaskClaimedBy(USER_MARTIN);
    assertThat(unclaimList(martinsAgain, nobodys).statusCode()).isEqualTo(DONE);
    assertThat(json(guiGet(cookie, "/usertask/"
        + martinsAgain)).read("$.assignee", Object.class)).isNull();

  }

}
