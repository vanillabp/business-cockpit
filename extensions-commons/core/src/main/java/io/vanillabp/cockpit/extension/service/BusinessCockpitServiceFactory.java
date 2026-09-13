package io.vanillabp.cockpit.extension.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
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

        requireATransactionToReport(
            extension.reportsWorkflows(),
            "the change of workflow aggregate '%s'".formatted(aggregateIdOf(workflowAggregate)));
        bridgeOf(workflowAggregate)
            .workflowsOfAggregate(
                context.getWorkflowModuleId(),
                context.getBpmnProcessId(),
                aggregateIdOf(workflowAggregate))
            .forEach(
                workflow -> extension
                    .publishWorkflowEvent(
                        workflow, WorkflowEventKind.UPDATED, null, OffsetDateTime.now(),
                        EventTransaction.CURRENT, context.getWorkflowAggregateClass()));

      }

      @Override
      public void aggregateChanged(
          final Object workflowAggregate,
          final String... userTaskIds) {

        requireATransactionToReport(
            extension.reportsUserTasks(),
            "the change of the user tasks of workflow aggregate '%s'"
                .formatted(aggregateIdOf(workflowAggregate)));
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
                        EventTransaction.CURRENT, context.getWorkflowAggregateClass()));

      }

      @Override
      public Optional<UserTask> getUserTask(
          final Object workflowAggregate,
          final String userTaskId) {

        // one transaction around the whole question, the caller's where the caller has one: the
        // BPMS is asked which task it holds, the aggregate is read for the details provider, and
        // an answer built from two units of work could describe two different moments
        return extension
            .readInOneTransaction(
                context.getWorkflowAggregateClass(),
                () -> {
                  final var bridge = bridgeOf(workflowAggregate);
                  return bridge
                      .userTaskOfAggregate(
                          context.getWorkflowModuleId(),
                          context.getBpmnProcessId(),
                          aggregateIdOf(workflowAggregate),
                          userTaskId)
                      .flatMap(userTask -> extension.readUserTask(bridge, userTask))
                      .map(UserTask.class::cast);
                });

      }

      /**
       * Refuses a report where the calling thread runs no transaction.
       * <p>
       * The entry of a report is written into the transaction which persists the change it
       * reports, so that nothing is reported for a change which was rolled back. Without a
       * transaction there is nothing to write it into, and the platform's own refusal speaks of
       * an adapter reporting from a worker thread, which is not what happened here.
       * <p>
       * A report nobody writes needs nothing. An application which switched the two lists off,
       * and a workflow module which configured nothing about the cockpit, carry on without a
       * transaction the way they did before anybody asked for one.
       *
       * @param anythingIsReported Whether this kind of report is switched on at all
       * @param what The report, in a form fitting "Reporting ... to the Business Cockpit"
       */
      private void requireATransactionToReport(
          final boolean anythingIsReported,
          final String what) {

        if (!anythingIsReported) {
          return;
        }
        if (!extension.reportsAnythingOf(context.getWorkflowModuleId())) {
          return;
        }
        if (extension.aTransactionIsOpenFor(context.getWorkflowAggregateClass())) {
          return;
        }
        throw new IllegalStateException(
            """
                Reporting %s to the Business Cockpit needs a transaction, and the thread calling \
                BusinessCockpitService runs none. A report is written into VanillaBP's outbox \
                together with the change it is about, so that the cockpit never hears of a change \
                which was rolled back, and there is nothing here to write it into. Open a \
                transaction around the change and the report: annotate the method doing both with \
                '@Transactional' (BPMN process '%s' of workflow module '%s'). Reading through \
                this service needs no transaction - only reporting does."""
                .formatted(what, context.getBpmnProcessId(), context.getWorkflowModuleId()));

      }

      private BusinessCockpitBpmsBridge bridgeOf(
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
