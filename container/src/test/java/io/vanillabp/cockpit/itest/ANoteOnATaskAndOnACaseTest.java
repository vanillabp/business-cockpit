package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The users keep one note per user task and per case. Everybody who sees the entry may change
 * it, no report of a workflow module touches it, and an ended entry keeps its note as it was.
 * The full-text search of the lists reads it, and the update stream tells the other tabs.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class ANoteOnATaskAndOnACaseTest extends ItestBase {

  private String moduleId;
  private String token;
  private String martin;
  private String petra;

  @BeforeEach
  void registerModuleAndLogin() {
    moduleId = unique("note-module");
    token = unique("token");
    registerWorkflowModule(moduleId, "http://localhost:65000");
    martin = loginToGui(USER_MARTIN);
    petra = loginToGui(USER_PETRA);
  }

  @Test
  void everybodyWhoSeesATaskMayWriteItsNote() {

    final var userTaskId = aTaskBothSee();

    final var written = guiPatch(martin, "/usertask/"
        + userTaskId
        + "/note", """
            { "text": "Customer calls back on Monday" }
            """);
    assertThat(written.statusCode()).isEqualTo(200);
    assertThat(json(written).read("$.note", String.class)).isEqualTo("Customer calls back on Monday");

    assertThat(guiPatch(petra, "/usertask/"
        + userTaskId
        + "/note", """
            { "text": "Customer called, waiting for documents" }
            """).statusCode()).isEqualTo(200);
    assertThat(noteOfTask(martin, userTaskId)).isEqualTo("Customer called, waiting for documents");

    assertThat(guiPatch(martin, "/usertask/"
        + userTaskId
        + "/note", """
            { "text": "" }
            """).statusCode()).isEqualTo(200);
    assertThat(noteOfTask(martin, userTaskId)).as("an empty text clears the note").isNull();

  }

  @Test
  void aTaskSomebodyMayNotSeeIsUnknownToThem() {

    final var userTaskId = reportUserTask(moduleId, token, unique("workflow"), """
        "assignee": "%s"
        """.formatted(USER_MARTIN));

    assertThat(guiPatch(petra, "/usertask/"
        + userTaskId
        + "/note", """
            { "text": "not mine" }
            """).statusCode()).isEqualTo(404);
    assertThat(noteOfTask(martin, userTaskId)).isNull();

  }

  @Test
  void anEndedTaskKeepsItsNote() {

    final var userTaskId = aTaskBothSee();
    guiPatch(martin, "/usertask/"
        + userTaskId
        + "/note", """
            { "text": "before the end" }
            """);
    reportUserTaskCompleted(moduleId, token, userTaskId);

    assertThat(guiPatch(martin, "/usertask/"
        + userTaskId
        + "/note", """
            { "text": "after the end" }
            """).statusCode()).isEqualTo(409);
    assertThat(noteOfTask(martin, userTaskId)).as("the note can still be read")
        .isEqualTo("before the end");

  }

  @Test
  void oneNoteForSeveralTasks() {

    final var first = aTaskBothSee();
    final var second = aTaskBothSee();

    assertThat(guiPatch(martin, "/usertask/note", """
        { "userTaskIds": [ "%s", "%s" ], "text": "checked in the weekly meeting" }
        """.formatted(first, second)).statusCode()).isEqualTo(200);
    assertThat(noteOfTask(martin, first)).isEqualTo("checked in the weekly meeting");
    assertThat(noteOfTask(martin, second)).isEqualTo("checked in the weekly meeting");

    reportUserTaskCompleted(moduleId, token, second);
    assertThat(guiPatch(martin, "/usertask/note", """
        { "userTaskIds": [ "%s", "%s" ], "text": "changed again" }
        """.formatted(first, second)).statusCode()).isEqualTo(409);
    assertThat(noteOfTask(martin, first)).as("none of the tasks is changed")
        .isEqualTo("checked in the weekly meeting");

  }

  /**
   * A change reported by the workflow module is laid onto the stored task and saved whole. The
   * note is not in the report, and it stays. The same holds for a creation which arrives after a
   * change created the task.
   */
  @Test
  void noReportOfTheWorkflowModuleTouchesTheNote() {

    final var userTaskId = unique("task");
    assertThat(bpmsV1_1("/usertask/"
        + userTaskId
        + "/updated", taskReport(userTaskId, "Do ride"))
        .statusCode()).isEqualTo(200);
    guiPatch(martin, "/usertask/"
        + userTaskId
        + "/note", """
            { "text": "kept by the cockpit" }
            """);

    assertThat(bpmsV1_1("/usertask/"
        + userTaskId
        + "/updated", taskReport(userTaskId, "Do ride now"))
        .statusCode()).isEqualTo(200);
    assertThat(bpmsV1_1("/usertask/created", taskReport(userTaskId, "Do ride"))
        .statusCode()).isEqualTo(200);

    final var task = json(guiGet(martin, "/usertask/"
        + userTaskId));
    assertThat(task.read("$.title.en", String.class)).isEqualTo("Do ride now");
    assertThat(task.read("$.note", String.class)).isEqualTo("kept by the cockpit");

  }

  @Test
  void theFullTextSearchFindsATaskByItsNote() {

    final var userTaskId = aTaskBothSee();
    final var word = unique("word");
    guiPatch(martin, "/usertask/"
        + userTaskId
        + "/note", """
            { "text": "remember %s" }
            """.formatted(word));

    assertThat(userTaskList(martin, word, "OpenTasksWithFollowUp")
        .read("$.userTasks[*].id", List.class))
        .containsExactly(userTaskId);

  }

  @Test
  void theOtherTabsLearnAboutANewNote() {

    final var userTaskId = aTaskBothSee();
    final var stream = openEventStream(petra);
    await().untilAsserted(() -> assertThat(stream).anyMatch(line -> line.contains("ping")));
    final var before = eventsAbout(userTaskId, stream);

    guiPatch(martin, "/usertask/"
        + userTaskId
        + "/note", """
            { "text": "for petra to see" }
            """);

    await().untilAsserted(() -> assertThat(eventsAbout(userTaskId, stream))
        .as("petra's stream so far: %s", stream)
        .isGreaterThan(before));

  }

  @Test
  void everybodyWhoSeesACaseMayWriteItsNote() {

    final var workflowId = aRunningCase();

    final var written = guiPatch(martin, "/workflow/"
        + workflowId
        + "/note", """
            { "text": "waiting for documents" }
            """);
    assertThat(written.statusCode()).isEqualTo(200);
    assertThat(json(written).read("$.note", String.class)).isEqualTo("waiting for documents");

    assertThat(guiPatch(martin, "/workflow/"
        + workflowId
        + "/note", null).statusCode())
        .isEqualTo(200);
    assertThat(noteOfCase(workflowId)).as("no body clears the note").isNull();

    assertThat(guiPatch(martin, "/workflow/"
        + unique("unknown")
        + "/note", """
            { "text": "nobody's" }
            """).statusCode()).isEqualTo(404);

  }

  @Test
  void anEndedCaseKeepsItsNote() {

    final var workflowId = aRunningCase();
    guiPatch(martin, "/workflow/"
        + workflowId
        + "/note", """
            { "text": "before the end" }
            """);
    assertThat(bpmsV1_1("/workflow/"
        + workflowId
        + "/completed", workflowReport(workflowId, "Ride"))
        .statusCode()).isEqualTo(200);

    assertThat(guiPatch(martin, "/workflow/"
        + workflowId
        + "/note", """
            { "text": "after the end" }
            """).statusCode()).isEqualTo(409);
    assertThat(noteOfCase(workflowId)).isEqualTo("before the end");

  }

  @Test
  void oneNoteForSeveralCasesAndNoReportTouchesIt() {

    final var first = aRunningCase();
    final var second = aRunningCase();

    assertThat(guiPatch(martin, "/workflow/note", """
        { "workflowIds": [ "%s", "%s" ], "text": "escalated" }
        """.formatted(first, second)).statusCode()).isEqualTo(200);
    assertThat(bpmsV1_1("/workflow/"
        + first
        + "/updated", workflowReport(first, "Ride, changed"))
        .statusCode()).isEqualTo(200);

    assertThat(noteOfCase(first)).isEqualTo("escalated");
    assertThat(noteOfCase(second)).isEqualTo("escalated");
    assertThat(json(guiGet(martin, "/workflow/"
        + first)).read("$.title.en", String.class))
        .isEqualTo("Ride, changed");

  }

  @Test
  void theFullTextSearchFindsACaseByItsNote() {

    final var workflowId = aRunningCase();
    final var word = unique("word");
    guiPatch(martin, "/workflow/"
        + workflowId
        + "/note", """
            { "text": "remember %s" }
            """.formatted(word));

    assertThat(workflowList(martin, word, "Active").read("$.workflows[*].id", List.class))
        .containsExactly(workflowId);

  }

  private String aTaskBothSee() {

    return reportUserTask(moduleId, token, unique("workflow"), """
        "candidateGroups": [ "%s", "%s" ]
        """.formatted(GROUP_OF_MARTIN, GROUP_OF_PETRA));

  }

  private String aRunningCase() {

    final var workflowId = unique("workflow");
    assertThat(bpmsV1_1("/workflow/created", workflowReport(workflowId, "Ride")).statusCode())
        .isEqualTo(200);
    return workflowId;

  }

  private String taskReport(
      final String userTaskId,
      final String title) {

    return """
        {
          "id": "%s",
          "userTaskId": "%s",
          "timestamp": "%s",
          "workflowModuleId": "%s",
          "bpmnProcessId": "taxi-ride",
          "title": { "en": "%s" },
          "taskDefinition": "do-ride",
          "uiUriPath": "/remoteEntry.js",
          "uiUriType": "WEBPACK_MF_REACT",
          "assignee": "%s",
          "detailsFulltextSearch": "%s"
        }
        """.formatted(unique("event"), userTaskId, isoNow(), moduleId, title, USER_MARTIN, token);

  }

  private String workflowReport(
      final String workflowId,
      final String title) {

    return """
        {
          "id": "%s",
          "workflowId": "%s",
          "timestamp": "%s",
          "workflowModuleId": "%s",
          "bpmnProcessId": "taxi-ride",
          "bpmnProcessVersion": "1",
          "initiator": "martin",
          "title": { "en": "%s" },
          "uiUriPath": "/remoteEntry.js",
          "uiUriType": "WEBPACK_MF_REACT",
          "detailsFulltextSearch": "%s"
        }
        """.formatted(unique("event"), workflowId, isoNow(), moduleId, title, token);

  }

  private String noteOfTask(
      final String cookie,
      final String userTaskId) {
    return json(guiGet(cookie, "/usertask/"
        + userTaskId)).read("$.note", String.class);
  }

  private String noteOfCase(
      final String workflowId) {
    return json(guiGet(martin, "/workflow/"
        + workflowId)).read("$.note", String.class);
  }

}
