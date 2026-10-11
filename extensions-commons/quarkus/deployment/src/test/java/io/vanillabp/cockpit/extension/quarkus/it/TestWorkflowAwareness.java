package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.bpmsdouble.DummyTaskAwarenessSource;
import io.vanillabp.integration.adapter.spi.WorkflowAwareness;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * The BPMS double holds every workflow the test starts. The election reads this when the
 * application asks which BPMS runs the workflow of a workflow aggregate.
 */
@ApplicationScoped
public class TestWorkflowAwareness implements DummyTaskAwarenessSource {

  @Override
  public WorkflowAwareness awarenessOfTask(
      final String adapterId,
      final Object workflowAggregateId,
      final String taskId) {

    return WorkflowAwareness.ACTIVE;

  }

}
