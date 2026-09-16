package io.vanillabp.cockpit.bpms.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.BcEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsArrayValue;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsValue;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.tasklist.UserTaskService;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.tasklist.model.UserTaskEndReason;
import io.vanillabp.cockpit.tasklist.model.UserTaskRepository;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.WorkflowlistService;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.cockpit.workflowlist.model.WorkflowRepository;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
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

/**
 * The same guard as on the REST way in, over Kafka.
 * <p>
 * Both ways in go through the same two services, which is where the order of reports is decided, so
 * these tests prove that instead of being a second set of cases. They cover a completion which
 * arrives before the creation it completes, and a change which arrives after the change which
 * followed it.
 */
@ExtendWith(SuppressOutputExtension.class)
class KafkaOrderOfReportsTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-09-11T08:00:00Z");
    private static final OffsetDateTime CHANGED_AT = OffsetDateTime.parse("2026-09-11T09:00:00Z");
    private static final OffsetDateTime ENDED_AT = OffsetDateTime.parse("2026-09-11T10:00:00Z");

    private final Map<String, UserTask> storedTasks = new HashMap<>();

    private final Map<String, Workflow> storedWorkflows = new HashMap<>();

    private KafkaUserTaskController userTasks;

    private KafkaWorkflowController workflows;

    @BeforeEach
    void setUp() throws Exception {

        final var personAndGroupMapper = mock(PersonAndGroupMapper.class);
        when(personAndGroupMapper.toModelPerson(anyString())).thenAnswer(invocation -> {
            final var person = new Person();
            person.setId(invocation.getArgument(0));
            return person;
        });
        when(personAndGroupMapper.toModelGroup(anyString())).thenAnswer(invocation -> {
            final var group = new Group();
            group.setId(invocation.getArgument(0));
            return group;
        });

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

    /**
     * A protobuf timestamp is an instant, and the mapper turns it back into an
     * {@link OffsetDateTime} in the offset the JVM runs in. So the same moment is compared here
     * rather than the same reading of it.
     */
    private static void assertSameMoment(
            final OffsetDateTime expected,
            final OffsetDateTime actual) {

        assertNotNull(actual);
        assertEquals(expected.toInstant(), actual.toInstant());

    }

    private void consume(
            final BcEvent event) {

        final var record = new ConsumerRecord<String, byte[]>("topic", 0, 0, "key", event.toByteArray());
        if (event.hasUserTaskCreatedV11()
                || event.hasUserTaskUpdatedV11()
                || event.hasUserTaskCompletedV11()) {
            userTasks.consumeUserTaskEvent(record);
        } else {
            workflows.consumeWorkflowEvent(record);
        }

    }

    private static Timestamp protobuf(
            final OffsetDateTime timestamp) {

        return Timestamp
                .newBuilder()
                .setSeconds(timestamp.toEpochSecond())
                .setNanos(timestamp.getNano())
                .build();

    }

    private static DetailsMap oneDetail(
            final String key,
            final String value) {

        return DetailsMap
                .newBuilder()
                .putDetails(
                        key,
                        DetailsArrayValue
                                .newBuilder()
                                .addArrayValues(DetailsValue.newBuilder().setStringValue(value))
                                .build())
                .build();

    }

    private static UserTaskCreatedOrUpdatedEvent.Builder reportedTask(
            final OffsetDateTime timestamp,
            final String customer) {

        return UserTaskCreatedOrUpdatedEvent
                .newBuilder()
                .setId("event-" + timestamp)
                .setUserTaskId("task-1")
                .setTimestamp(protobuf(timestamp))
                .setWorkflowModuleId("taxi-ride")
                .setBpmnProcessId("ride")
                .setWorkflowId("workflow-1")
                .setTaskDefinition("assign-driver")
                .putTitle("en", "Assign a driver")
                .setUiUriPath("/ui")
                .setUiUriType("WEBPACK_MF_REACT")
                .setDetails(oneDetail("customer", customer));

    }

    private static WorkflowCreatedOrUpdatedEvent.Builder reportedWorkflow(
            final OffsetDateTime timestamp,
            final String customer) {

        return WorkflowCreatedOrUpdatedEvent
                .newBuilder()
                .setId("event-" + timestamp)
                .setWorkflowId("workflow-1")
                .setTimestamp(protobuf(timestamp))
                .setWorkflowModuleId("taxi-ride")
                .setBpmnProcessId("ride")
                .putTitle("en", "A ride")
                .setUiUriPath("/ui")
                .setUiUriType("WEBPACK_MF_REACT")
                .setDetails(oneDetail("customer", customer));

    }

    @Test
    void aCompletionArrivingBeforeTheCreationLeavesTheTaskEnded() {

        consume(BcEvent
                .newBuilder()
                .setUserTaskCompletedV11(reportedTask(ENDED_AT, "Anna"))
                .build());
        consume(BcEvent
                .newBuilder()
                .setUserTaskCreatedV11(reportedTask(CREATED_AT, "Anna"))
                .build());

        final var stored = storedTasks.get("task-1");
        assertNotNull(stored, "the completion of an unknown task has to create it");
        assertSameMoment(ENDED_AT, stored.getEndedAt());
        assertEquals(UserTaskEndReason.COMPLETED, stored.getEndReason());
        // the creation filled in what the end could not report
        assertSameMoment(CREATED_AT, stored.getCreatedAt());
        assertEquals("Assign a driver", stored.getTitle().get("en"));

    }

    @Test
    void aCompletionArrivingBeforeTheCreationLeavesTheCaseEnded() {

        consume(BcEvent
                .newBuilder()
                .setWorkflowCompletedV11(reportedWorkflow(ENDED_AT, "Anna"))
                .build());
        consume(BcEvent
                .newBuilder()
                .setWorkflowCreatedV11(reportedWorkflow(CREATED_AT, "Anna"))
                .build());

        final var stored = storedWorkflows.get("workflow-1");
        assertNotNull(stored, "the completion of an unknown case has to create it");
        assertSameMoment(ENDED_AT, stored.getEndedAt());
        assertSameMoment(CREATED_AT, stored.getCreatedAt());
        assertEquals("A ride", stored.getTitle().get("en"));

    }

    @Test
    void anOlderChangeDoesNotOverwriteAYoungerOne() {

        consume(BcEvent
                .newBuilder()
                .setUserTaskCreatedV11(reportedTask(CREATED_AT, "Anna"))
                .build());
        consume(BcEvent
                .newBuilder()
                .setUserTaskUpdatedV11(reportedTask(ENDED_AT, "Berta").setUpdated(true))
                .build());
        consume(BcEvent
                .newBuilder()
                .setUserTaskUpdatedV11(reportedTask(CHANGED_AT, "Anna").setUpdated(true))
                .build());

        final var stored = storedTasks.get("task-1");
        assertEquals(Map.of("customer", "Berta"), stored.getDetails());
        assertSameMoment(ENDED_AT, stored.getLatestEventAt());
        assertNull(stored.getEndedAt(), "none of the three reports ended the task");

    }

}
