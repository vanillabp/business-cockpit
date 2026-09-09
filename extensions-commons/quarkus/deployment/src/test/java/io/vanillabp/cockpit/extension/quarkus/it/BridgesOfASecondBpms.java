package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.List;
import java.util.Optional;

import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.spi.UserTaskDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * The second shape a BPMS half may be registered in: one bean holding the bridges of every
 * adapter id it serves. A half serving several configured adapter ids cannot say at build time
 * how many that is, so on Quarkus this is the only shape it has.
 */
@ApplicationScoped
public class BridgesOfASecondBpms {

  /** The adapter the bridges of this list serve. */
  public static final String ADAPTER_ID = "second";

  @Produces
  @Singleton
  public List<BusinessCockpitBpmsBridge> bridgesOfTheSecondBpms() {

    return List.of(new SecondBpmsBridge());

  }

  /** A BPMS half which knows nothing: the test asks which bridge serves an adapter, no more. */
  public static class SecondBpmsBridge implements BusinessCockpitBpmsBridge {

    @Override
    public String adapterId() {

      return ADAPTER_ID;

    }

    @Override
    public String adapterType() {

      return "dummy";

    }

    @Override
    public Optional<UserTaskDetailsPrefill> prefilledUserTaskDetails(
        final UserTaskReference userTask) {

      return Optional.empty();

    }

    @Override
    public Optional<WorkflowDetailsPrefill> prefilledWorkflowDetails(
        final WorkflowReference workflow) {

      return Optional.empty();

    }

    @Override
    public List<WorkflowReference> workflowsOfAggregate(
        final String workflowModuleId,
        final String bpmnProcessId,
        final String workflowAggregateId) {

      return List.of();

    }

    @Override
    public List<UserTaskReference> userTasksOfAggregate(
        final String workflowModuleId,
        final String bpmnProcessId,
        final String workflowAggregateId,
        final List<String> userTaskIds) {

      return List.of();

    }

    @Override
    public Optional<UserTaskReference> userTaskOfAggregate(
        final String workflowModuleId,
        final String bpmnProcessId,
        final String workflowAggregateId,
        final String userTaskId) {

      return Optional.empty();

    }

  }

}
