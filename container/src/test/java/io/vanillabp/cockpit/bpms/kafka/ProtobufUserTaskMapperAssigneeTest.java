package io.vanillabp.cockpit.bpms.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vanillabp.cockpit.bpms.api.protobuf.v1.UserTaskCreatedOrUpdatedEvent;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.Group;
import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The assignment of a user task is cockpit-owned state: a task is taken over in the cockpit, so no
 * workflow system reports it. These tests pin down that an incoming event never wipes it - the
 * defect which silenced the notification of the former assignee, because the notification poller
 * cannot determine a recipient without an assignee.
 */
class ProtobufUserTaskMapperAssigneeTest {

    private ProtobufUserTaskMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        final var impl = new ProtobufUserTaskMapperImpl();
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
        final var field = ProtobufUserTaskMapper.class.getDeclaredField("personAndGroupMapper");
        field.setAccessible(true);
        field.set(impl, personAndGroupMapper);
        mapper = impl;
    }

    private static UserTask takenOverTask() {
        final var task = new UserTask();
        task.setId("task-1");
        final var assignee = new Person();
        assignee.setId("taskOwner");
        task.setAssignee(assignee);
        final var candidate = new Person();
        candidate.setId("candidate");
        task.setCandidateUsers(new ArrayList<>(List.of(candidate)));
        final var candidateGroup = new Group();
        candidateGroup.setId("engineGroup");
        task.setCandidateGroups(new ArrayList<>(List.of(candidateGroup)));
        return task;
    }

    private static UserTaskCreatedOrUpdatedEvent.Builder event() {
        return UserTaskCreatedOrUpdatedEvent
                .newBuilder()
                .setId("evt-1")
                .setUserTaskId("task-1")
                .setUiUriType("EXTERNAL")
                .setUpdated(true);
    }

    @Test
    void toEndedTask_keepsAssigneeAndCandidates() {
        final var task = takenOverTask();

        final var result = mapper.toEndedTask(event().build(), task);

        assertEquals("taskOwner", result.getAssignee().getId());
        assertEquals(List.of("candidate"),
                result.getCandidateUsers().stream().map(Person::getId).toList());
    }

    @Test
    void toEndedTask_mapsInitiator() {
        final var result = mapper.toEndedTask(
                event().setInitiator("completingUser").build(), takenOverTask());

        assertEquals("completingUser", result.getInitiator());
    }

    @Test
    void toEndedTask_withoutInitiator_clearsIt() {
        final var task = takenOverTask();
        task.setInitiator("someoneFromAnEarlierEvent");

        final var result = mapper.toEndedTask(event().build(), task);

        assertNull(result.getInitiator(), "no initiator reported means 'caused by the process'");
    }

    @Test
    void toUpdatedTask_withoutAssignee_keepsTheTakeover() {
        final var result = mapper.toUpdatedTask(event().build(), takenOverTask());

        assertEquals("taskOwner", result.getAssignee().getId());
    }

    @Test
    void toUpdatedTask_withAssignee_overwritesIt() {
        final var result = mapper.toUpdatedTask(
                event().setAssignee("otherUser").build(), takenOverTask());

        assertEquals("otherUser", result.getAssignee().getId());
    }

    @Test
    void toUpdatedTask_keepsCandidateUsersReportedByNobody() {
        // an assignment made in the cockpit is not part of any event
        final var result = mapper.toUpdatedTask(event().build(), takenOverTask());

        assertEquals(List.of("candidate"),
                result.getCandidateUsers().stream().map(Person::getId).toList());
    }

    @Test
    void toUpdatedTask_keepsCandidateUsersEvenIfTheEventReportsOthers() {
        // the event carries the candidates known to the engine, never the cockpit-side assignment
        final var result = mapper.toUpdatedTask(
                event().addCandidateUsers("someoneFromTheEngine").build(), takenOverTask());

        assertEquals(List.of("candidate"),
                result.getCandidateUsers().stream().map(Person::getId).toList());
    }

    @Test
    void toUpdatedTask_stillReplacesCandidateGroups() {
        // groups are never written on the cockpit side, so the event stays authoritative
        final var result = mapper.toUpdatedTask(
                event().addCandidateGroups("newGroup").build(), takenOverTask());

        assertEquals(List.of("newGroup"),
                result.getCandidateGroups().stream().map(Group::getId).toList());
    }

}
