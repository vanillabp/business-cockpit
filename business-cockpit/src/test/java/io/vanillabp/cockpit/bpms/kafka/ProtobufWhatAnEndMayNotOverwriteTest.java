package io.vanillabp.cockpit.bpms.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.Timestamp;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.NotificationDelivery;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.tasklist.model.UiUriType;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What an end reported over Kafka leaves alone, field by field.
 * <p>
 * The same end as on the REST way in, and the same list of fields, because a workflow module which
 * writes to a topic reports what one writing to the server reports. Two things differ here and both
 * are why this is a second set of cases rather than a pointer at the first: a field the sender left
 * out arrives as an unset optional, and the titles and the business data arrive empty, because
 * protobuf gives a map no presence information.
 */
class ProtobufWhatAnEndMayNotOverwriteTest {

    private static final OffsetDateTime DUE_AT = OffsetDateTime.parse("2026-09-12T08:00:00Z");

    private static final OffsetDateTime FOLLOW_UP_AT = OffsetDateTime.parse("2026-09-14T08:00:00Z");

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-09-11T08:00:00Z");

    private static final OffsetDateTime ENDED_AT = OffsetDateTime.parse("2026-09-13T10:00:00Z");

    private ProtobufUserTaskMapper userTaskMapper;

    private ProtobufWorkflowMapper workflowMapper;

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

        final var tasks = new ProtobufUserTaskMapperImpl();
        final var taskField = ProtobufUserTaskMapper.class.getDeclaredField("personAndGroupMapper");
        taskField.setAccessible(true);
        taskField.set(tasks, personAndGroupMapper);
        userTaskMapper = tasks;

        final var workflows = new ProtobufWorkflowMapperImpl();
        final var workflowField = ProtobufWorkflowMapper.class.getDeclaredField("personAndGroupMapper");
        workflowField.setAccessible(true);
        workflowField.set(workflows, personAndGroupMapper);
        workflowMapper = workflows;

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
     * {@link OffsetDateTime} in the offset the JVM runs in. So the same moment is compared rather
     * than the same reading of it.
     */
    private static void assertSameMoment(
            final OffsetDateTime expected,
            final OffsetDateTime actual) {

        assertNotNull(actual);
        assertEquals(expected.toInstant(), actual.toInstant());

    }

    /** A task the cockpit knows everything about, so that every field has something to lose. */
    private static UserTask storedTask() {

        final var task = new UserTask();
        task.setId("task-1");
        task.setCreatedAt(CREATED_AT);
        task.setSource("taxi-ride-application");
        task.setWorkflowModuleId("taxi-ride");
        task.setBpmnProcessId("ride");
        task.setBpmnProcessVersion("3");
        task.setWorkflowId("workflow-1");
        task.setSubWorkflowId("workflow-1");
        task.setBusinessId("4711");
        task.setBpmnTaskId("Task_AssignDriver");
        task.setTaskDefinition("assign-driver");
        task.setTitle(new HashMap<>(Map.of("en", "Assign a driver for ride 4711")));
        task.setWorkflowTitle(new HashMap<>(Map.of("en", "Ride 4711")));
        task.setTaskDefinitionTitle(new HashMap<>(Map.of("en", "Assign a driver")));
        task.setUiUriPath("/ui");
        task.setUiUriType(UiUriType.WEBPACK_MF_REACT);
        task.setComment("the driver called in sick");
        task.setDueDate(DUE_AT);
        task.setFollowUpDate(FOLLOW_UP_AT);
        task.setNotificationDelivery(io.vanillabp.spi.cockpit.usertask.NotificationDelivery.FORCE);
        task.setDetails(new HashMap<>(Map.of("customer", "Anna")));
        task.setDetailsFulltextSearch("Anna");
        return task;

    }

