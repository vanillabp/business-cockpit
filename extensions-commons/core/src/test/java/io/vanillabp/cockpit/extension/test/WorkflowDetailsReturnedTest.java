package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What a details provider returns as an object of its own.
 * <p>
 * Business code may set who may open a workflow, so the event takes these lists over like every
 * other value business code may set.
 */
@ExtendWith(SuppressOutputExtension.class)
public class WorkflowDetailsReturnedTest {

  @Test
  @DisplayName("The event reads its id from the workflow id")
  public void theIdIsTheWorkflowId() {

    final var event = EventFixture.workflow(WorkflowEventKind.CREATED);

    assertEquals("workflow-1", event.getId());

  }

  @Test
  @DisplayName("Who may open the workflow is taken over from the details a provider returned")
  public void whoMayOpenTheWorkflowIsTakenOver() {

    final var event = EventFixture.workflow(WorkflowEventKind.CREATED);
    final var returned = new WorkflowEvent(WorkflowEventKind.CREATED);
    returned.setAccessibleToUsers(List.of("bert"));
    returned.setAccessibleToGroups(List.of("dispatch"));

    event.applyReturnedDetails(returned);

    assertEquals(List.of("bert"), event.getAccessibleToUsers());
    assertEquals(List.of("dispatch"), event.getAccessibleToGroups());
    assertEquals("workflow-1", event.getId());

  }

  @Test
  @DisplayName("Details of its own without a comment keep the comment the BPMS prefilled")
  public void detailsWithoutACommentKeepThePrefilledOne() {

    final var event = EventFixture.workflow(WorkflowEventKind.CANCELLED);
    event.setComment("the customer withdrew");

    event.applyReturnedDetails(new WorkflowEvent(WorkflowEventKind.CANCELLED));

    assertEquals("the customer withdrew", event.getComment());

  }

  @Test
  @DisplayName("A comment in details of its own replaces the comment the BPMS prefilled")
  public void aCommentOfTheModuleWins() {

    final var event = EventFixture.workflow(WorkflowEventKind.CANCELLED);
    event.setComment("the customer withdrew");
    final var returned = new WorkflowEvent(WorkflowEventKind.CANCELLED);
    returned.setComment("withdrawn by phone");

    event.applyReturnedDetails(returned);

    assertEquals("withdrawn by phone", event.getComment());

  }

  @Test
  @DisplayName("User-task details of its own without a comment keep the comment the BPMS prefilled")
  public void userTaskDetailsWithoutACommentKeepThePrefilledOne() {

    final var event = EventFixture.userTask(UserTaskEventKind.CANCELED);
    event.setComment("the customer withdrew");

    event.applyReturnedDetails(new UserTaskEvent(UserTaskEventKind.CANCELED));

    assertEquals("the customer withdrew", event.getComment());

    final var returned = new UserTaskEvent(UserTaskEventKind.CANCELED);
    returned.setComment("withdrawn by phone");
    event.applyReturnedDetails(returned);

    assertEquals("withdrawn by phone", event.getComment());

  }

}
