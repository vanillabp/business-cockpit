package io.vanillabp.cockpit.workflowlist.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * What the GUI API answers about where a case is shown. It follows the same rule as a task: the
 * reported path is handed out unchanged, the type travels with it, and the proxy route of the
 * module is answered on its own.
 */
@ExtendWith(SuppressOutputExtension.class)
class UiUriOfAWorkflowTest {

    private GuiApiMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        final var impl = new WorkflowListGuiApiMapperImpl();
        final var field = GuiApiMapper.class.getDeclaredField("personAndGroupMapper");
        field.setAccessible(true);
        field.set(impl, mock(PersonAndGroupApiMapper.class));
        mapper = impl;
    }

    private static Workflow workflow(
            final String uiUriType,
            final String uiUriPath) {

        final var workflow = new Workflow();
        workflow.setId("workflow-1");
        workflow.setWorkflowModuleId("taxi-ride");
        workflow.setUiUriType(uiUriType);
        workflow.setUiUriPath(uiUriPath);
        return workflow;

    }

    @Test
    void aFederatedWorkflowKeepsTheReportedPath() {

        final var workflow = workflow("WEBPACK_MF_REACT", "/remoteEntry.js");

        assertThat(mapper.toApi(workflow).getUiUri()).isEqualTo("/remoteEntry.js");

    }

    @Test
    void aWorkflowOfAnotherApplicationKeepsTheAddressItWasReportedWith() {

        final var workflow = workflow("EXTERNAL", "https://orders.example.com/order/4711");

        assertThat(mapper.toApi(workflow).getUiUri())
                .isEqualTo("https://orders.example.com/order/4711");

    }

    @Test
    void aTypeTheCockpitNeverHeardOfTravelsAllTheSame() {

        final var answer = mapper.toApi(workflow("ANGULAR", "/some/where"));

        assertThat(answer.getUiUriType()).isEqualTo("ANGULAR");
        assertThat(answer.getUiUri()).isEqualTo("/some/where");

    }

    @Test
    void aWorkflowWithoutAnAddressHasNone() {

        assertThat(mapper.toApi(workflow("EXTERNAL", null)).getUiUri()).isNull();

    }

    @Test
    void theProxyRouteOfTheModuleIsAnsweredOnItsOwn() {

        final var workflow = workflow("WEBPACK_MF_REACT", "/remoteEntry.js");

        assertThat(mapper.toApi(workflow).getWorkflowModuleUri()).isEqualTo("/wm/taxi-ride");

    }

}
