package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Which tasks and workflows a list shows for each mode, and for a request which names no mode.
 * Without a mode a list of tasks shows the open ones, with or without a follow-up date, and a
 * list of workflows shows the ones in progress. That holds for the first page, for the update of a
 * list and for the tasks of one workflow alike.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class WhatAListShowsForItsModeTest extends ItestBase {

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

  @Test
  void aListOfTasksWithoutAModeLeavesOutTheEndedTasks() {

    final var workflowId = unique("workflow");
    final var open = reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\"");
    final var ended = reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\"");
    reportUserTaskCompleted(moduleId, token, ended);

    final var firstPage = guiPost(cookie, "/usertask", """
        { "pageSize": 50, "searchQueries": [ { "query": "%s" } ] }
        """.formatted(token));
    assertThat(firstPage.statusCode()).isEqualTo(200);
    assertThat(json(firstPage).read("$.userTasks[*].id", List.class)).containsExactly(open);

    final var update = guiPut(cookie, "/usertask", """
        { "size": 50, "knownUserTasksIds": [], "searchQueries": [ { "query": "%s" } ] }
        """.formatted(token));
    assertThat(update.statusCode()).isEqualTo(200);
    assertThat(json(update).read("$.userTasks[*].id", List.class)).containsExactly(open);

    assertThat(userTaskList(cookie, token, "All").read("$.userTasks[*].id", List.class))
        .containsExactlyInAnyOrder(open, ended);

  }

  @Test
  void aTaskWhichEndedAfterTheTimestampIsOpenAndNotClosed() throws InterruptedException {

    final var workflowId = unique("workflow");
    final var endedBefore = reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\"");
    reportUserTaskCompleted(moduleId, token, endedBefore);
    // MongoDB keeps milliseconds, so the timestamp has no more, and a pause on either side keeps
    // both completions off the millisecond of the timestamp
    Thread.sleep(10);
    final var timestamp = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
    Thread.sleep(10);
    final var endedAfter = reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\"");
    final var open = reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\"");
    assertEndedBefore(endedBefore, timestamp);
    reportUserTaskCompleted(moduleId, token, endedAfter);

    final var expected = Map.of(
        "OpenTasksWithFollowUp", List.of(endedAfter, open),
        "ClosedTasksOnly", List.of(endedBefore),
        "All", List.of(endedBefore, endedAfter, open));

    expected.forEach((
        mode,
        tasks) -> {
      final var list = guiPost(cookie, "/usertask?initialTimestamp="
          + timestamp, """
              { "pageSize": 50, "mode": "%s", "searchQueries": [ { "query": "%s" } ] }
              """.formatted(mode, token));
      assertThat(list.statusCode()).as(mode).isEqualTo(200);
      assertThat(json(list).read("$.userTasks[*].id", List.class))
          .as(mode)
          .containsExactlyInAnyOrderElementsOf(tasks);
      final var count = guiPost(cookie, "/usertask/count?initialTimestamp="
          + timestamp, """
              { "filters": [ { "key": "k", "mode": "%s", "searchQueries": [ { "query": "%s" } ] } ] }
              """.formatted(mode, token));
      assertThat(count.statusCode()).as(mode).isEqualTo(200);
      assertThat(json(count).read("$.counts[0].count", Long.class)).as(mode).isEqualTo(tasks.size());
    });

  }

  @Test
  void theUpdateWithTheTimestampOfTheFirstPageKeepsATaskWhichEndedInBetween() {

    final var workflowId = unique("workflow");
    final var endsLater = reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\"");
    final var open = reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\"");
    final var filter = "\"searchQueries\": [ { \"query\": \"%s\" } ]".formatted(token);

    // the first page sends no timestamp and gets the time of the server back
    final var firstPage = guiPost(cookie, "/usertask", "{ \"pageSize\": 50, %s }".formatted(filter));
    assertThat(firstPage.statusCode()).isEqualTo(200);
    assertThat(json(firstPage).read("$.userTasks[*].id", List.class)).containsExactlyInAnyOrder(endsLater, open);
    final var timestamp = json(firstPage).read("$.serverTimestamp", String.class);
    assertThat(timestamp).isNotNull();

    reportUserTaskCompleted(moduleId, token, endsLater);

    final var update = guiPut(cookie, "/usertask?initialTimestamp="
        + URLEncoder.encode(timestamp, StandardCharsets.UTF_8),
        "{ \"size\": 50, \"knownUserTasksIds\": [], %s }".formatted(filter));
    assertThat(update.statusCode()).isEqualTo(200);
    assertThat(json(update).read("$.userTasks[*].id", List.class)).containsExactlyInAnyOrder(endsLater, open);
    assertThat(json(update).read("$.page.totalElements", Long.class)).isEqualTo(2L);

    final var newList = guiPut(cookie, "/usertask", "{ \"size\": 50, \"knownUserTasksIds\": [], %s }"
        .formatted(filter));
    assertThat(newList.statusCode()).isEqualTo(200);
    assertThat(json(newList).read("$.userTasks[*].id", List.class)).containsExactly(open);

  }

  /**
   * The test needs the first completion stored before the timestamp. This check makes sure it is,
   * so a red test always means the cockpit and never the clock of the test.
   */
  private void assertEndedBefore(
      final String endedBefore,
      final OffsetDateTime timestamp) {

    final var closed = guiPost(cookie, "/usertask?initialTimestamp="
        + timestamp, """
            { "pageSize": 50, "mode": "All", "searchQueries": [ { "query": "%s" } ] }
            """.formatted(token));
    assertThat(json(closed).read("$.userTasks[?(@.id == '%s')].endedAt".formatted(endedBefore), List.class))
        .singleElement()
        .satisfies(endedAt -> assertThat(OffsetDateTime.parse(endedAt.toString())).isBefore(timestamp));

  }

  @Test
  void aListOfWorkflowsWithoutAModeLeavesOutTheEndedWorkflows() {

    final var running = unique("workflow");
    final var ended = unique("workflow");
    reportWorkflow(moduleId, token, running, "created");
    reportWorkflow(moduleId, token, ended, "created");
    reportWorkflow(moduleId, token, ended, "completed");

    final var firstPage = guiPost(cookie, "/workflow", """
        { "pageSize": 50, "searchQueries": [ { "query": "%s" } ] }
        """.formatted(token));
    assertThat(firstPage.statusCode()).isEqualTo(200);
    assertThat(json(firstPage).read("$.workflows[*].id", List.class)).containsExactly(running);

    final var update = guiPut(cookie, "/workflow", """
        { "size": 50, "knownWorkflowsIds": [], "searchQueries": [ { "query": "%s" } ] }
        """.formatted(token));
    assertThat(update.statusCode()).isEqualTo(200);
    assertThat(json(update).read("$.workflows[*].id", List.class)).containsExactly(running);

    assertThat(workflowList(cookie, token, "All").read("$.workflows[*].id", List.class))
        .containsExactlyInAnyOrder(running, ended);

  }

  @Test
  void everyModeOfTheWorkflowsGivesItsOwnListAndTheOldNamesGiveTheSameOnes() {

    final var running = unique("workflow");
    final var suspended = unique("workflow");
    final var ended = unique("workflow");
    reportWorkflow(moduleId, token, running, "created");
    reportWorkflow(moduleId, token, suspended, "created");
    reportWorkflow(moduleId, token, suspended, "suspended");
    reportWorkflow(moduleId, token, ended, "created");
    reportWorkflow(moduleId, token, ended, "completed");

    final var expected = Map.of(
        "All", List.of(running, suspended, ended),
        "InProgress", List.of(running, suspended),
        "Done", List.of(ended),
        "Active", List.of(running, suspended),
        "Inactive", List.of(ended));

    expected.forEach((
        mode,
        workflows) -> assertThat(workflowList(cookie, token, mode)
            .read("$.workflows[*].id", List.class))
            .as(mode)
            .containsExactlyInAnyOrderElementsOf(workflows));

  }

  @Test
  void anUnknownModeOfTheWorkflowsIsAnsweredWith400WhichNamesTheModesThereAre() {

    final var response = guiPost(cookie, "/workflow", """
        { "pageSize": 1, "mode": "Suspended" }
        """);

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.body()).isEqualTo("The request is not valid: 'mode' takes one of 'All', "
        + "'InProgress', 'Done', 'Active', 'Inactive'.");

  }

  @Test
  void theModeOpenTasksIsGoneAndIsAnsweredWith400WhichNamesTheModesThereAre() {

    final var response = guiPost(cookie, "/usertask", """
        { "pageSize": 1, "mode": "OpenTasks" }
        """);

    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(response.body()).isEqualTo("The request is not valid: 'mode' takes one of 'All', "
        + "'OpenTasksWithoutFollowUp', 'OpenTasksWithFollowUp', 'OpenTaskOnlyFollowUp', "
        + "'ClosedTasksOnly'.");

  }

  @Test
  void everyModeOfTheTasksOfAWorkflowGivesItsOwnList() {

    final var workflowId = unique("workflow");
    reportWorkflow(moduleId, token, workflowId, "created");
    final var open = reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\"");
    final var followedUp = reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\"");
    final var ended = reportUserTask(moduleId, token, workflowId, "\"assignee\": \"martin\"");
    reportUserTaskCompleted(moduleId, token, ended);
    assertThat(guiPatch(cookie, "/usertask/"
        + followedUp
        + "/follow-up-date", """
            { "timestamp": "%s" }
            """.formatted(OffsetDateTime.now().plusDays(3))).statusCode()).isEqualTo(200);

    final var expected = Map.of(
        "\"mode\": \"All\"", List.of(open, followedUp, ended),
        "\"mode\": \"OpenTasksWithFollowUp\"", List.of(open, followedUp),
        "\"mode\": \"OpenTasksWithoutFollowUp\"", List.of(open),
        "\"mode\": \"OpenTaskOnlyFollowUp\"", List.of(followedUp),
        "\"mode\": \"ClosedTasksOnly\"", List.of(ended),
        "\"pageSize\": 50", List.of(open, followedUp));

    expected.forEach((
        mode,
        tasks) -> {
      final var response = guiPost(cookie, "/workflow/"
          + workflowId
          + "/usertasks?llatcup=true", "{ %s }".formatted(mode));
      assertThat(response.statusCode()).as(mode).isEqualTo(200);
      assertThat(json(response).read("$[*].id", List.class))
          .as(mode)
          .containsExactlyInAnyOrderElementsOf(tasks);
    });

  }

}
