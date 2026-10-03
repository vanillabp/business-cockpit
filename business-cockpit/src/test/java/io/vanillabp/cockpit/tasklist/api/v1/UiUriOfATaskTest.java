package io.vanillabp.cockpit.tasklist.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.vanillabp.cockpit.tasklist.model.UserTask;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * What the GUI API answers about where a task is shown.
 * <p>
 * It answers the path the workflow module reported, whatever the type says. Turning that path into
 * an address is a convention between the module and the user interface which loads it, so the type
 * is carried along and never read. The proxy route of the module is in the same answer, as
 * <code>workflowModuleUri</code>, which is what a user interface needs to build the address itself.
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
            final String uiUriType,
            final String uiUriPath) {

        final var task = new UserTask();
        task.setId("task-1");
        task.setWorkflowModuleId("taxi-ride");
        task.setUiUriType(uiUriType);
        task.setUiUriPath(uiUriPath);
        return task;

    }

    @Test
    void aFederatedTaskKeepsTheReportedPath() {

        final var task = task("WEBPACK_MF_REACT", "/remoteEntry.js");

        assertThat(mapper.toApi(task, "anna").getUiUri()).isEqualTo("/remoteEntry.js");

    }

    @Test
    void aPathWithoutALeadingSlashIsNotTouchedEither() {

        final var task = task("WEBPACK_MF_REACT", "remoteEntry.js");

        assertThat(mapper.toApi(task, "anna").getUiUri()).isEqualTo("remoteEntry.js");

    }

    @Test
    void aTaskOfAnotherApplicationKeepsTheAddressItWasReportedWith() {

        final var task = task("EXTERNAL", "https://tickets.example.com/ticket/4711");

        assertThat(mapper.toApi(task, "anna").getUiUri())
                .isEqualTo("https://tickets.example.com/ticket/4711");

    }

    @Test
    void aTypeTheCockpitNeverHeardOfTravelsAllTheSame() {

        final var task = task("ANGULAR", "/some/where");

        final var answer = mapper.toApi(task, "anna");
        assertThat(answer.getUiUriType()).isEqualTo("ANGULAR");
        assertThat(answer.getUiUri()).isEqualTo("/some/where");

    }

    @Test
    void aTaskWithoutAnAddressHasNone() {

        assertThat(mapper.toApi(task("EXTERNAL", null), "anna").getUiUri()).isNull();

    }

    @Test
    void theProxyRouteOfTheModuleIsAnsweredOnItsOwn() {

        final var task = task("WEBPACK_MF_REACT", "/remoteEntry.js");

        assertThat(mapper.toApi(task, "anna").getWorkflowModuleUri()).isEqualTo("/wm/taxi-ride");

    }

}
