package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTask;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.UserTaskRepository;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.Workflow;
import io.vanillabp.cockpit.devshell.simulator.businesscockpit.model.WorkflowRepository;
import io.vanillabp.cockpit.gui.api.v1.Count;
import io.vanillabp.cockpit.gui.api.v1.UserTaskCountFilter;
import io.vanillabp.cockpit.gui.api.v1.UserTaskCountsRequest;
import io.vanillabp.cockpit.gui.api.v1.UserTaskRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.UserTasksRequest;
import io.vanillabp.cockpit.gui.api.v1.WorkflowCountFilter;
import io.vanillabp.cockpit.gui.api.v1.WorkflowCountsRequest;
import io.vanillabp.cockpit.gui.api.v1.WorkflowRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.WorkflowsRequest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Which user tasks and workflows the lists of the simulator show for each mode, and what its two
 * count endpoints count. The simulator boots with its in-memory database, and the controllers of
 * the official API are called directly, past the login.
 *
 * <p>Without a mode the simulator shows everything, ended or not. The cockpit shows the open
 * entries then. The difference is on purpose, see decision 70 in the repository's DECISIONS.md.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:what-the-simulator-lists",
    // the simulator does not start without a user to log in as
    "dev-shell-simulator.users[0].id=john", "dev-shell-simulator.users[0].groups[0]=ADMIN"
})
class WhatTheSimulatorListsForItsModeTest {

  private static final String WORKFLOW = "the-workflow";

  @Autowired
  private OfficialApiTaskController tasks;

  @Autowired
  private OfficialApiWorkflowController workflows;

  @Autowired
  private UserTaskRepository userTaskRepository;

  @Autowired
  private WorkflowRepository workflowRepository;

  @BeforeEach
  void storeOneEntryOfEachKind() {

    final var now = OffsetDateTime.now();
    userTaskRepository.saveAll(List.of(
        userTask("open", null, null),
        userTask("followed-up", null, now.plusDays(3)),
        userTask("follow-up-due", null, now.minusDays(1)),
        userTask("ended", now.minusMinutes(5), null),
        userTask("ended-while-followed-up", now.minusMinutes(5), now.plusDays(3))));
    workflowRepository.saveAll(List.of(
        workflow("running", "ride-1", null),
        workflow("running-too", "ride-2", null),
        workflow("completed", "ride-1", now.minusMinutes(5))));

  }

  @AfterEach
  void removeTheEntries() {

    userTaskRepository.deleteAll();
    workflowRepository.deleteAll();

  }

