package io.vanillabp.cockpit.extension.test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.vanillabp.cockpit.extension.config.UiUriType;
import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.spi.cockpit.usertask.NotificationDelivery;

/**
 * Events with every field filled, so that a mapper leaving one out is visible. A fixed
 * timestamp keeps the assertions about what travels readable.
 */
public final class EventFixture {

  /** The moment every event of a test happened at. */
  public static final OffsetDateTime TIMESTAMP = OffsetDateTime
      .of(2026, 9, 8, 12, 0, 0, 0, ZoneOffset.UTC);

  private EventFixture() {
  }

  /**
   * @param kind What happened to the task
   * @return A user-task event with every field filled
   */
  public static UserTaskEvent userTask(
      final UserTaskEventKind kind) {

    final var event = new UserTaskEvent(kind);
    event.setEventId("event-1");
    event.setUserTaskId("task-1");
    event.setTimestamp(TIMESTAMP);
    event.setSource("host-1");
    event.setInitiator("clerk");
    event.setComment("please have a look");
    event.setWorkflowModuleId("test-module");
    event.setBpmnProcessId("TestProcess");
    event.setBpmnProcessVersion("3");
    event.setWorkflowId("workflow-1");
    event.setSubWorkflowId("sub-workflow-1");
    event.setBusinessId("4711");
    event.setBpmnTaskId("Activity_approve");
    event.setTaskDefinition("approve");
    event.setWorkflowTitle(new LinkedHashMap<>(Map.of("en", "Order 4711")));
    event.setTitle(new LinkedHashMap<>(Map.of("en", "Approve order 4711")));
    event.setTaskDefinitionTitle(new LinkedHashMap<>(Map.of("en", "Approval")));
    event.setUiUriPath("/remoteEntry.js");
    event.setUiUriType(UiUriType.WEBPACK_MF_REACT);
    event.setAssignee("anna");
    event.setCandidateUsers(List.of("bert"));
    event.setCandidateGroups(List.of("approvers"));
    event.setExcludedCandidateUsers(List.of("carl"));
    event.setDueDate(TIMESTAMP.plusDays(1));
    event.setFollowUpDate(TIMESTAMP.plusHours(2));
    event.setDetails(details());
    event.setDetailsFulltextSearch("order 4711 approve");
    event.setNotificationDelivery(NotificationDelivery.FORCE);
    event.setI18nLanguages(List.of("en"));
    return event;

  }

  /**
   * @param kind What happened to the workflow
   * @return A workflow event with every field filled
   */
  public static WorkflowEvent workflow(
      final WorkflowEventKind kind) {

    final var event = new WorkflowEvent(kind);
    event.setEventId("event-2");
    event.setWorkflowId("workflow-1");
    event.setBusinessId("4711");
    event.setTimestamp(TIMESTAMP);
    event.setSource("host-1");
    event.setInitiator("clerk");
    event.setComment("in progress");
    event.setWorkflowModuleId("test-module");
    event.setBpmnProcessId("TestProcess");
    event.setBpmnProcessVersion("3");
    event.setTitle(new LinkedHashMap<>(Map.of("en", "Order 4711")));
    event.setUiUriPath("/remoteEntry.js");
    event.setUiUriType(UiUriType.EXTERNAL);
    event.setDetails(details());
    event.setDetailsFulltextSearch("order 4711");
    event.setAccessibleToUsers(List.of("anna"));
    event.setAccessibleToGroups(List.of("approvers"));
    event.setI18nLanguages(List.of("en"));
    return event;

  }

  /**
   * @return The registration of a workflow module
   */
  public static RegisterWorkflowModuleEvent workflowModule() {

    return new RegisterWorkflowModuleEvent(
        "event-3", TIMESTAMP, "host-1", "test-module", "http://localhost:8081", RegisterWorkflowModuleEvent.TASK_PROVIDER_API_URI_PATH, RegisterWorkflowModuleEvent.WORKFLOW_PROVIDER_API_URI_PATH, List
            .of("approvers"), Map.of("TEAM_LEAD", List.of("TEAM_MEMBER")));

  }

  /**
   * The business data a details provider reports: a scalar, a list, a nested object and a
   * value which is deliberately absent - every shape the protobuf conversion has a case for.
   *
   * @return The details
   */
  public static Map<String, Object> details() {

    final var details = new LinkedHashMap<String, Object>();
    details.put("amount", 250);
    details.put("currency", "EUR");
    details.put("urgent", Boolean.TRUE);
    details.put("tags", List.of("first", "second"));
    details.put("customer", Map.of("name", "Anna"));
    details.put("cancelledAt", null);
    return details;

  }

}
