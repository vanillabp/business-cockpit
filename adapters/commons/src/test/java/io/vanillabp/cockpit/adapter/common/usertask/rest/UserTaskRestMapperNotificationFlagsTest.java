package io.vanillabp.cockpit.adapter.common.usertask.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUpdatedEvent;
import io.vanillabp.spi.cockpit.usertask.NotificationDelivery;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies the workflow-module side REST mapper carries the {@link NotificationDelivery} directive
 * from the module's user task event onto the outgoing bpms-api v1_1 event (T03).
 */
class UserTaskRestMapperNotificationFlagsTest {

    private final UserTaskRestMapper mapper = new UserTaskRestMapperImpl();

    @Test
    void created_mapsForce() {
        final var event = new UserTaskCreatedEvent("wfm", List.of());
        event.setNotificationDelivery(NotificationDelivery.FORCE);

        final var result = mapper.map(event);
        assertEquals(io.vanillabp.cockpit.bpms.api.v1_1.NotificationDelivery.FORCE,
                result.getNotificationDelivery());
    }

    @Test
    void created_absentDelivery_mapsToNull() {
        final var event = new UserTaskCreatedEvent("wfm", List.of());

        final var result = mapper.map(event);
        assertNull(result.getNotificationDelivery());
    }

    @Test
    void updated_mapsSuppress() {
        final var event = new UserTaskUpdatedEvent("wfm", List.of());
        event.setNotificationDelivery(NotificationDelivery.SUPPRESS);

        final var result = mapper.map(event);
        assertEquals(io.vanillabp.cockpit.bpms.api.v1_1.NotificationDelivery.SUPPRESS,
                result.getNotificationDelivery());
    }

}
