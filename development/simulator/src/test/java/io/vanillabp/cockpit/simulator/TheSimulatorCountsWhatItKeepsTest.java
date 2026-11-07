package io.vanillabp.cockpit.simulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

import io.vanillabp.cockpit.gui.api.v1.Count;
import io.vanillabp.cockpit.gui.api.v1.UserTaskCountFilter;
import io.vanillabp.cockpit.gui.api.v1.UserTaskCountsRequest;
import io.vanillabp.cockpit.gui.api.v1.UserTaskRetrieveMode;
import io.vanillabp.cockpit.gui.api.v1.WorkflowCountFilter;
import io.vanillabp.cockpit.gui.api.v1.WorkflowCountsRequest;
import io.vanillabp.cockpit.gui.api.v1.WorkflowRetrieveMode;
import io.vanillabp.cockpit.simulator.usertask.OfficialTasklistApiController;
import io.vanillabp.cockpit.simulator.usertask.OfficialTasklistApiMapperImpl;
import io.vanillabp.cockpit.simulator.workflow.OfficialWorkflowlistApiController;
import io.vanillabp.cockpit.simulator.workflow.OfficialWorkflowlistApiMapperImpl;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The simulator answers {@code POST /usertask/count} and {@code POST /workflow/count}. It counts the
 * user tasks and workflows it keeps, which are the ones somebody asked for by id, and it counts them
 * by the modes of the cockpit. A filter without a mode counts what the cockpit counts then: the open
 * tasks and the workflows in progress.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheSimulatorCountsWhatItKeepsTest {

  @Test
  void theUserTasksAreCountedByTheirMode() {

    final var controller = new OfficialTasklistApiController();
    ReflectionTestUtils.setField(controller, "mapper", new OfficialTasklistApiMapperImpl());
    controller.getUserTask("open", false);
    controller.getUserTask("ended", false).getBody().setEndedAt(OffsetDateTime.now().minusMinutes(5));
    controller.getUserTask("followed-up", false).getBody().setFollowUpDate(OffsetDateTime.now().plusDays(3));
    controller.getUserTask("open", false).getBody().setFollowUpDate(null);
    controller.getUserTask("followed-up-earlier", false).getBody().setFollowUpDate(OffsetDateTime.now().minusDays(1));

    final var request = new UserTaskCountsRequest().filters(List.of(
        new UserTaskCountFilter().key("all").mode(UserTaskRetrieveMode.ALL),
        new UserTaskCountFilter().key("open").mode(UserTaskRetrieveMode.OPENTASKSWITHFOLLOWUP),
        new UserTaskCountFilter().key("due").mode(UserTaskRetrieveMode.OPENTASKSWITHOUTFOLLOWUP),
        new UserTaskCountFilter().key("waiting").mode(UserTaskRetrieveMode.OPENTASKONLYFOLLOWUP),
        new UserTaskCountFilter().key("closed").mode(UserTaskRetrieveMode.CLOSEDTASKSONLY),
        new UserTaskCountFilter().key("no mode")));
    final var answer = controller.countUserTasks(request, null);

    assertThat(answer.getStatusCode().value()).isEqualTo(200);
    assertThat(answer.getBody().getServerTimestamp()).isNotNull();
    assertThat(answer.getBody().getCounts())
        .extracting(Count::getKey, Count::getCount)
        .containsExactly(
            tuple("all", 4L),
            tuple("open", 3L),
            tuple("due", 2L),
            tuple("waiting", 1L),
            tuple("closed", 1L),
            tuple("no mode", 3L));

  }

  @Test
  void theWorkflowsAreCountedByTheirModeAndTheirBusinessIds() {

    final var controller = new OfficialWorkflowlistApiController();
    ReflectionTestUtils.setField(controller, "mapper", new OfficialWorkflowlistApiMapperImpl());
    controller.getWorkflow("running").getBody().setBusinessId("ride-1");
    controller.getWorkflow("running-too").getBody().setBusinessId("ride-2");
    final var ended = controller.getWorkflow("ended").getBody();
    ended.setBusinessId("ride-1");
    ended.setEndedAt(OffsetDateTime.now().minusMinutes(5));

    final var request = new WorkflowCountsRequest().filters(List.of(
        new WorkflowCountFilter().key("all").mode(WorkflowRetrieveMode.ALL),
        new WorkflowCountFilter().key("in progress").mode(WorkflowRetrieveMode.INPROGRESS),
        new WorkflowCountFilter().key("done").mode(WorkflowRetrieveMode.DONE),
        new WorkflowCountFilter().key("active").mode(WorkflowRetrieveMode.ACTIVE),
        new WorkflowCountFilter().key("inactive").mode(WorkflowRetrieveMode.INACTIVE),
        new WorkflowCountFilter().key("no mode"),
        new WorkflowCountFilter().key("ride-1").mode(WorkflowRetrieveMode.ALL).businessIds(List.of("ride-1"))));
    final var answer = controller.countWorkflows(request, null);

    assertThat(answer.getStatusCode().value()).isEqualTo(200);
    assertThat(answer.getBody().getServerTimestamp()).isNotNull();
    assertThat(answer.getBody().getCounts())
        .extracting(Count::getKey, Count::getCount)
        .containsExactly(
            tuple("all", 3L),
            tuple("in progress", 2L),
            tuple("done", 1L),
            tuple("active", 2L),
            tuple("inactive", 1L),
            tuple("no mode", 2L),
            tuple("ride-1", 2L));

  }

}
