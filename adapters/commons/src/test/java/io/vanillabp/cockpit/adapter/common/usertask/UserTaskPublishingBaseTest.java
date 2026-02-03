package io.vanillabp.cockpit.adapter.common.usertask;

import io.vanillabp.cockpit.adapter.common.properties.VanillaBpCockpitProperties;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskCreatedEvent;
import io.vanillabp.cockpit.adapter.common.usertask.events.UserTaskUiUriType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTaskPublishingBaseTest {

    @Mock
    private VanillaBpCockpitProperties properties;

    private TestUserTaskPublishingBase publishingBase;

    private static final String WORKER_ID = "test-worker";
    private static final String WORKFLOW_MODULE_ID = "test-module";
    private static final List<String> I18N_LANGUAGES = Arrays.asList("en", "de");

    @BeforeEach
    void setUp() {
        publishingBase = new TestUserTaskPublishingBase(WORKER_ID, properties);
    }

    @Test
    void editUserTaskCreatedOrUpdatedEvent_setsSource() {
        UserTaskCreatedEvent event = new UserTaskCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("WEBPACK_MF_REACT");

        publishingBase.callEditUserTaskCreatedOrUpdatedEvent(event);

        assertThat(event.getSource()).isEqualTo(WORKER_ID);
    }

    @Test
    void editUserTaskCreatedOrUpdatedEvent_setsUiUriPath() {
        UserTaskCreatedEvent event = new UserTaskCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/custom/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");

        publishingBase.callEditUserTaskCreatedOrUpdatedEvent(event);

        assertThat(event.getUiUriPath()).isEqualTo("/custom/ui/path");
    }

    @Test
    void editUserTaskCreatedOrUpdatedEvent_setsUiUriType_WEBPACK_MF_REACT() {
        UserTaskCreatedEvent event = new UserTaskCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("WEBPACK_MF_REACT");

        publishingBase.callEditUserTaskCreatedOrUpdatedEvent(event);

        assertThat(event.getUiUriType()).isEqualTo(UserTaskUiUriType.WEBPACK_MF_REACT);
    }

    @Test
    void editUserTaskCreatedOrUpdatedEvent_setsUiUriType_EXTERNAL() {
        UserTaskCreatedEvent event = new UserTaskCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("EXTERNAL");

        publishingBase.callEditUserTaskCreatedOrUpdatedEvent(event);

        assertThat(event.getUiUriType()).isEqualTo(UserTaskUiUriType.EXTERNAL);
    }

    @Test
    void editUserTaskCreatedOrUpdatedEvent_withInvalidUiUriType_throwsRuntimeException() {
        UserTaskCreatedEvent event = new UserTaskCreatedEvent(WORKFLOW_MODULE_ID, I18N_LANGUAGES);
        when(properties.getUiUriPath(WORKFLOW_MODULE_ID)).thenReturn("/ui/path");
        when(properties.getUiUriType(WORKFLOW_MODULE_ID)).thenReturn("INVALID_TYPE");

        assertThatThrownBy(() -> publishingBase.callEditUserTaskCreatedOrUpdatedEvent(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unsupported UI-URI-type");
    }

    private static class TestUserTaskPublishingBase extends UserTaskPublishingBase {

        protected TestUserTaskPublishingBase(String workerId, VanillaBpCockpitProperties properties) {
            super(workerId, properties);
        }

        public void callEditUserTaskCreatedOrUpdatedEvent(UserTaskCreatedEvent event) {
            editUserTaskCreatedOrUpdatedEvent(event);
        }
    }
}
