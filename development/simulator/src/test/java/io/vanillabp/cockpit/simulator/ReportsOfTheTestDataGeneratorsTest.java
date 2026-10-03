package io.vanillabp.cockpit.simulator;

import static org.assertj.core.api.Assertions.assertThat;

import com.devskiller.jfairy.Fairy;
import io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCreatedEvent;
import io.vanillabp.cockpit.simulator.common.FairyHelper;
import io.vanillabp.cockpit.simulator.usertask.testdata.UserTaskTestDataGenerator;
import io.vanillabp.cockpit.simulator.usertask.testdata.UserTaskTestDataParameters;
import io.vanillabp.cockpit.simulator.workflow.testdata.WorkflowTestDataGenerator;
import io.vanillabp.cockpit.simulator.workflow.testdata.WorkflowTestDataParameters;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The reports the test data generators send about a user task or a case they created before.
 *
 * <p>The cockpit validates every report against the schema of its API, and it refuses a report
 * whose body names another user task or case than its path. The generators send the id of the
 * created event in the path. So each report has to name the same id in its body and carry every
 * field the schema asks for. Otherwise the cockpit answers {@code 400 Bad Request}, and the
 * simulator shows no end at all.
 *
 * <p>The generated client classes carry no validation, so the fields are checked against the
 * {@code required} lists of {@code apis/bpms-api/openapi/v1_1.yaml}. The server classes generated
 * from the same file mark them {@code @NotNull}. Nothing is sent.
 */
@ExtendWith(SuppressOutputExtension.class)
class ReportsOfTheTestDataGeneratorsTest {

    /** {@code UserTaskUpdatedEvent}, and the creation and the ends which are built from it */
    private static final List<String> REQUIRED_OF_A_USER_TASK_REPORT = List.of(
            "id", "userTaskId", "timestamp", "bpmnProcessId", "taskDefinition", "title", "uiUriPath", "uiUriType");

    /** {@code UserTaskLifecycleEvent}, which suspension and activation are built from */
    private static final List<String> REQUIRED_OF_A_USER_TASK_LIFECYCLE_REPORT = List.of(
            "id", "userTaskId", "timestamp");

    /** {@code WorkflowUpdatedEvent}, and the creation and the ends which are built from it */
    private static final List<String> REQUIRED_OF_A_CASE_REPORT = List.of(
            "id", "workflowId", "timestamp", "bpmnProcessId", "workflowModuleId", "uiUriPath", "uiUriType");

    private static Map<String, Fairy> fairies;

    @BeforeAll
    static void setUp() {

        fairies = Map.of("en", FairyHelper.buildFairy("en"));

    }

    private static void assertValidReport(
            final Object report,
            final List<String> required,
            final String idInBody,
            final String idInPath) {

        final var name = report.getClass().getSimpleName();
        assertThat(idInBody)
                .as("the body of %s has to name the id of the path", name)
                .isEqualTo(idInPath);
        final var properties = PropertyAccessorFactory.forBeanPropertyAccess(report);
        assertThat(required)
                .as("the fields the schema asks of %s", name)
                .allSatisfy(field -> assertThat(properties.getPropertyValue(field)).as(field).isNotNull());

    }

    @Test
    void everyReportAboutAUserTaskNamesTheTaskOfItsPath() {

        final var generator = new UserTaskTestDataGenerator(
                1, 1, null, new String[] { "anna" }, new String[] { "drivers" }, fairies,
                new UserTaskTestDataParameters());
        final var created = UserTaskTestDataGenerator.buildCreatedEvent(
                new Random(1), fairies, null, null, null);
        // the creation is the event whose id every later report uses in its path
        assertValidReport(created, REQUIRED_OF_A_USER_TASK_REPORT, created.getUserTaskId(), created.getUserTaskId());
        final var idInPath = created.getUserTaskId();

        final io.vanillabp.cockpit.bpms.api.v1_1.UserTaskUpdatedEvent updated =
                ReflectionTestUtils.invokeMethod(generator, "buildUpdatedEvent", created);
        assertValidReport(updated, REQUIRED_OF_A_USER_TASK_REPORT, updated.getUserTaskId(), idInPath);

        final io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCompletedEvent completed =
                ReflectionTestUtils.invokeMethod(generator, "buildCompletedEvent", created);
        assertValidReport(completed, REQUIRED_OF_A_USER_TASK_REPORT, completed.getUserTaskId(), idInPath);

        final io.vanillabp.cockpit.bpms.api.v1_1.UserTaskCancelledEvent cancelled =
                ReflectionTestUtils.invokeMethod(generator, "buildCancelledEvent", created);
        assertValidReport(cancelled, REQUIRED_OF_A_USER_TASK_REPORT, cancelled.getUserTaskId(), idInPath);

        final io.vanillabp.cockpit.bpms.api.v1_1.UserTaskSuspendedEvent suspended =
                ReflectionTestUtils.invokeMethod(generator, "buildSuspendedEvent", created);
        assertValidReport(suspended, REQUIRED_OF_A_USER_TASK_LIFECYCLE_REPORT, suspended.getUserTaskId(), idInPath);

        final io.vanillabp.cockpit.bpms.api.v1_1.UserTaskActivatedEvent activated =
                ReflectionTestUtils.invokeMethod(generator, "buildActivatedEvent", created);
        assertValidReport(activated, REQUIRED_OF_A_USER_TASK_LIFECYCLE_REPORT, activated.getUserTaskId(), idInPath);

    }

    @Test
    void everyReportAboutACaseNamesTheCaseOfItsPath() {

        final var generator = new WorkflowTestDataGenerator(
                1, 1, null, fairies, new WorkflowTestDataParameters());
        final WorkflowCreatedEvent created = ReflectionTestUtils.invokeMethod(generator, "buildCreatedEvent");
        assertValidReport(created, REQUIRED_OF_A_CASE_REPORT, created.getWorkflowId(), created.getWorkflowId());
        final var idInPath = created.getWorkflowId();

        final io.vanillabp.cockpit.bpms.api.v1_1.WorkflowUpdatedEvent updated =
                ReflectionTestUtils.invokeMethod(generator, "buildUpdatedEvent", created);
        assertValidReport(updated, REQUIRED_OF_A_CASE_REPORT, updated.getWorkflowId(), idInPath);

        final io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCompletedEvent completed =
                ReflectionTestUtils.invokeMethod(generator, "buildCompletedEvent", created);
        assertValidReport(completed, REQUIRED_OF_A_CASE_REPORT, completed.getWorkflowId(), idInPath);

        final io.vanillabp.cockpit.bpms.api.v1_1.WorkflowCancelledEvent cancelled =
                ReflectionTestUtils.invokeMethod(generator, "buildCancelledEvent", created);
        assertValidReport(cancelled, REQUIRED_OF_A_CASE_REPORT, cancelled.getWorkflowId(), idInPath);

    }

}
