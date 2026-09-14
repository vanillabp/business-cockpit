package io.vanillabp.cockpit.workflowlist.api.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.vanillabp.cockpit.tasklist.model.UiUriType;
import io.vanillabp.cockpit.users.model.PersonAndGroupApiMapper;
import io.vanillabp.cockpit.workflowlist.model.Workflow;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * What the cockpit's user interface is told to open for a case, which follows the same rule as a
 * task: a federated module is served through the cockpit's own proxy, and a case shown by another
 * application keeps the address that application was reported with.
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
            final UiUriType uiUriType,
            final String uiUriPath) {

        final var workflow = new Workflow();
        workflow.setId("workflow-1");
        workflow.setWorkflowModuleId("taxi-ride");
        workflow.setUiUriType(uiUriType);
        workflow.setUiUriPath(uiUriPath);
        return workflow;

    }

    @Test
    void aFederatedWorkflowIsServedThroughTheCockpitsProxy() {

        final var workflow = workflow(UiUriType.WEBPACK_MF_REACT, "/remoteEntry.js");

        assertThat(mapper.toApi(workflow).getUiUri()).isEqualTo("/wm/taxi-ride/remoteEntry.js");

    }

    @Test
    void aWorkflowOfAnotherApplicationKeepsTheAddressItWasReportedWith() {

        final var workflow = workflow(UiUriType.EXTERNAL, "https://orders.example.com/order/4711");

        assertThat(mapper.toApi(workflow).getUiUri())
                .isEqualTo("https://orders.example.com/order/4711");

    }

    @Test
    void aWorkflowWithoutAnAddressHasNone() {

        assertThat(mapper.toApi(workflow(UiUriType.EXTERNAL, null)).getUiUri()).isNull();

    }

}
