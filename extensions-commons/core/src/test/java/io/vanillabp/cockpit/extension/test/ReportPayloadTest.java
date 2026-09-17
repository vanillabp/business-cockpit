package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.event.ReportPayload;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * What a report survives on its way through the outbox entry it travels with.
 * <p>
 * The report is built at the event and sent when the entry is dispatched, so everything the
 * cockpit is told passes through these bytes. A field which does not survive them is a field
 * the cockpit never hears about.
 */
@ExtendWith(SuppressOutputExtension.class)
public class ReportPayloadTest {

  @Test
  @DisplayName("Every field of a user-task report comes back")
  public void aUserTaskReportComesBack() {

    final var event = EventFixture.userTask(UserTaskEventKind.CREATED);

    final var read = ReportPayload.userTaskEvent(ReportPayload.of(event));

    assertEquals(UserTaskEventKind.CREATED, read.getEventKind());
    assertEquals(event.getEventId(), read.getEventId());
    assertEquals(event.getUserTaskId(), read.getUserTaskId());
    assertEquals(event.getTimestamp(), read.getTimestamp());
    assertEquals(event.getSource(), read.getSource());
    assertEquals(event.getInitiator(), read.getInitiator());
    assertEquals(event.getComment(), read.getComment());
    assertEquals(event.getWorkflowModuleId(), read.getWorkflowModuleId());
    assertEquals(event.getBpmnProcessId(), read.getBpmnProcessId());
    assertEquals(event.getBpmnProcessVersion(), read.getBpmnProcessVersion());
    assertEquals(event.getWorkflowId(), read.getWorkflowId());
    assertEquals(event.getSubWorkflowId(), read.getSubWorkflowId());
    assertEquals(event.getBusinessId(), read.getBusinessId());
    assertEquals(event.getBpmnTaskId(), read.getBpmnTaskId());
    assertEquals(event.getTaskDefinition(), read.getTaskDefinition());
    assertEquals(event.getWorkflowTitle(), read.getWorkflowTitle());
    assertEquals(event.getTitle(), read.getTitle());
    assertEquals(event.getTaskDefinitionTitle(), read.getTaskDefinitionTitle());
    assertEquals(event.getUiUriPath(), read.getUiUriPath());
    assertEquals(event.getUiUriType(), read.getUiUriType());
    assertEquals(event.getAssignee(), read.getAssignee());
    assertEquals(event.getCandidateUsers(), read.getCandidateUsers());
    assertEquals(event.getCandidateGroups(), read.getCandidateGroups());
    assertEquals(event.getExcludedCandidateUsers(), read.getExcludedCandidateUsers());
    assertEquals(event.getAdmittedUsers(), read.getAdmittedUsers());
    assertEquals(event.getDueDate(), read.getDueDate());
    assertEquals(event.getFollowUpDate(), read.getFollowUpDate());
    assertEquals(event.getDetailsFulltextSearch(), read.getDetailsFulltextSearch());
    assertEquals(event.getNotificationDelivery(), read.getNotificationDelivery());
    assertEquals(event.getI18nLanguages(), read.getI18nLanguages());

  }

  @Test
  @DisplayName("Every field of a workflow report comes back")
  public void aWorkflowReportComesBack() {

    final var event = EventFixture.workflow(WorkflowEventKind.UPDATED);

    final var read = ReportPayload.workflowEvent(ReportPayload.of(event));

    assertEquals(WorkflowEventKind.UPDATED, read.getEventKind());
    assertEquals(event.getEventId(), read.getEventId());
    assertEquals(event.getWorkflowId(), read.getWorkflowId());
    assertEquals(event.getBusinessId(), read.getBusinessId());
    assertEquals(event.getTimestamp(), read.getTimestamp());
    assertEquals(event.getSource(), read.getSource());
    assertEquals(event.getInitiator(), read.getInitiator());
    assertEquals(event.getComment(), read.getComment());
    assertEquals(event.getWorkflowModuleId(), read.getWorkflowModuleId());
    assertEquals(event.getBpmnProcessId(), read.getBpmnProcessId());
    assertEquals(event.getBpmnProcessVersion(), read.getBpmnProcessVersion());
    assertEquals(event.getTitle(), read.getTitle());
    assertEquals(event.getUiUriPath(), read.getUiUriPath());
    assertEquals(event.getUiUriType(), read.getUiUriType());
    assertEquals(event.getDetailsFulltextSearch(), read.getDetailsFulltextSearch());
    assertEquals(event.getAccessibleToUsers(), read.getAccessibleToUsers());
    assertEquals(event.getAccessibleToGroups(), read.getAccessibleToGroups());
    assertEquals(event.getI18nLanguages(), read.getI18nLanguages());
    assertEquals(EventFixture.details(), read.getDetails());

  }

  @Test
  @DisplayName("The business data comes back as it was set, the value set to nothing included")
  public void theBusinessDataComesBack() {

    final var event = EventFixture.userTask(UserTaskEventKind.UPDATED);

    final var read = ReportPayload.userTaskEvent(ReportPayload.of(event));

    assertEquals(EventFixture.details(), read.getDetails());
    assertTrue(
        read.getDetails().containsKey("cancelledAt"),
        "a detail a provider set to nothing was dropped on the way");
    assertNull(read.getDetails().get("cancelledAt"));

  }

  @Test
  @DisplayName("An amount keeps every digit it was reported with")
  public void anAmountKeepsItsDigits() {

    final var event = EventFixture.userTask(UserTaskEventKind.UPDATED);
    final var details = new LinkedHashMap<String, Object>();
    details.put("amount", new BigDecimal("250.10"));
    event.setDetails(details);

    final var read = ReportPayload.userTaskEvent(ReportPayload.of(event));

    assertEquals(new BigDecimal("250.10"), read.getDetails().get("amount"));

  }

  @Test
  @DisplayName("A report of a newer version is read without the fields this one does not know")
  public void aReportOfANewerVersionIsStillRead() {

    final var payload = """
        {"eventKind":"CREATED","eventId":"event-9","userTaskId":"task-9",\
        "whatAComingVersionAdded":{"nested":true}}"""
        .getBytes(StandardCharsets.UTF_8);

    final var read = ReportPayload.userTaskEvent(payload);

    assertEquals("event-9", read.getEventId());
    assertEquals("task-9", read.getUserTaskId());

  }

  @Test
  @DisplayName("Bytes which are no report end the entry for good")
  public void bytesWhichAreNoReportEndTheEntry() {

    final var failure = assertThrows(
        PhaseTwoPermanentFailure.class,
        () -> ReportPayload.userTaskEvent("not a report".getBytes(StandardCharsets.UTF_8)));

    assertTrue(failure.getMessage().contains("12 bytes"), failure.getMessage());
    assertTrue(failure.getMessage().contains("remove it from the outbox store"), failure
        .getMessage());

  }

  @Test
  @DisplayName("A value which cannot be written names the task it was reported about")
  public void aValueWhichCannotBeWrittenNamesItsTask() {

    final var event = EventFixture.userTask(UserTaskEventKind.UPDATED);
    event.setDetails(Map.of("whatever", new Object()));

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> ReportPayload.of(event));

    assertTrue(failure.getMessage().contains("user task 'task-1'"), failure.getMessage());
    assertTrue(failure.getMessage().contains("cannot be turned into JSON"), failure.getMessage());

  }

}
