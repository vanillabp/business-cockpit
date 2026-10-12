package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.event.WorkflowEvent;
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

}
