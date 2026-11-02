package io.vanillabp.cockpit.bpms.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.google.protobuf.Timestamp;

import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskActivatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskSuspendedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowActivatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowSuspendedEvent;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The suspension and the activation of a process instance, over Kafka.
 * <p>
 * The Kafka way in reaches the same two services as the REST way in, and those services hold the
 * rules. So these tests only show that the four messages reach them. The rules themselves are tested
 * on the REST way in.
 */
@ExtendWith(SuppressOutputExtension.class)
class KafkaSuspendedAndActivatedTest {

  private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-11-01T08:00:00Z");
  private static final OffsetDateTime SUSPENDED_AT = OffsetDateTime.parse("2026-11-01T09:00:00Z");
  private static final OffsetDateTime ACTIVATED_AT = OffsetDateTime.parse("2026-11-01T10:00:00Z");

  private final Map<String, UserTask> storedTasks = new HashMap<>();

  private final Map<String, Workflow> storedWorkflows = new HashMap<>();

  private KafkaUserTaskController userTasks;

  private KafkaWorkflowController workflows;

  @BeforeEach
  void setUp() throws Exception {

    final var personAndGroupMapper = mock(PersonAndGroupMapper.class);
    final var userTaskMapper = new ProtobufUserTaskMapperImpl();
    set(ProtobufUserTaskMapper.class, userTaskMapper, "personAndGroupMapper", personAndGroupMapper);
    final var workflowMapper = new ProtobufWorkflowMapperImpl();
    set(ProtobufWorkflowMapper.class, workflowMapper, "personAndGroupMapper", personAndGroupMapper);

    final var userTaskRepository = mock(UserTaskRepository.class);
    when(userTaskRepository.findById(anyString()))
        .thenAnswer(invocation -> Optional.ofNullable(storedTasks.get(invocation.getArgument(0))));
    when(userTaskRepository.save(any(UserTask.class))).thenAnswer(invocation -> {
      final UserTask task = invocation.getArgument(0);
      storedTasks.put(task.getId(), task);
      return task;
    });

    final var workflowRepository = mock(WorkflowRepository.class);
    when(workflowRepository.findById(anyString()))
        .thenAnswer(invocation -> Optional.ofNullable(storedWorkflows.get(invocation.getArgument(0))));
    when(workflowRepository.save(any(Workflow.class))).thenAnswer(invocation -> {
      final Workflow workflow = invocation.getArgument(0);
      storedWorkflows.put(workflow.getId(), workflow);
      return workflow;
    });

    final var userTaskService = new UserTaskService();
    set(UserTaskService.class, userTaskService, "userTasks", userTaskRepository);
    set(UserTaskService.class, userTaskService, "logger", mock(Logger.class));
    set(UserTaskService.class, userTaskService, "mongoTemplate", mock(MongoTemplate.class));

    final var workflowlistService = new WorkflowlistService();
    set(WorkflowlistService.class, workflowlistService, "workflowRepository", workflowRepository);
    set(WorkflowlistService.class, workflowlistService, "logger", mock(Logger.class));
    set(WorkflowlistService.class, workflowlistService, "mongoTemplate", mock(MongoTemplate.class));

    userTasks = new KafkaUserTaskController(userTaskService, userTaskMapper);
    workflows = new KafkaWorkflowController(workflowlistService, workflowMapper);

  }

  private static void set(
      final Class<?> declaringClass,
      final Object target,
      final String field,
      final Object value) throws Exception {

    final var declaredField = declaringClass.getDeclaredField(field);
    declaredField.setAccessible(true);
    declaredField.set(target, value);

  }

  private static ConsumerRecord<String, byte[]> record(
      final BcEvent event) {

    return new ConsumerRecord<>("topic", 0, 0, "key", event.toByteArray());

  }

  private static Timestamp protobuf(
      final OffsetDateTime timestamp) {

    return Timestamp
        .newBuilder()
        .setSeconds(timestamp.toEpochSecond())
        .setNanos(timestamp.getNano())
        .build();

  }

  /**
   * A protobuf timestamp is an instant, and the mapper turns it back into an
   * {@link OffsetDateTime} in the offset the JVM runs in. So the same moment is compared.
   */
  private static void assertSameMoment(
      final OffsetDateTime expected,
      final OffsetDateTime actual) {

    assertNotNull(actual);
    assertEquals(expected.toInstant(), actual.toInstant());

  }

