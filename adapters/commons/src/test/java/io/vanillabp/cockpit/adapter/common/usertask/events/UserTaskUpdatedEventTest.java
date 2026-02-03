package io.vanillabp.cockpit.adapter.common.usertask.events;

import io.vanillabp.spi.cockpit.details.DetailsEvent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserTaskUpdatedEventTest {

    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @Test
    void constructor_setsWorkflowModuleIdAndI18nLanguages() {
        UserTaskUpdatedEvent event = new UserTaskUpdatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);

        assertThat(event.getWorkflowModuleId()).isEqualTo(WORKFLOW_MODULE_ID);
        assertThat(event.getI18nLanguages()).isEqualTo(I18N_LANGUAGES);
    }

    @Test
    void getEventType_returnsUpdated() {
        UserTaskUpdatedEvent event = new UserTaskUpdatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);

        assertThat(event.getEventType()).isEqualTo(DetailsEvent.Event.UPDATED);
    }

    @Test
    void extendsUserTaskEventImpl() {
        UserTaskUpdatedEvent event = new UserTaskUpdatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);

        assertThat(event).isInstanceOf(UserTaskEventImpl.class);
    }

    @Test
    void implementsUserTaskEvent() {
        UserTaskUpdatedEvent event = new UserTaskUpdatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);

        assertThat(event).isInstanceOf(UserTaskEvent.class);
    }
}
