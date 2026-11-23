package io.vanillabp.cockpit.itest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A task without a candidate user, a candidate group or an assignee addresses nobody, so every
 * user sees it. Once a user claims it, it is theirs and the others do not see it. Once they give
 * it back, it addresses nobody again, and every user sees it again. The task stores whether it
 * addresses nobody, so giving it back has to write that value as well. This holds for one task
 * and for a list of tasks.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
class GivingBackATaskForNobodyShowsItToEverybodyTest extends ItestBase {

  private static final int DONE = HttpStatus.OK.value();

  private static final String EVERYTHING_OPEN = "OpenTasksWithFollowUp";

  private String moduleId;
  private String token;
  private String martin;
  private String petra;

  @BeforeEach
  void registerModuleAndLogin() {
    moduleId = unique("dangling-module");
    token = unique("token");
    registerWorkflowModule(moduleId, "http://localhost:65000");
    martin = loginToGui(USER_MARTIN);
    petra = loginToGui(USER_PETRA);
  }

  private String reportTaskForNobody() {

    return reportUserTask(moduleId, token, unique("workflow"), "\"candidateUsers\": []");

  }

  @SuppressWarnings("unchecked")
  private List<String> whatPetraSees() {

    return userTaskList(petra, token, EVERYTHING_OPEN).read("$.userTasks[*].id", List.class);

  }

  @Test
  void oneTaskGivenBackIsSeenByEverybodyAgain() {

    final var task = reportTaskForNobody();
    assertThat(whatPetraSees()).containsExactly(task);

    assertThat(guiPatch(martin, "/usertask/"
        + task
        + "/claim", null).statusCode()).isEqualTo(DONE);
    assertThat(whatPetraSees()).isEmpty();

    assertThat(guiPatch(martin, "/usertask/"
        + task
        + "/claim?unclaim=true", null).statusCode()).isEqualTo(DONE);
    assertThat(whatPetraSees()).containsExactly(task);

  }

  @Test
  void aListOfTasksGivenBackIsSeenByEverybodyAgain() {

    final var first = reportTaskForNobody();
    final var second = reportTaskForNobody();
    final var both = """
        { "userTaskIds": [ "%s", "%s" ] }
        """.formatted(first, second);
    assertThat(whatPetraSees()).containsExactlyInAnyOrder(first, second);

    assertThat(guiPatch(martin, "/usertask/claim", both).statusCode()).isEqualTo(DONE);
    assertThat(whatPetraSees()).isEmpty();

    assertThat(guiPatch(martin, "/usertask/claim?unclaim=true", both).statusCode()).isEqualTo(DONE);
    assertThat(whatPetraSees()).containsExactlyInAnyOrder(first, second);

  }

  @Test
  void aTaskForAGroupGivenBackStaysWithTheGroup() {

    final var task = reportUserTask(moduleId, token, unique("workflow"), """
        "candidateGroups": [ "%s" ]
        """.formatted(GROUP_OF_MARTIN));
    assertThat(whatPetraSees()).isEmpty();

    assertThat(guiPatch(martin, "/usertask/"
        + task
        + "/claim", null).statusCode()).isEqualTo(DONE);
    assertThat(guiPatch(martin, "/usertask/"
        + task
        + "/claim?unclaim=true", null).statusCode()).isEqualTo(DONE);

    assertThat(whatPetraSees()).isEmpty();
    assertThat(userTaskList(martin, token, EVERYTHING_OPEN).read("$.userTasks[*].id", List.class))
        .containsExactly(task);

  }

}
