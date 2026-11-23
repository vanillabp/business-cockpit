package io.vanillabp.cockpit.bpms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A task which addresses nobody takes who sees it from the first later report which names
 * somebody. An assign in the cockpit makes a person a candidate of such a task. That must not take
 * this right away from the workflow module: a later report which names people still decides who
 * sees the task, and the person of the assign is no longer a candidate then. Without a report which
 * names somebody, the assign stays.
 */
@ExtendWith(SuppressOutputExtension.class)
class AReportWinsOverAnAssignOfAnUnaddressedTaskTest {

  private static UserTask anUnaddressedTaskAssignedInTheCockpit() {

    final var task = new UserTask();
    task.setId("task-1");
    final var alice = new Person();
    alice.setId("alice");
    task.addCandidatePerson(alice);
    return task;

  }

  private static Group group(
      final String id) {

    final var group = new Group();
    group.setId(id);
    return group;

  }

  @Test
  void aLaterReportWhichNamesAGroupDecidesWhoSeesTheTask() {

    final var stored = anUnaddressedTaskAssignedInTheCockpit();

    WhoSeesAUserTask.keepWhoSeesTheStoredTask(stored, "change",
        report -> report.setCandidateGroups(new ArrayList<>(List.of(group("CLERK")))));

    assertThat(stored.getCandidateGroups()).extracting(Group::getId).containsExactly("CLERK");
    assertThat(stored.getCandidateUsers()).isNullOrEmpty();

  }

  @Test
  void theReportAfterThatKeepsWhoSeesTheTaskAsUsual() {

    final var stored = anUnaddressedTaskAssignedInTheCockpit();
    WhoSeesAUserTask.keepWhoSeesTheStoredTask(stored, "change",
        report -> report.setCandidateGroups(new ArrayList<>(List.of(group("CLERK")))));

    WhoSeesAUserTask.keepWhoSeesTheStoredTask(stored, "change",
        report -> report.setCandidateGroups(new ArrayList<>(List.of(group("MANAGER")))));

    assertThat(stored.getCandidateGroups()).extracting(Group::getId).containsExactly("CLERK");

  }

  /** The positive twin: a report which names nobody leaves the assign as it is. */
  @Test
  void aLaterReportWhichNamesNobodyKeepsTheAssign() {

    final var stored = anUnaddressedTaskAssignedInTheCockpit();

    WhoSeesAUserTask.keepWhoSeesTheStoredTask(stored, "change", report -> {
    });

    assertThat(stored.getCandidateUsers()).extracting(Person::getId).containsExactly("alice");

  }

  /** A task which a report addressed keeps who sees it, whatever an assign added later. */
  @Test
  void anAssignOfAnAddressedTaskIsKept() {

    final var stored = new UserTask();
    stored.setId("task-1");
    stored.setCandidateGroups(new ArrayList<>(List.of(group("CLERK"))));
    final var alice = new Person();
    alice.setId("alice");
    stored.addCandidatePerson(alice);

    WhoSeesAUserTask.keepWhoSeesTheStoredTask(stored, "change",
        report -> report.setCandidateGroups(new ArrayList<>(List.of(group("MANAGER")))));

    assertThat(stored.getCandidateGroups()).extracting(Group::getId).containsExactly("CLERK");
    assertThat(stored.getCandidateUsers()).extracting(Person::getId).containsExactly("alice");

  }

}
