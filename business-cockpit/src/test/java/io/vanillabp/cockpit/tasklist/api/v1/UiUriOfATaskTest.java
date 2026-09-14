package io.vanillabp.cockpit.tasklist.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.vanillabp.cockpit.tasklist.model.UiUriType;
import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * What the cockpit's user interface is told to open for a task.
 * <p>
 * A workflow module federated into the cockpit is served through the cockpit's own proxy, so its
 * path is answered below the module's proxy route. A task worked on in another application is not
 * proxied at all, and prefixing its address would send the browser to a route of the cockpit which
 * does not exist.
 */
@ExtendWith(SuppressOutputExtension.class)
class UiUriOfATaskTest {

    private GuiApiMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        final var impl = new TasklistGuiApiMapperImpl();
        final var field = GuiApiMapper.class.getDeclaredField("personAndGroupMapper");
        field.setAccessible(true);
        field.set(impl, mock(PersonAndGroupApiMapper.class));
        mapper = impl;
    }

    private static UserTask task(
            final UiUriType uiUriType,
            final String uiUriPath) {

        final var task = new UserTask();
        task.setId("task-1");
        task.setWorkflowModuleId("taxi-ride");
        task.setUiUriType(uiUriType);
        task.setUiUriPath(uiUriPath);
        return task;

    }

    @Test
    void aFederatedTaskIsServedThroughTheCockpitsProxy() {

        final var task = task(UiUriType.WEBPACK_MF_REACT, "/remoteEntry.js");

        assertThat(mapper.toApi(task, "anna").getUiUri())
                .isEqualTo("/wm/taxi-ride/remoteEntry.js");

    }

    @Test
    void aPathWithoutALeadingSlashIsJoinedAllTheSame() {

        final var task = task(UiUriType.WEBPACK_MF_REACT, "remoteEntry.js");

        assertThat(mapper.toApi(task, "anna").getUiUri())
                .isEqualTo("/wm/taxi-ride/remoteEntry.js");

    }

    @Test
    void aTaskOfAnotherApplicationKeepsTheAddressItWasReportedWith() {

        final var task = task(UiUriType.EXTERNAL, "https://tickets.example.com/ticket/4711");

        assertThat(mapper.toApi(task, "anna").getUiUri())
                .isEqualTo("https://tickets.example.com/ticket/4711");

    }

    @Test
    void aTaskWithoutAnAddressHasNone() {

        assertThat(mapper.toApi(task(UiUriType.EXTERNAL, null), "anna").getUiUri()).isNull();

    }

}
