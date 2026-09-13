package io.vanillabp.cockpit.bpms.api.v1_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * What an end reported over REST does to the business data the cockpit stored.
 * <p>
 * Version 1 of the cockpit sent the business data with a completion and the list of finished work
 * showed what a case was finished with. Version 2 sent identifiers and timestamps only, and the
 * server threw away what it did get. Both halves of that are fixed, and these tests are the server
 * half.
 */
@ExtendWith(SuppressOutputExtension.class)
class EndedEventDetailsTest {

    private UserTaskMapper userTaskMapper;

    private WorkflowMapper workflowMapper;

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

        final var tasks = new UserTaskMapperV1_1Impl();
        final var taskField = UserTaskMapper.class.getDeclaredField("personAndGroupMapper");
        taskField.setAccessible(true);
        taskField.set(tasks, personAndGroupMapper);
        userTaskMapper = tasks;

        final var workflows = new WorkflowMapperV1_1Impl();
        final var workflowField = WorkflowMapper.class.getDeclaredField("personAndGroupMapper");
        workflowField.setAccessible(true);
        workflowField.set(workflows, personAndGroupMapper);
        workflowMapper = workflows;
    }

    private static UserTask storedTask() {
        final var task = new UserTask();
        task.setId("task-1");
        task.setDetails(new HashMap<>(Map.of("amount", "250")));
        task.setDetailsFulltextSearch("250");
        final var assignee = new Person();
        assignee.setId("anna");
        task.setAssignee(assignee);
        final var candidate = new Person();
        candidate.setId("bert");
        task.setCandidateUsers(List.of(candidate));
        return task;
    }

    private static Workflow storedWorkflow() {
        final var workflow = new Workflow();
        workflow.setId("workflow-1");
        workflow.setDetails(new HashMap<>(Map.of("customer", "Anna")));
        workflow.setDetailsFulltextSearch("Anna");
        return workflow;
    }

    private static UserTaskCompletedEvent completedTask() {
        final var event = new UserTaskCompletedEvent();
        event.setId("evt-1");
        event.setUserTaskId("task-1");
        return event;
    }

    private static WorkflowCompletedEvent completedWorkflow() {
        final var event = new WorkflowCompletedEvent();
        event.setId("evt-2");
        event.setWorkflowId("workflow-1");
        return event;
    }

    @Test
    @DisplayName("A completed task is stored with the business data it was completed with")
    void aCompletionIsStoredWithItsBusinessData() {

        final var event = completedTask();
        event.setDetails(Map.of("amount", "500"));
        event.setDetailsFulltextSearch("500");
        event.setTitle(Map.of("en", "Approve the order"));

        final var result = userTaskMapper.toEndedTask(event, storedTask());

        assertEquals(Map.of("amount", "500"), result.getDetails());
        assertEquals("500", result.getDetailsFulltextSearch());
        assertEquals(Map.of("en", "Approve the order"), result.getTitle());

    }

    @Test
    @DisplayName("A completed task which reports no business data keeps what is stored")
    void aCompletionWithoutBusinessDataKeepsWhatIsStored() {

        final var result = userTaskMapper.toEndedTask(completedTask(), storedTask());

        assertEquals(Map.of("amount", "250"), result.getDetails());
        assertEquals("250", result.getDetailsFulltextSearch());

    }

    @Test
    @DisplayName("A completed task keeps who it belonged to")
    void aCompletionKeepsTheAssigneeAndCandidates() {

        final var result = userTaskMapper.toEndedTask(completedTask(), storedTask());

        assertEquals("anna", result.getAssignee().getId());
        assertEquals(
                List.of("bert"),
                result.getCandidateUsers().stream().map(Person::getId).toList());

    }

    @Test
    @DisplayName("An ended workflow is stored with the business data it ended with")
    void anEndedWorkflowIsStoredWithItsBusinessData() {

        final var event = completedWorkflow();
        event.setDetails(Map.of("customer", "Bert"));
        event.setDetailsFulltextSearch("Bert");

        final var result = workflowMapper.toEndedWorkflow(event, storedWorkflow());

        assertEquals(Map.of("customer", "Bert"), result.getDetails());
        assertEquals("Bert", result.getDetailsFulltextSearch());

    }

    @Test
    @DisplayName("An ended workflow which reports no business data keeps what is stored")
    void anEndedWorkflowWithoutBusinessDataKeepsWhatIsStored() {

        final var result = workflowMapper.toEndedWorkflow(completedWorkflow(), storedWorkflow());

        assertEquals(Map.of("customer", "Anna"), result.getDetails());
        assertEquals("Anna", result.getDetailsFulltextSearch());

    }

}
