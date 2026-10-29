package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Which tasks and workflows a list shows for each mode, and for a request which names no mode.
 * Without a mode a list of tasks shows the open ones, with or without a follow-up date, and a
 * list of workflows shows the active ones. That holds for the first page, for the update of a
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
