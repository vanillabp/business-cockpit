package io.vanillabp.cockpit.adapter.common.usertask.events;

import io.vanillabp.spi.cockpit.details.DetailsEvent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserTaskCancelledEventTest {

    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @Test
    void constructor_setsWorkflowModuleIdAndI18nLanguages() {
        UserTaskCancelledEvent event = new UserTaskCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);

        assertThat(event.getWorkflowModuleId()).isEqualTo(WORKFLOW_MODULE_ID);
        assertThat(event.getI18nLanguages()).isEqualTo(I18N_LANGUAGES);
    }

    @Test
    void getEventType_returnsCanceled() {
        UserTaskCancelledEvent event = new UserTaskCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);

        assertThat(event.getEventType()).isEqualTo(DetailsEvent.Event.CANCELED);
    }

    @Test
    void extendsUserTaskEventImpl() {
        UserTaskCancelledEvent event = new UserTaskCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);

        assertThat(event).isInstanceOf(UserTaskEventImpl.class);
    }

    @Test
    void implementsUserTaskEvent() {
        UserTaskCancelledEvent event = new UserTaskCancelledEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);

        assertThat(event).isInstanceOf(UserTaskEvent.class);
    }
}
