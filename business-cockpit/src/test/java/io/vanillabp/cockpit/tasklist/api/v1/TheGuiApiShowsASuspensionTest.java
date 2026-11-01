package io.vanillabp.cockpit.tasklist.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.api.v1.WorkflowListGuiApiMapperImpl;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What the GUI API answers about a suspended user task or case.
 * <p>
 * A user interface shows a suspended entry differently and lets nobody work on it. So the GUI API
 * carries {@code suspendedAt} while the entry is suspended and leaves it out otherwise, the way it
 * carries {@code endedAt}. A list can be sorted and filtered by it like by every other field.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheGuiApiShowsASuspensionTest {

  private static final OffsetDateTime SUSPENDED_AT = OffsetDateTime.parse("2026-11-01T09:00:00Z");

  private static <T> T withPersons(
      final T mapper,
      final Class<?> declaringClass) throws Exception {

    final var field = declaringClass.getDeclaredField("personAndGroupMapper");
    field.setAccessible(true);
    field.set(mapper, mock(PersonAndGroupApiMapper.class));
    return mapper;

  }

  @Test
  void aSuspendedTaskCarriesTheMomentOfItsSuspension() throws Exception {

    final var mapper = withPersons(new TasklistGuiApiMapperImpl(), GuiApiMapper.class);
    final var task = new UserTask();
    task.setId("task-1");
    task.setSuspendedAt(SUSPENDED_AT);
    task.setSuspensionChangedAt(SUSPENDED_AT);

    assertThat(mapper.toApi(task, "anna").getSuspendedAt()).isEqualTo(SUSPENDED_AT);

  }

  @Test
  void aTaskWhichIsNotSuspendedCarriesNoMoment() throws Exception {

    final var mapper = withPersons(new TasklistGuiApiMapperImpl(), GuiApiMapper.class);
    final var task = new UserTask();
    task.setId("task-1");
    // an activation leaves the moment it happened, which the user interface never needs
    task.setSuspensionChangedAt(SUSPENDED_AT);

    assertThat(mapper.toApi(task, "anna").getSuspendedAt()).isNull();

  }

  @Test
  void aSuspendedCaseCarriesTheMomentOfItsSuspension() throws Exception {

    final var mapper = withPersons(
        new WorkflowListGuiApiMapperImpl(),
        io.vanillabp.cockpit.workflowlist.api.v1.GuiApiMapper.class);
    final var workflow = new Workflow();
    workflow.setId("workflow-1");
    workflow.setSuspendedAt(SUSPENDED_AT);

    assertThat(mapper.toApi(workflow).getSuspendedAt()).isEqualTo(SUSPENDED_AT);

  }

  @Test
  void bothListsAreSortedAndFilteredByTheSuspension() {

    assertThat(UserTaskService.FIELDS_OF_THE_LIST).contains("suspendedAt");
    assertThat(WorkflowlistService.FIELDS_OF_THE_LIST).contains("suspendedAt");
    // the cockpit's own mark of the last report stays inside the cockpit
    assertThat(UserTaskService.FIELDS_OF_THE_LIST).doesNotContain("suspensionChangedAt");
    assertThat(WorkflowlistService.FIELDS_OF_THE_LIST).doesNotContain("suspensionChangedAt");

  }

}
