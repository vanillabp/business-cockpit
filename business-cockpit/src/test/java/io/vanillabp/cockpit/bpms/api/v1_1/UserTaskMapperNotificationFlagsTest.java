package io.vanillabp.cockpit.bpms.api.v1_1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.PersonAndGroupMapper;
import io.vanillabp.spi.cockpit.usertask.NotificationDelivery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies the v1_1 REST ingress mapper carries the {@link NotificationDelivery} directive from the
 * event onto the persisted {@link UserTask}, and that an absent value maps to {@code null}
 * (interpreted as USER_CONFIG) (T03).
 */
class UserTaskMapperNotificationFlagsTest {

    private UserTaskMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        final var impl = new UserTaskMapperV1_1Impl();
        final var field = UserTaskMapper.class.getDeclaredField("personAndGroupMapper");
        field.setAccessible(true);
        field.set(impl, mock(PersonAndGroupMapper.class));
        mapper = impl;
    }

    private static UserTaskCreatedEvent createdEvent() {
        final var event = new UserTaskCreatedEvent();
        event.setId("evt-1");
        event.setUserTaskId("task-1");
        return event;
    }

    @Test
    void toNewTask_absentDelivery_mapsToNull() {
        final UserTask result = mapper.toNewTask(createdEvent());
        assertNull(result.getNotificationDelivery());
    }

    @Test
    void toNewTask_mapsForce() {
        final var event = createdEvent();
        event.setNotificationDelivery(
                io.vanillabp.cockpit.bpms.api.v1_1.NotificationDelivery.FORCE);

        final UserTask result = mapper.toNewTask(event);
        assertEquals(NotificationDelivery.FORCE, result.getNotificationDelivery());
    }

    @Test
    void toNewTask_mapsSuppress() {
        final var event = createdEvent();
        event.setNotificationDelivery(
                io.vanillabp.cockpit.bpms.api.v1_1.NotificationDelivery.SUPPRESS);

        final UserTask result = mapper.toNewTask(event);
        assertEquals(NotificationDelivery.SUPPRESS, result.getNotificationDelivery());
    }

    @Test
    void toUpdatedTask_updatesDeliveryOnExistingTask() {
        final var existing = new UserTask();
        existing.setNotificationDelivery(NotificationDelivery.SUPPRESS);

        final var event = new UserTaskUpdatedEvent();
        event.setId("evt-2");
        event.setUserTaskId("task-1");
        event.setNotificationDelivery(
                io.vanillabp.cockpit.bpms.api.v1_1.NotificationDelivery.FORCE);

        final UserTask result = mapper.toUpdatedTask(event, existing);
        assertEquals(NotificationDelivery.FORCE, result.getNotificationDelivery());
    }

}
