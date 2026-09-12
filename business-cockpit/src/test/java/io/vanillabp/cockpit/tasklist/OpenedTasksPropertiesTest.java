package io.vanillabp.cockpit.tasklist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The four levels of the setting and which of them decides. The feature tests of the container
 * module run the same table through a started application; what is pinned here are the answers for
 * a level which says nothing, which is what most installations will consist of.
 */
class OpenedTasksPropertiesTest {

    private static OpenedTasksProperties properties(
            final boolean global,
            final Map<String, OpenedTasksProperties.WorkflowModule> workflowModules) {

        final var properties = new OpenedTasksProperties();
        properties.setOpenedTasksStayVisible(global);
        properties.setWorkflowModules(workflowModules);
        return properties;

    }

    private static OpenedTasksProperties.WorkflowModule module(
            final Boolean value,
            final Map<String, OpenedTasksProperties.Workflow> workflows) {

        final var module = new OpenedTasksProperties.WorkflowModule();
        module.setOpenedTasksStayVisible(value);
        module.setWorkflows(workflows);
        return module;

    }

    private static OpenedTasksProperties.Workflow workflow(
            final Boolean value,
            final Map<String, OpenedTasksProperties.UserTask> userTasks) {

        final var workflow = new OpenedTasksProperties.Workflow();
        workflow.setOpenedTasksStayVisible(value);
        workflow.setUserTasks(userTasks);
        return workflow;

    }

    private static OpenedTasksProperties.UserTask userTask(
            final Boolean value) {

        final var userTask = new OpenedTasksProperties.UserTask();
        userTask.setOpenedTasksStayVisible(value);
        return userTask;

    }

    @Test
    void anApplicationWritingNothingKeepsTasksVisible() {

        assertThat(new OpenedTasksProperties()
                .openedTasksStayVisible("any-module", "any-workflow", "any-task"))
                .isTrue();

    }

    @Test
    void theGlobalValueAnswersEveryTaskNobodySpokeAbout() {

        final var properties = properties(false, Map.of());

        assertThat(properties.openedTasksStayVisible("taxi-ride", "TaxiRide", "approve")).isFalse();

    }

    @Test
    void aWorkflowModuleAnswersForItsWorkflowsAndTasks() {

        final var properties = properties(false, Map.of(
                "taxi-ride", module(true, Map.of())));

        assertThat(properties.openedTasksStayVisible("taxi-ride", "TaxiRide", "approve")).isTrue();
        assertThat(properties.openedTasksStayVisible("another-module", "TaxiRide", "approve"))
                .isFalse();

    }

    @Test
    void aWorkflowBeatsItsWorkflowModule() {

        final var properties = properties(true, Map.of(
                "taxi-ride", module(false, Map.of(
                        "TaxiRide", workflow(true, Map.of())))));

        assertThat(properties.openedTasksStayVisible("taxi-ride", "TaxiRide", "approve")).isTrue();
        assertThat(properties.openedTasksStayVisible("taxi-ride", "AnotherRide", "approve"))
                .isFalse();

    }

    @Test
    void aUserTaskBeatsItsWorkflow() {

        final var properties = properties(true, Map.of(
                "taxi-ride", module(true, Map.of(
                        "TaxiRide", workflow(true, Map.of(
                                "approve", userTask(false)))))));

        assertThat(properties.openedTasksStayVisible("taxi-ride", "TaxiRide", "approve")).isFalse();
        assertThat(properties.openedTasksStayVisible("taxi-ride", "TaxiRide", "pay")).isTrue();

    }

    /** A level written without a value of its own is a level answered by the one above it. */
    @Test
    void aLevelWhichOnlyCarriesItsChildrenDecidesNothing() {

        final var properties = properties(false, Map.of(
                "taxi-ride", module(null, Map.of(
                        "TaxiRide", workflow(null, Map.of(
                                "approve", userTask(true)))))));

        assertThat(properties.openedTasksStayVisible("taxi-ride", "TaxiRide", "approve")).isTrue();
        assertThat(properties.openedTasksStayVisible("taxi-ride", "TaxiRide", "pay")).isFalse();

    }

}