    /** A case the cockpit knows everything about. */
    private static Workflow storedWorkflow() {

        final var workflow = new Workflow();
        workflow.setId("workflow-1");
        workflow.setCreatedAt(CREATED_AT);
        workflow.setSource("taxi-ride-application");
        workflow.setWorkflowModuleId("taxi-ride");
        workflow.setBpmnProcessId("ride");
        workflow.setBpmnProcessVersion("3");
        workflow.setBusinessId("4711");
        workflow.setTitle(new HashMap<>(Map.of("en", "Ride 4711")));
        workflow.setUiUriPath("/ui");
        workflow.setUiUriType(UiUriType.WEBPACK_MF_REACT);
        workflow.setComment("the customer cancelled");
        final var starter = new Person();
        starter.setId("anna");
        workflow.setInitiator(starter);
        final var reader = new Person();
        reader.setId("bert");
        workflow.setAccessibleToUsers(new ArrayList<>(List.of(reader)));
        final var drivers = new Group();
        drivers.setId("drivers");
        workflow.setAccessibleToGroups(new ArrayList<>(List.of(drivers)));
        workflow.setDetails(new HashMap<>(Map.of("customer", "Anna")));
        workflow.setDetailsFulltextSearch("Anna");
        return workflow;

    }

    /**
     * The end of a task whose case the BPMS cannot read any more. The details map is set because the
     * sender sets it whether it has anything to put in it or not, and the address of the user
     * interface is set because it comes from the configuration of the workflow module and is
     * therefore known even then.
     */
    private static UserTaskCreatedOrUpdatedEvent.Builder endOfATaskNobodyCanDescribe() {

        return UserTaskCreatedOrUpdatedEvent
                .newBuilder()
                .setId("event-completed")
                .setUserTaskId("task-1")
                .setTimestamp(protobuf(ENDED_AT))
                .setWorkflowModuleId("taxi-ride")
                .setBpmnProcessId("ride")
                .setTaskDefinition("assign-driver")
                .setUiUriPath("/ui")
                .setUiUriType("WEBPACK_MF_REACT")
                .setDetails(DetailsMap.getDefaultInstance());

    }

    /** @see #endOfATaskNobodyCanDescribe() */
    private static WorkflowCreatedOrUpdatedEvent.Builder endOfACaseNobodyCanDescribe() {

        return WorkflowCreatedOrUpdatedEvent
                .newBuilder()
                .setId("event-completed")
                .setWorkflowId("workflow-1")
                .setTimestamp(protobuf(ENDED_AT))
                .setWorkflowModuleId("taxi-ride")
                .setBpmnProcessId("ride")
                .setUiUriPath("/ui")
                .setUiUriType("WEBPACK_MF_REACT")
                .setDetails(DetailsMap.getDefaultInstance());

    }

    @Test
    @DisplayName("An end which reports no due date keeps the stored one")
    void theDueDateSurvivesAnEndWhichDoesNotReportIt() {

        final var result = userTaskMapper
                .toEndedTask(endOfATaskNobodyCanDescribe().build(), storedTask());

        assertSameMoment(DUE_AT, result.getDueDate());

    }

    @Test
    @DisplayName("An end which reports a due date of its own replaces the stored one")
    void aReportedDueDateWins() {

        final var reported = OffsetDateTime.parse("2026-09-20T08:00:00Z");

        final var result = userTaskMapper.toEndedTask(
                endOfATaskNobodyCanDescribe().setDueDate(protobuf(reported)).build(),
                storedTask());

        assertSameMoment(reported, result.getDueDate());

    }

    @Test
    @DisplayName("An end which reports no title keeps the stored one")
    void theTitleSurvivesAnEndWhichDoesNotReportIt() {

        final var result = userTaskMapper
                .toEndedTask(endOfATaskNobodyCanDescribe().build(), storedTask());

        assertEquals(Map.of("en", "Assign a driver for ride 4711"), result.getTitle());

    }

    @Test
    @DisplayName("An end which reports a title of its own replaces the stored one")
    void aReportedTitleWins() {

        final var result = userTaskMapper.toEndedTask(
                endOfATaskNobodyCanDescribe().putTitle("en", "Driver assigned").build(),
                storedTask());

        assertEquals(Map.of("en", "Driver assigned"), result.getTitle());

    }

