package io.vanillabp.cockpit.extension.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.integration.extension.spi.service.AggregateServiceContext;
import io.vanillabp.integration.extension.spi.service.AggregateServiceFactory;
import io.vanillabp.spi.cockpit.BusinessCockpitService;
import io.vanillabp.spi.cockpit.usertask.UserTask;

/**
 * Builds the <code>BusinessCockpitService&lt;A&gt;</code> a workflow service injects, one per
 * workflow aggregate class of the application.
 * <p>
 * Injecting it is optional. VanillaBP builds the bean when something asks for it, so an
 * application which never reports a change of its own never has one - which is the one thing
 * about this service that changed from version 1, where the primary process of a workflow
 * service had to inject it whether it used it or not.
 */
public class BusinessCockpitServiceFactory implements AggregateServiceFactory<BusinessCockpitService> {

  private final BusinessCockpitExtension extension;

  /**
   * @param extension The extension the service reports through
   */
  public BusinessCockpitServiceFactory(
      final BusinessCockpitExtension extension) {

    this.extension = extension;

  }

  @Override
  public Class<BusinessCockpitService> getServiceInterface() {

    return BusinessCockpitService.class;

  }

  @Override
  public BusinessCockpitService createService(
      final AggregateServiceContext context) {

    return new BusinessCockpitService<Object>() {

      @Override
      public void aggregateChanged(
          final Object workflowAggregate) {

        bridgeOf(workflowAggregate)
            .workflowsOfAggregate(
                context.getWorkflowModuleId(),
                context.getBpmnProcessId(),
                aggregateIdOf(workflowAggregate))
            .forEach(
                workflow -> extension
                    .publishWorkflowEvent(
                        workflow, WorkflowEventKind.UPDATED, null, OffsetDateTime.now(),
                        EventTransaction.CURRENT));

      }

      @Override
      public void aggregateChanged(
          final Object workflowAggregate,
          final String... userTaskIds) {

        bridgeOf(workflowAggregate)
            .userTasksOfAggregate(
                context.getWorkflowModuleId(),
                context.getBpmnProcessId(),
                aggregateIdOf(workflowAggregate),
                userTaskIds == null ? List.of() : List.of(userTaskIds))
            .forEach(
                userTask -> extension
                    .publishUserTaskEvent(
                        userTask, UserTaskEventKind.UPDATED, null, OffsetDateTime.now(),
                        EventTransaction.CURRENT));

      }

      @Override
      public Optional<UserTask> getUserTask(
          final Object workflowAggregate,
          final String userTaskId) {

        final var bridge = bridgeOf(workflowAggregate);
        return bridge
            .userTaskOfAggregate(
                context.getWorkflowModuleId(),
                context.getBpmnProcessId(),
                aggregateIdOf(workflowAggregate),
                userTaskId)
            .flatMap(userTask -> extension.readUserTask(bridge, userTask))
            .map(UserTask.class::cast);

      }

      private io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge bridgeOf(
          final Object workflowAggregate) {

        return extension
            .bridgeOf(
                context
                    .getElection()
                    .adapterIdOfWorkflow(
                        context.getWorkflowModuleId(),
                        context.getBpmnProcessId(),
                        context.getWorkflowAggregateId(workflowAggregate)));

      }

      private String aggregateIdOf(
          final Object workflowAggregate) {

        return String.valueOf(context.getWorkflowAggregateId(workflowAggregate));

      }

    };

  }

}
