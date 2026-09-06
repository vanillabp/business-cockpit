package io.vanillabp.cockpit.bpms.api.v1_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.vanillabp.cockpit.users.model.Person;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A reporting system may leave the assignee out, which has to reach the stored task as no assignee
 * at all. Mapping the absent value through the person mapper used to store a person carrying a null
 * id: the GUI hid it, but claiming such a task compared the null id to the claiming user and
 * answered HTTP 500.
 */
class UserTaskMapperAssigneeTest {

    private UserTaskMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        final var impl = new UserTaskMapperV1_1Impl();
        final var personAndGroupMapper = mock(PersonAndGroupMapper.class);
        when(personAndGroupMapper.toModelPerson(anyString())).thenAnswer(invocation -> {
            final var person = new Person();
            person.setId(invocation.getArgument(0));
            return person;
        });
        final var field = UserTaskMapper.class.getDeclaredField("personAndGroupMapper");
        field.setAccessible(true);
        field.set(impl, personAndGroupMapper);
        mapper = impl;
    }

    private static UserTaskCreatedEvent createdEvent() {
        final var event = new UserTaskCreatedEvent();
        event.setId("evt-1");
        event.setUserTaskId("task-1");
        return event;
    }

    private static UserTaskUpdatedEvent updatedEvent() {
        final var event = new UserTaskUpdatedEvent();
        event.setId("evt-2");
        event.setUserTaskId("task-1");
        return event;
    }

    @Test
    void toNewTask_withoutAssignee_leavesItUnassigned() {
        assertNull(mapper.toNewTask(createdEvent()).getAssignee());
    }

    @Test
    void toNewTask_withAssignee_mapsThePerson() {
        final var event = createdEvent();
        event.setAssignee("martin");

        assertEquals("martin", mapper.toNewTask(event).getAssignee().getId());
    }

    @Test
    void toNewTask_ofAnUpdateEventWithoutAssignee_leavesItUnassigned() {
        // an update event for an unknown task creates it, so it takes the same path
        assertNull(mapper.toNewTask(updatedEvent()).getAssignee());
    }

    @Test
    void toUpdatedTask_withoutAssignee_keepsTheStoredOne() {
        final var stored = mapper.toNewTask(createdEvent());
        final var assignee = new Person();
        assignee.setId("takenOverInTheCockpit");
        stored.setAssignee(assignee);

        assertEquals("takenOverInTheCockpit",
                mapper.toUpdatedTask(updatedEvent(), stored).getAssignee().getId());
    }

}