    @Test
    @DisplayName("An end which reports no title of the case keeps the stored one")
    void theWorkflowTitleSurvivesAnEndWhichDoesNotReportIt() {

        final var result = userTaskMapper
                .toEndedTask(endOfATaskNobodyCanDescribe().build(), storedTask());

        assertEquals(Map.of("en", "Ride 4711"), result.getWorkflowTitle());

    }

    @Test
    @DisplayName("An end which reports no title of the task definition keeps the stored one")
    void theTaskDefinitionTitleSurvivesAnEndWhichDoesNotReportIt() {

        final var result = userTaskMapper
                .toEndedTask(endOfATaskNobodyCanDescribe().build(), storedTask());

        assertEquals(Map.of("en", "Assign a driver"), result.getTaskDefinitionTitle());

    }

    @Test
    @DisplayName("An end which reports no business id keeps the stored one")
    void theBusinessIdSurvivesAnEndWhichDoesNotReportIt() {

        final var result = userTaskMapper
                .toEndedTask(endOfATaskNobodyCanDescribe().build(), storedTask());

        assertEquals("4711", result.getBusinessId());

    }

    @Test
    @DisplayName("An end which reports no version of the BPMN process keeps the stored one")
    void theBpmnProcessVersionSurvivesAnEndWhichDoesNotReportIt() {

        final var result = userTaskMapper
                .toEndedTask(endOfATaskNobodyCanDescribe().build(), storedTask());

        assertEquals("3", result.getBpmnProcessVersion());

    }

    @Test
    @DisplayName("An end which reports no sub workflow keeps the stored one")
    void theSubWorkflowSurvivesAnEndWhichDoesNotReportIt() {

        final var result = userTaskMapper
                .toEndedTask(endOfATaskNobodyCanDescribe().build(), storedTask());

        assertEquals("workflow-1", result.getSubWorkflowId());

    }

    @Test
    @DisplayName("An end which reports no comment keeps the stored one")
    void theCommentSurvivesAnEndWhichDoesNotReportIt() {

        final var result = userTaskMapper
                .toEndedTask(endOfATaskNobodyCanDescribe().build(), storedTask());

        assertEquals("the driver called in sick", result.getComment());

    }

    @Test
    @DisplayName("An end which reports no source keeps the stored one")
    void theSourceSurvivesAnEndWhichDoesNotReportIt() {

        final var result = userTaskMapper
                .toEndedTask(endOfATaskNobodyCanDescribe().build(), storedTask());

        assertEquals("taxi-ride-application", result.getSource());

    }

    @Test
    @DisplayName("An end which reports no way of delivering notifications keeps the stored one")
    void theNotificationDeliverySurvivesAnEndWhichDoesNotReportIt() {

        final var result = userTaskMapper
                .toEndedTask(endOfATaskNobodyCanDescribe().build(), storedTask());

        assertEquals(
                io.vanillabp.spi.cockpit.usertask.NotificationDelivery.FORCE,
                result.getNotificationDelivery());

    }

    @Test
    @DisplayName("An end which asks for the user's own configuration replaces the stored way")
    void aReportedNotificationDeliveryWins() {

        final var result = userTaskMapper.toEndedTask(
                endOfATaskNobodyCanDescribe()
                        .setNotificationDelivery(NotificationDelivery.SUPPRESS)
                        .build(),
                storedTask());

        assertEquals(
                io.vanillabp.spi.cockpit.usertask.NotificationDelivery.SUPPRESS,
                result.getNotificationDelivery());

    }

    @Test
    @DisplayName("An end never writes the follow-up date, which a user sets in the cockpit")
    void theFollowUpDateIsNeverWrittenByAnEnd() {

        final var reported = OffsetDateTime.parse("2026-09-30T08:00:00Z");

        final var result = userTaskMapper.toEndedTask(
                endOfATaskNobodyCanDescribe().setFollowUpDate(protobuf(reported)).build(),
                storedTask());

        assertEquals(FOLLOW_UP_AT, result.getFollowUpDate());

    }

