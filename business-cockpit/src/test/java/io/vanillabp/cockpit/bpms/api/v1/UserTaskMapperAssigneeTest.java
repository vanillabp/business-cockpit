package io.vanillabp.cockpit.bpms.api.v1;

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
 * The v1 ingress builds the assignee the same way v1.1 does, so it has to leave an absent assignee
 * absent as well instead of storing a person carrying a null id.
 */
class UserTaskMapperAssigneeTest {

    private UserTaskMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        final var impl = new UserTaskMapperV1Impl();
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

    private static UserTaskCreatedOrUpdatedEvent event() {
        final var event = new UserTaskCreatedOrUpdatedEvent();
        event.setId("evt-1");
        event.setUserTaskId("task-1");
        return event;
    }

    @Test
    void toNewTask_withoutAssignee_leavesItUnassigned() {
        assertNull(mapper.toNewTask(event()).getAssignee());
    }

    @Test
    void toNewTask_withAssignee_mapsThePerson() {
        final var event = event();
        event.setAssignee("martin");

        assertEquals("martin", mapper.toNewTask(event).getAssignee().getId());
    }

}
