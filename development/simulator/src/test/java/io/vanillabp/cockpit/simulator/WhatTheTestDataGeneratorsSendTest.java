package io.vanillabp.cockpit.simulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.devskiller.jfairy.Fairy;

import io.vanillabp.cockpit.bpms.api.v1_1.BpmsApi;
import io.vanillabp.cockpit.simulator.common.FairyHelper;
import io.vanillabp.cockpit.simulator.usertask.testdata.UserTaskTestDataGenerator;
import io.vanillabp.cockpit.simulator.usertask.testdata.UserTaskTestDataParameters;
import io.vanillabp.cockpit.simulator.workflow.testdata.WorkflowTestDataGenerator;
import io.vanillabp.cockpit.simulator.workflow.testdata.WorkflowTestDataParameters;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Which reports the test data generators send. The client of the cockpit is a mock which records
 * every call, so nothing leaves the test.
 */
@ExtendWith(SuppressOutputExtension.class)
class WhatTheTestDataGeneratorsSendTest {

  private static Map<String, Fairy> fairies;

  @BeforeAll
  static void setUp() {

    fairies = Map.of("en", FairyHelper.buildFairy("en"));

  }

  private static UserTaskTestDataGenerator userTaskGenerator(
      final int offset,
      final int noOfEvents,
      final BpmsApi bpmsApi) {

    final var parameters = new UserTaskTestDataParameters();
    parameters.setPercentageUpdates(60);
    parameters.setThrottling(0);
    return new UserTaskTestDataGenerator(offset, noOfEvents, bpmsApi, new String[]{
        "anna", "bert"
    }, new String[]{
        "drivers", "dispatchers"
    }, fairies, parameters);

  }

  /**
   * The first generator of a form gets the offset 0. The seed used to be the clock times the
   * offset, which is 0 for it whatever the time, so it sent the same tasks in every run.
   */
  @Test
  void twoGeneratorsOfTheFirstPositionDrawDifferentNumbers() {

    final var first = (Random) ReflectionTestUtils.getField(userTaskGenerator(0, 1, null), "random");
    final var second = (Random) ReflectionTestUtils.getField(userTaskGenerator(0, 1, null), "random");
    assertThat(first.nextLong()).isNotEqualTo(second.nextLong());

    final var firstCases = (Random) ReflectionTestUtils.getField(
        new WorkflowTestDataGenerator(0, 1, null, fairies, new WorkflowTestDataParameters()), "random");
    final var secondCases = (Random) ReflectionTestUtils.getField(
        new WorkflowTestDataGenerator(0, 1, null, fairies, new WorkflowTestDataParameters()), "random");
    assertThat(firstCases.nextLong()).isNotEqualTo(secondCases.nextLong());

  }

  /**
   * The generator sends every kind of report about a user task, and none about a task after its
   * end. Sending only updates was a branch which could not be left, and the switch below it drew
   * from all numbers an int has, so it all but always activated.
   */
  @Test
  void theUserTaskGeneratorEndsSuspendsAndActivatesTasksAndSendsNothingAfterAnEnd() {

    final var calls = new ArrayList<List<Object>>();
    final var bpmsApi = mock(BpmsApi.class, Mockito.withSettings().defaultAnswer(invocation -> {
      final var call = new ArrayList<Object>();
      call.add(invocation.getMethod().getName());
      call.add(invocation.getArguments()[0]);
      calls.add(call);
      return null;
    }));

    userTaskGenerator(1, 600, bpmsApi).run();

    final var kinds = calls.stream().map(call -> call.get(0)).toList();
    assertThat(kinds).contains(
        "userTaskCreatedEvent",
        "userTaskUpdatedEvent",
        "userTaskCompletedEvent",
        "userTaskCancelledEvent",
        "userTaskSuspendedEvent",
        "userTaskActivatedEvent");

    final var ended = new HashSet<Object>();
    for (final var call : calls) {
      final var kind = call.get(0);
      if (kind.equals("registerWorkflowModule") || kind.equals("userTaskCreatedEvent")) {
        continue;
      }
      assertThat(ended).as("a report after the end of %s", call.get(1)).doesNotContain(call.get(1));
      if (kind.equals("userTaskCompletedEvent") || kind.equals("userTaskCancelledEvent")) {
        ended.add(call.get(1));
      }
    }

  }

}
