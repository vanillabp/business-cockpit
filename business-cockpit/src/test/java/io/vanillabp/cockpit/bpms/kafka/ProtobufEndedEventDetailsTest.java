package io.vanillabp.cockpit.bpms.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsArrayValue;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsMap;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.DetailsValue;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.bpms.api.protobuf.v1.WorkflowCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * What an end reported over Kafka does to the business data the cockpit already stored.
 * <p>
 * A workflow module reports the same fields for an end as for a change, and the details map is one
 * of them. A protobuf map has no presence information, so an end which carries no details arrives
 * as an empty map and cannot be told apart from one which was never filled. Storing that empty map
 * would erase the business data of the case at the very moment the list of completed cases starts
 * showing it.
 */
@ExtendWith(SuppressOutputExtension.class)
class ProtobufEndedEventDetailsTest {

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

    private static UserTask storedTask() {
        final var task = new UserTask();
        task.setId("task-1");
        task.setDetails(new HashMap<>(Map.of("amount", "250")));
        task.setDetailsFulltextSearch("250");
        return task;
    }

    private static Workflow storedWorkflow() {
        final var workflow = new Workflow();
        workflow.setId("workflow-1");
        workflow.setDetails(new HashMap<>(Map.of("customer", "Anna")));
        workflow.setDetailsFulltextSearch("Anna");
        return workflow;
    }

    @Test
    @DisplayName("A completed task which reports no details keeps the details it had")
    void anEndWithoutDetailsKeepsTheStoredTaskDetails() {

        final var stored = storedTask();

        final var result = userTaskMapper.toEndedTask(
                UserTaskCreatedOrUpdatedEvent
                        .newBuilder()
                        .setId("evt-1")
                        .setUserTaskId("task-1")
                        .setUiUriType("EXTERNAL")
                        // the extension sets the field whether it has anything to put in it or
                        // not, which is how an end of a task the BPMS cannot describe any more
                        // arrives here
                        .setDetails(DetailsMap.getDefaultInstance())
                        .build(),
                stored);

        assertEquals(Map.of("amount", "250"), result.getDetails());
        assertEquals("250", result.getDetailsFulltextSearch());

    }

    @Test
    @DisplayName("A completed task which reports details is stored with them")
    void anEndWithDetailsReplacesTheStoredTaskDetails() {

        final var stored = storedTask();

        final var result = userTaskMapper.toEndedTask(
                UserTaskCreatedOrUpdatedEvent
                        .newBuilder()
                        .setId("evt-2")
                        .setUserTaskId("task-1")
                        .setUiUriType("EXTERNAL")
                        .setDetails(oneDetail("amount", "500"))
                        .setDetailsFulltextSearch("500")
                        .build(),
                stored);

        assertEquals(Map.of("amount", "500"), result.getDetails());
        assertEquals("500", result.getDetailsFulltextSearch());

    }

    @Test
    @DisplayName("An ended workflow which reports no details keeps the details it had")
    void anEndWithoutDetailsKeepsTheStoredWorkflowDetails() {

        final var stored = storedWorkflow();

        final var result = workflowMapper.toEndedWorkflow(
                WorkflowCreatedOrUpdatedEvent
                        .newBuilder()
                        .setId("evt-3")
                        .setWorkflowId("workflow-1")
                        .setUiUriType("EXTERNAL")
                        .setDetails(DetailsMap.getDefaultInstance())
                        .build(),
                stored);

        assertEquals(Map.of("customer", "Anna"), result.getDetails());
        assertEquals("Anna", result.getDetailsFulltextSearch());

    }

    @Test
    @DisplayName("An ended workflow which reports details is stored with them")
    void anEndWithDetailsReplacesTheStoredWorkflowDetails() {

        final var stored = storedWorkflow();

        final var result = workflowMapper.toEndedWorkflow(
                WorkflowCreatedOrUpdatedEvent
                        .newBuilder()
                        .setId("evt-4")
                        .setWorkflowId("workflow-1")
                        .setUiUriType("EXTERNAL")
                        .setDetails(oneDetail("customer", "Bert"))
                        .setDetailsFulltextSearch("Bert")
                        .build(),
                stored);

        assertEquals(Map.of("customer", "Bert"), result.getDetails());
        assertEquals("Bert", result.getDetailsFulltextSearch());

    }

}