  @Test
  void aUserTaskIsSuspendedAndActivatedAgain() {

    userTasks.consumeUserTaskEvent(record(BcEvent
        .newBuilder()
        .setUserTaskCreatedV11(UserTaskCreatedOrUpdatedEvent
            .newBuilder()
            .setId("event-created")
            .setUserTaskId("task-1")
            .setTimestamp(protobuf(CREATED_AT))
            .setWorkflowModuleId("taxi-ride")
            .setBpmnProcessId("ride")
            .setTaskDefinition("assign-driver")
            .putTitle("en", "Assign a driver")
            .setUiUriPath("/ui")
            .setUiUriType("WEBPACK_MF_REACT"))
        .build()));

    userTasks.consumeUserTaskEvent(record(BcEvent
        .newBuilder()
        .setUserTaskSuspended(UserTaskSuspendedEvent
            .newBuilder()
            .setId("event-suspended")
            .setApiVersion("1.0")
            .setUserTaskId("task-1")
            .setTimestamp(protobuf(SUSPENDED_AT))
            .setSuspendedBy("olivia"))
        .build()));

    assertSameMoment(SUSPENDED_AT, storedTasks.get("task-1").getSuspendedAt());
    assertEquals("olivia", storedTasks.get("task-1").getSuspendedBy());

    userTasks.consumeUserTaskEvent(record(BcEvent
        .newBuilder()
        .setUserTaskActivated(UserTaskActivatedEvent
            .newBuilder()
            .setId("event-activated")
            .setApiVersion("1.0")
            .setUserTaskId("task-1")
            .setTimestamp(protobuf(ACTIVATED_AT))
            .setSuspendedBy("sam"))
        .build()));

    assertNull(storedTasks.get("task-1").getSuspendedAt());
    assertEquals("sam", storedTasks.get("task-1").getSuspendedBy());

  }

  @Test
  void aSuspensionOfAnUnknownUserTaskIsTakenAndLeftOut() {

    // the listener does not throw, so the record is not handed to it again
    userTasks.consumeUserTaskEvent(record(BcEvent
        .newBuilder()
        .setUserTaskSuspended(UserTaskSuspendedEvent
            .newBuilder()
            .setId("event-suspended")
            .setUserTaskId("task-1")
            .setTimestamp(protobuf(SUSPENDED_AT)))
        .build()));

    assertNull(storedTasks.get("task-1"));

  }

  @Test
  void aWorkflowIsSuspendedAndActivatedAgain() {

    workflows.consumeWorkflowEvent(record(BcEvent
        .newBuilder()
        .setWorkflowCreatedV11(WorkflowCreatedOrUpdatedEvent
            .newBuilder()
            .setId("event-created")
            .setWorkflowId("workflow-1")
            .setTimestamp(protobuf(CREATED_AT))
            .setWorkflowModuleId("taxi-ride")
            .setBpmnProcessId("ride")
            .putTitle("en", "A ride")
            .setUiUriPath("/ui")
            .setUiUriType("WEBPACK_MF_REACT"))
        .build()));

    workflows.consumeWorkflowEvent(record(BcEvent
        .newBuilder()
        .setWorkflowSuspended(WorkflowSuspendedEvent
            .newBuilder()
            .setId("event-suspended")
            .setApiVersion("1.0")
            .setWorkflowId("workflow-1")
            .setTimestamp(protobuf(SUSPENDED_AT))
            .setSuspendedBy("olivia"))
        .build()));

    assertSameMoment(SUSPENDED_AT, storedWorkflows.get("workflow-1").getSuspendedAt());
    assertEquals("olivia", storedWorkflows.get("workflow-1").getSuspendedBy());

    workflows.consumeWorkflowEvent(record(BcEvent
        .newBuilder()
        .setWorkflowActivated(WorkflowActivatedEvent
            .newBuilder()
            .setId("event-activated")
            .setApiVersion("1.0")
            .setWorkflowId("workflow-1")
            .setTimestamp(protobuf(ACTIVATED_AT))
            .setSuspendedBy("sam"))
        .build()));

    assertNull(storedWorkflows.get("workflow-1").getSuspendedAt());
    assertEquals("sam", storedWorkflows.get("workflow-1").getSuspendedBy());

  }

}
