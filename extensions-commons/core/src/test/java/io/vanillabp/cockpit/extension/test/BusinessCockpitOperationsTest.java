package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.outbox.BusinessCockpitOperations;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The keys the outbox deduplicates the extension's entries by.
 * <p>
 * These are a persisted contract: an entry written before an upgrade is deduplicated after it,
 * so a change here is a change of behaviour for entries which already exist. The assertions
 * pin the shape rather than describe it.
 */
@ExtendWith(SuppressOutputExtension.class)
public class BusinessCockpitOperationsTest {

  private static PhaseTwoCall userTaskCall(
      final String userTaskId,
      final UserTaskEventKind kind) {

    return PhaseTwoCall
        .of(
            BusinessCockpitOperations.publishUserTaskEvent(),
            "test-module",
            "TestProcess",
            "4711",
            "the-bpms",
            Map
                .of(
                    BusinessCockpitOperations.ARG_USER_TASK_ID, userTaskId,
                    BusinessCockpitOperations.ARG_EVENT_KIND, kind.name(),
                    BusinessCockpitOperations.ARG_EVENT_ID, "event-1"));

  }

  @Test
  @DisplayName("Every key starts with the operation it belongs to")
  public void everyKeyNamesItsOperation() {

    assertTrue(
        userTaskCall("task-1", UserTaskEventKind.CREATED)
            .idempotencyKey()
            .orElseThrow()
            .startsWith(BusinessCockpitOperations.PUBLISH_USER_TASK_EVENT));

  }

  @Test
  @DisplayName("Pending updates of one task collapse into one entry")
  public void pendingUpdatesCollapse() {

    assertEquals(
        userTaskCall("task-1", UserTaskEventKind.UPDATED).idempotencyKey(),
        userTaskCall("task-1", UserTaskEventKind.UPDATED).idempotencyKey());

  }

  @Test
  @DisplayName("An update never collapses with the task's creation or its end")
  public void kindsDoNotCollapse() {

    final var updated = userTaskCall("task-1", UserTaskEventKind.UPDATED).idempotencyKey();
    assertNotEquals(
        updated, userTaskCall("task-1", UserTaskEventKind.CREATED).idempotencyKey());
    assertNotEquals(
        updated, userTaskCall("task-1", UserTaskEventKind.COMPLETED).idempotencyKey());

  }

  @Test
  @DisplayName("Two tasks never collapse")
  public void tasksDoNotCollapse() {

    assertNotEquals(
        userTaskCall("task-1", UserTaskEventKind.UPDATED).idempotencyKey(),
        userTaskCall("task-2", UserTaskEventKind.UPDATED).idempotencyKey());

  }

  @Test
  @DisplayName("A workflow module registers once, however many adapters it was deployed to")
  public void oneRegistrationPerWorkflowModule() {

    final var first = PhaseTwoCall
        .of(
            BusinessCockpitOperations.registerWorkflowModule(), "test-module",
            BusinessCockpitOperations.EVERY_BPMN_PROCESS, null, null, Map.of());
    final var second = PhaseTwoCall
        .of(
            BusinessCockpitOperations.registerWorkflowModule(), "test-module",
            BusinessCockpitOperations.EVERY_BPMN_PROCESS, null, null, Map.of());

    assertEquals(first.idempotencyKey(), second.idempotencyKey());
    assertEquals(
        "%s|test-module".formatted(BusinessCockpitOperations.REGISTER_WORKFLOW_MODULE),
        first.idempotencyKey().orElseThrow());

  }

  @Test
  @DisplayName("An entry says in words what it is about, for an operator reading the store")
  public void anEntryDescribesItself() {

    final var description = BusinessCockpitOperations
        .publishUserTaskEvent()
        .describe(
            Map
                .of(
                    BusinessCockpitOperations.ARG_USER_TASK_ID, "task-1",
                    BusinessCockpitOperations.ARG_EVENT_KIND, "CREATED"));

    assertTrue(description.contains("task-1"), description);
    assertTrue(description.contains("CREATED"), description);

  }

}