  static Stream<Arguments> userTaskModes() {

    return Stream.of(
        Arguments.of(UserTaskRetrieveMode.ALL,
            List.of("open", "followed-up", "follow-up-due", "ended", "ended-while-followed-up")),
        Arguments.of(UserTaskRetrieveMode.OPENTASKSWITHFOLLOWUP,
            List.of("open", "followed-up", "follow-up-due")),
        Arguments.of(UserTaskRetrieveMode.OPENTASKSWITHOUTFOLLOWUP,
            List.of("open", "follow-up-due")),
        Arguments.of(UserTaskRetrieveMode.OPENTASKONLYFOLLOWUP,
            List.of("followed-up")),
        Arguments.of(UserTaskRetrieveMode.CLOSEDTASKSONLY,
            List.of("ended", "ended-while-followed-up")),
        Arguments.of(null,
            List.of("open", "followed-up", "follow-up-due", "ended", "ended-while-followed-up")));

  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("userTaskModes")
  void theListOfUserTasksShowsWhatItsModeSays(
      final UserTaskRetrieveMode mode,
      final List<String> expected) {

    final var list = tasks
        .getUserTasks(new UserTasksRequest().mode(mode).pageSize(50), null)
        .getBody();

    assertThat(list.getUserTasks())
        .extracting(io.vanillabp.cockpit.gui.api.v1.UserTask::getId)
        .containsExactlyInAnyOrderElementsOf(expected);
    assertThat(list.getPage().getTotalElements()).isEqualTo(expected.size());

    final var ofTheWorkflow = workflows
        .getUserTasksOfWorkflow(WORKFLOW, true, new UserTasksRequest().mode(mode).pageSize(50))
        .getBody();
    assertThat(ofTheWorkflow)
        .extracting(io.vanillabp.cockpit.gui.api.v1.UserTask::getId)
        .containsExactlyInAnyOrderElementsOf(expected);

  }

  static Stream<Arguments> workflowModes() {

    return Stream.of(
        Arguments.of(WorkflowRetrieveMode.ALL, List.of("running", "running-too", "completed")),
        Arguments.of(WorkflowRetrieveMode.INPROGRESS, List.of("running", "running-too")),
        Arguments.of(WorkflowRetrieveMode.DONE, List.of("completed")),
        Arguments.of(WorkflowRetrieveMode.ACTIVE, List.of("running", "running-too")),
        Arguments.of(WorkflowRetrieveMode.INACTIVE, List.of("completed")),
        Arguments.of(null, List.of("running", "running-too", "completed")));

  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("workflowModes")
  void theListOfWorkflowsShowsWhatItsModeSays(
      final WorkflowRetrieveMode mode,
      final List<String> expected) {

    final var list = workflows
        .getWorkflows(new WorkflowsRequest().mode(mode).pageSize(50), null, null)
        .getBody();

    assertThat(list.getWorkflows())
        .extracting(io.vanillabp.cockpit.gui.api.v1.Workflow::getId)
        .containsExactlyInAnyOrderElementsOf(expected);
    assertThat(list.getPage().getTotalElements()).isEqualTo(expected.size());

  }

  @Test
  void theCountOfUserTasksIsTheTotalOfTheListOfEachMode() {

    final var filters = new ArrayList<UserTaskCountFilter>();
    userTaskModes().forEach(arguments -> filters.add(new UserTaskCountFilter()
        .key(String.valueOf(arguments.get()[0]))
        .mode((UserTaskRetrieveMode) arguments.get()[0])));

    final var counts = tasks
        .countUserTasks(new UserTaskCountsRequest().filters(filters), null)
        .getBody();

    assertThat(counts.getServerTimestamp()).isNotNull();
    assertThat(counts.getCounts())
        .extracting(Count::getKey, Count::getCount)
        .containsExactly(
            tuple("All", 5L),
            tuple("OpenTasksWithFollowUp", 3L),
            tuple("OpenTasksWithoutFollowUp", 2L),
            tuple("OpenTaskOnlyFollowUp", 1L),
            tuple("ClosedTasksOnly", 2L),
            tuple("null", 5L));

  }

  @Test
  void theCountOfWorkflowsIsTheTotalOfTheListOfEachModeAndBusinessId() {

    final var filters = new ArrayList<WorkflowCountFilter>();
    workflowModes().forEach(arguments -> filters.add(new WorkflowCountFilter()
        .key(String.valueOf(arguments.get()[0]))
        .mode((WorkflowRetrieveMode) arguments.get()[0])));
    filters.add(new WorkflowCountFilter()
        .key("ride-1")
        .businessIds(List.of("ride-1")));

    final var counts = workflows
        .countWorkflows(new WorkflowCountsRequest().filters(filters), null)
        .getBody();

    assertThat(counts.getServerTimestamp()).isNotNull();
    assertThat(counts.getCounts())
        .extracting(Count::getKey, Count::getCount)
        .containsExactly(
            tuple("All", 3L),
            tuple("InProgress", 2L),
            tuple("Done", 1L),
            tuple("Active", 2L),
            tuple("Inactive", 1L),
            tuple("null", 3L),
            tuple("ride-1", 2L));

  }

  private static UserTask userTask(
      final String id,
      final OffsetDateTime endedAt,
      final OffsetDateTime followUpDate) {

    final var userTask = new UserTask();
    userTask.setId(id);
    userTask.setWorkflowId(WORKFLOW);
    userTask.setCreatedAt(OffsetDateTime.now());
    userTask.setEndedAt(endedAt);
    userTask.setFollowUpDate(followUpDate);
    return userTask;

  }

  private static Workflow workflow(
      final String id,
      final String businessId,
      final OffsetDateTime endedAt) {

    final var workflow = new Workflow();
    workflow.setId(id);
    workflow.setBusinessId(businessId);
    workflow.setCreatedAt(OffsetDateTime.now());
    workflow.setEndedAt(endedAt);
    return workflow;

  }

}
