package io.vanillabp.cockpit.devshell.simulator.businesscockpit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.gui.api.v1.WorkflowRetrieveMode;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The simulator plays the official GUI API, so it takes the deprecated names 'Active' and
 * 'Inactive' as well, and gives them the same list as 'InProgress' and 'Done'.
 */
@ExtendWith(SuppressOutputExtension.class)
class TheSimulatorReadsTheOldNamesOfTheWorkflowModesTest {

  private final OfficialApiMapper mapper = new OfficialApiMapperImpl();

  @Test
  void activeIsInProgressAndInactiveIsDone() {

    assertThat(mapper.toModel(WorkflowRetrieveMode.INPROGRESS))
        .isEqualTo(WorkflowService.RetrieveMode.INPROGRESS);
    assertThat(mapper.toModel(WorkflowRetrieveMode.ACTIVE))
        .isEqualTo(WorkflowService.RetrieveMode.INPROGRESS);
    assertThat(mapper.toModel(WorkflowRetrieveMode.DONE))
        .isEqualTo(WorkflowService.RetrieveMode.DONE);
    assertThat(mapper.toModel(WorkflowRetrieveMode.INACTIVE))
        .isEqualTo(WorkflowService.RetrieveMode.DONE);

  }

  @Test
  void withoutAModeTheSimulatorShowsEveryWorkflow() {

    assertThat(mapper.toModel((WorkflowRetrieveMode) null))
        .isEqualTo(WorkflowService.RetrieveMode.ALL);
    assertThat(mapper.toModel(WorkflowRetrieveMode.ALL))
        .isEqualTo(WorkflowService.RetrieveMode.ALL);

  }

}