    @Test
    @DisplayName("An end never writes when the task began, whatever timestamp it carries")
    void whenTheTaskBeganIsNeverWrittenByAnEnd() {

        final var result = userTaskMapper
                .toEndedTask(endOfATaskNobodyCanDescribe().build(), storedTask());

        assertEquals(CREATED_AT, result.getCreatedAt());

    }

    @Test
    @DisplayName("An end which names no case keeps the stored case of the task")
    void theCaseOfTheTaskSurvivesAnEndWhichDoesNotReportIt() {

        final var result = userTaskMapper
                .toEndedTask(endOfATaskNobodyCanDescribe().build(), storedTask());

        assertEquals("workflow-1", result.getWorkflowId());
        assertEquals("Task_AssignDriver", result.getBpmnTaskId());

    }

    @Test
    @DisplayName("An end of a case which reports no title keeps the stored one")
    void theTitleOfACaseSurvivesAnEndWhichDoesNotReportIt() {

        final var result = workflowMapper
                .toEndedWorkflow(endOfACaseNobodyCanDescribe().build(), storedWorkflow());

        assertEquals(Map.of("en", "Ride 4711"), result.getTitle());

    }

    @Test
    @DisplayName("An end of a case which reports a title of its own replaces the stored one")
    void aReportedTitleOfACaseWins() {

        final var result = workflowMapper.toEndedWorkflow(
                endOfACaseNobodyCanDescribe().putTitle("en", "Ride 4711, done").build(),
                storedWorkflow());

        assertEquals(Map.of("en", "Ride 4711, done"), result.getTitle());

    }

    @Test
    @DisplayName("An end of a case which reports no business id keeps the stored one")
    void theBusinessIdOfACaseSurvivesAnEndWhichDoesNotReportIt() {

        final var result = workflowMapper
                .toEndedWorkflow(endOfACaseNobodyCanDescribe().build(), storedWorkflow());

        assertEquals("4711", result.getBusinessId());

    }

    @Test
    @DisplayName("An end of a case which reports no version of the BPMN process keeps the stored one")
    void theBpmnProcessVersionOfACaseSurvivesAnEndWhichDoesNotReportIt() {

        final var result = workflowMapper
                .toEndedWorkflow(endOfACaseNobodyCanDescribe().build(), storedWorkflow());

        assertEquals("3", result.getBpmnProcessVersion());

    }

    @Test
    @DisplayName("An end of a case which reports no comment keeps the stored one")
    void theCommentOfACaseSurvivesAnEndWhichDoesNotReportIt() {

        final var result = workflowMapper
                .toEndedWorkflow(endOfACaseNobodyCanDescribe().build(), storedWorkflow());

        assertEquals("the customer cancelled", result.getComment());

    }

    @Test
    @DisplayName("An end of a case which reports nobody keeps who started it")
    void whoStartedTheCaseSurvivesAnEndWhichDoesNotReportIt() {

        final var result = workflowMapper
                .toEndedWorkflow(endOfACaseNobodyCanDescribe().build(), storedWorkflow());

        assertEquals("anna", result.getInitiator().getId());

    }

    @Test
    @DisplayName("An end of a case which names nobody keeps who may see it")
    void whoMaySeeTheCaseSurvivesAnEndWhichDoesNotReportIt() {

        final var result = workflowMapper
                .toEndedWorkflow(endOfACaseNobodyCanDescribe().build(), storedWorkflow());

        assertEquals(List.of("bert"), result.getAccessibleToUsers().stream().map(Person::getId).toList());
        assertEquals(List.of("drivers"), result.getAccessibleToGroups().stream().map(Group::getId).toList());

    }

    @Test
    @DisplayName("An end of a case never writes when it began, whatever timestamp it carries")
    void whenTheCaseBeganIsNeverWrittenByAnEnd() {

        final var result = workflowMapper
                .toEndedWorkflow(endOfACaseNobodyCanDescribe().build(), storedWorkflow());

        assertEquals(CREATED_AT, result.getCreatedAt());

    }

}
