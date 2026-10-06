package io.vanillabp.cockpit.extension.service;

import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.integration.extension.spi.election.OpenUserTask;
import io.vanillabp.integration.extension.spi.election.WorkflowStart;
import io.vanillabp.integration.extension.spi.service.AggregateServiceContext;
import io.vanillabp.integration.extension.spi.service.AggregateServiceFactory;
import io.vanillabp.spi.cockpit.BusinessCockpitService;
import io.vanillabp.spi.cockpit.usertask.UserTask;

/**
 * Builds the <code>BusinessCockpitService&lt;A&gt;</code> a workflow service injects, one per
 * workflow aggregate class of the application.
 * <p>
 * Injecting it is optional. VanillaBP builds the bean when something asks for it, so an
 * application which never reports a change of its own never has one. That is the one thing
 * about this service which changed from version 1, where the primary process of a workflow
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
        // nothing here may wait for a BPMS or read a storage it writes behind its engine: this
        // runs in the application's transaction. So the note VanillaBP wrote at the start names
        // the adapter where it can, and a BPMS half which cannot name the workflows without such
        // a read gets an entry which is resolved when it is dispatched
        final var start = context
            .getElection()
            .workflowStartOf(
                context.getWorkflowModuleId(),
                context.getBpmnProcessId(),
                context.getWorkflowAggregateId(workflowAggregate));
        final var timestamp = OffsetDateTime.now();
        final var found = bridgeOfWorkflow(start);
        if (found.isEmpty()) {
          // several adapters, and nothing VanillaBP wrote down names the one of this aggregate.
          // Electing it here may ask a BPMS and wait for it in the application's transaction, so
          // the entry is written without an adapter, and its dispatch elects it
          extension
              .publishWorkflowChangeResolvedWhenDispatched(
                  null,
                  context.getWorkflowModuleId(),
                  context.getBpmnProcessId(),
                  aggregateIdOf(workflowAggregate),
                  null,
                  null,
                  timestamp,
                  context.getWorkflowAggregateClass());
          return;
        }
        final var bridge = found.get();
        bridge
            .workflowsOfAggregateRightAway(
                context.getWorkflowModuleId(),
                context.getBpmnProcessId(),
                aggregateIdOf(workflowAggregate))
            .ifPresentOrElse(
                workflows -> workflows
                    .forEach(
                        workflow -> extension
                            .publishWorkflowEvent(
                                workflow, WorkflowEventKind.UPDATED, null, timestamp,
                                EventTransaction.CURRENT, context.getWorkflowAggregateClass())),
                () -> extension
                    .publishWorkflowChangeResolvedWhenDispatched(
                        bridge.adapterId(),
                        context.getWorkflowModuleId(),
                        context.getBpmnProcessId(),
                        aggregateIdOf(workflowAggregate),
                        start
                            .filter(written -> writtenFor(bridge, written))
                            .map(WorkflowStart::workflowId)
                            .orElse(null),
                        start
                            .filter(written -> writtenFor(bridge, written))
                            .map(WorkflowStart::processVersion)
                            .orElse(null),
                        timestamp,
                        context.getWorkflowAggregateClass()));

      }

      @Override
      public void aggregateChanged(
          final Object workflowAggregate,
          final String... userTaskIds) {

        requireATransactionToReport(
            extension.reportsUserTasks(),
            "the change of the user tasks of workflow aggregate '%s'"
                .formatted(aggregateIdOf(workflowAggregate)));
        final var named = userTaskIds == null ? List.<String>of() : List.of(userTaskIds);
        // nothing here may wait for a BPMS or read a storage it writes behind its engine, for
        // the reason given at aggregateChanged(aggregate). The note of the start names the
        // adapter where it can, and the open tasks VanillaBP wrote down name it otherwise
        final var start = context
            .getElection()
            .workflowStartOf(
                context.getWorkflowModuleId(),
                context.getBpmnProcessId(),
                context.getWorkflowAggregateId(workflowAggregate));
        final var openUserTasks = new OpenUserTasks(workflowAggregate);
        final var found = bridgeOfUserTasks(start, openUserTasks, List.of());
        final var timestamp = OffsetDateTime.now();
        if (found.isEmpty()) {
          // several adapters, and nothing VanillaBP wrote down names the one of this aggregate.
          // The entries are written without an adapter for the reason given at
          // aggregateChanged(aggregate), and their dispatch elects it
          userTasksResolvedWhenDispatched(workflowAggregate, null, named, List.of())
              .forEach(
                  userTask -> extension
                      .publishUserTaskChangeResolvedWhenDispatched(
                          userTask, timestamp, context.getWorkflowAggregateClass()));
          return;
        }
        final var bridge = found.get();
        if (bridge.reportsAChangedUserTaskRightAway()) {
          // a task the BPMS names none of is left out of the loop below, so no entry is written
          // for it and the cockpit keeps the data it stored before. That is all an empty answer
          // does here. It does not end the task, and it takes the task off no cockpit list:
          // only the BPMS' own report of an end does that
          bridge
              .userTasksOfAggregate(
                  context.getWorkflowModuleId(),
                  context.getBpmnProcessId(),
                  aggregateIdOf(workflowAggregate),
                  named)
              .forEach(
                  userTask -> extension
                      .publishUserTaskEvent(
                          userTask, UserTaskEventKind.UPDATED, null, timestamp,
                          EventTransaction.CURRENT, context.getWorkflowAggregateClass()));
          return;
        }
        userTasksResolvedWhenDispatched(workflowAggregate, bridge.adapterId(), named, openUserTasks.get())
            .forEach(
                userTask -> extension
                    .publishUserTaskChangeResolvedWhenDispatched(
                        userTask, timestamp, context.getWorkflowAggregateClass()));

      }

      /**
       * The tasks of a change whose reports are built when their entries are dispatched, as far
       * as VanillaBP wrote them down when it delivered them.
       * <p>
       * A task VanillaBP wrote down with the workflow of its case gets a reference with all its
       * identifiers. A named task it did not write down, or one whose case it cannot name, gets
       * a reference with the task's id alone, and the dispatch looks it up. Where the application
       * named no task and VanillaBP knows of no open one, a single reference without a task makes
       * the dispatch look up every open task of the aggregate. That covers tasks delivered before
       * VanillaBP wrote them down, and a delivery log which cannot be read.
       *
       * @param adapterId The adapter of the tasks, or <code>null</code> where nothing VanillaBP
       *          wrote down names it. The references carry no adapter then, and the dispatch
       *          elects it
       */
      private List<UserTaskReference> userTasksResolvedWhenDispatched(
          final Object workflowAggregate,
          final String adapterId,
          final List<String> named,
          final List<OpenUserTask> openUserTasks) {

        final var written = openUserTasks
            .stream()
            .filter(open -> (adapterId != null) && adapterId.equals(open.adapterId()))
            .filter(open -> named.isEmpty() || named.contains(open.userTaskId()))
            .map(open -> referenceOf(workflowAggregate, adapterId, open))
            .toList();
        if (named.isEmpty()) {
          return written.isEmpty()
              ? List.of(referenceOf(workflowAggregate, adapterId, (String) null))
              : written;
        }
        final var references = new LinkedList<>(written);
        named
            .stream()
            .distinct()
            .filter(userTaskId -> written.stream().noneMatch(open -> userTaskId.equals(open.userTaskId())))
            .map(userTaskId -> referenceOf(workflowAggregate, adapterId, userTaskId))
            .forEach(references::add);
        return references;

      }

      /**
       * A task as VanillaBP wrote it down when it delivered it.
       * <p>
       * The workflow of a task is its case, which is the workflow of the aggregate. VanillaBP
       * names it from the note of the start. Without that note, a task of the aggregate's own
       * BPMN process runs in the case itself, so the instance it runs in is the case. A task of
       * a called process without the note gets the id alone, and the dispatch asks the BPMS.
       */
      private UserTaskReference referenceOf(
          final Object workflowAggregate,
          final String adapterId,
          final OpenUserTask open) {

        final var workflowId = open.workflowId() != null
            ? open.workflowId()
            : context.getBpmnProcessId().equals(open.bpmnProcessId())
                ? open.subWorkflowId()
                : null;
        if (workflowId == null) {
          return referenceOf(workflowAggregate, adapterId, open.userTaskId());
        }
        return new UserTaskReference(
            adapterId, context.getWorkflowModuleId(), open.bpmnProcessId(), open
                .processVersion(), aggregateIdOf(workflowAggregate), workflowId, open
                    .userTaskId(), open.taskDefinition(), open.bpmnElementId());

      }

      /**
       * A task named by its id alone, or no task at all, which the dispatch looks up in the
       * aggregate's own BPMN process.
       */
      private UserTaskReference referenceOf(
          final Object workflowAggregate,
          final String adapterId,
          final String userTaskId) {

        return new UserTaskReference(
            adapterId, context.getWorkflowModuleId(), context
                .getBpmnProcessId(), null, aggregateIdOf(workflowAggregate), null, userTaskId, null, null);

      }

      /**
       * The BPMS half holding the user tasks of an aggregate, found without asking a BPMS where
       * that is possible. It is the half of the aggregate's workflows, found the same way, with
       * one more source before the election: the adapter which delivered the aggregate's open
       * tasks.
       *
       * @param userTaskIds The tasks whose adapter counts, or empty where every open task of the
       *          aggregate counts
       * @return The half, or empty where nothing VanillaBP wrote down names the adapter and the
       *         application configured several. Only the election can answer then
       */
      private Optional<BusinessCockpitBpmsBridge> bridgeOfUserTasks(
          final Optional<WorkflowStart> start,
          final OpenUserTasks openUserTasks,
          final List<String> userTaskIds) {

        return start
            .map(WorkflowStart::adapterId)
            .filter(extension::hasABridgeFor)
            .map(extension::bridgeOf)
            .or(extension::theOnlyBridge)
            .or(
                () -> openUserTasks
                    .get()
                    .stream()
                    .filter(open -> userTaskIds.isEmpty() || userTaskIds.contains(open.userTaskId()))
                    .map(OpenUserTask::adapterId)
                    .filter(extension::hasABridgeFor)
                    .findFirst()
                    .map(extension::bridgeOf));

      }

      /**
       * The open user tasks VanillaBP wrote down for one aggregate, read once and only where they
       * are needed. A half which builds its reports right away needs them only where nothing
       * else names its adapter.
       */
      private final class OpenUserTasks {

        private final Object workflowAggregate;

        private List<OpenUserTask> read;

        private OpenUserTasks(
            final Object workflowAggregate) {

          this.workflowAggregate = workflowAggregate;

        }

        private List<OpenUserTask> get() {

          if (read == null) {
            read = context
                .getElection()
                .openUserTasksOf(
                    context.getWorkflowModuleId(),
                    context.getBpmnProcessId(),
                    context.getWorkflowAggregateId(workflowAggregate));
          }
          return read;

        }

      }

      @Override
      public Optional<UserTask> getUserTask(
          final Object workflowAggregate,
          final String userTaskId) {

        // one transaction around the whole question, the caller's where the caller has one. The
        // BPMS is asked which task it holds, and the aggregate is read for the details provider.
        // An answer built from two units of work could describe two different moments
        return extension
            .readInOneTransaction(
                context.getWorkflowAggregateClass(),
                () -> {
                  // an empty answer of the BPMS is handed to the application as it is. It
                  // says the BPMS said nothing about that task, which is not the same as the
                  // task being over, and the javadoc of getUserTask tells the caller so. The
                  // details are not read for it either, because flatMap has nothing to read
                  // them for.
                  // The half is found the way a report finds it, so the election, which may
                  // wait for a BPMS, is asked only where nothing VanillaBP wrote down names
                  // the adapter. Of the open tasks only the one asked about counts, because
                  // its adapter is the one which holds it
                  final var start = context
                      .getElection()
                      .workflowStartOf(
                          context.getWorkflowModuleId(),
                          context.getBpmnProcessId(),
                          context.getWorkflowAggregateId(workflowAggregate));
                  final var bridge = bridgeOfUserTasks(
                      start,
                      new OpenUserTasks(workflowAggregate),
                      userTaskId == null ? List.of() : List.of(userTaskId))
                      .orElseGet(() -> bridgeOf(workflowAggregate));
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
       * reports, so nothing is reported for a change which was rolled back. Without a
       * transaction there is nothing to write it into. The platform's own refusal speaks of an
       * adapter reporting from a worker thread, which is not what happened here.
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

      /**
       * The BPMS half holding the workflows of an aggregate, found without asking a BPMS where
       * that is possible.
       * <p>
       * The adapter the note of the start names comes first, because a workflow does not change
       * its BPMS. The one adapter of an application with only one comes next. The election is not
       * asked here, because it may ask a BPMS and wait for it.
       *
       * @return The half, or empty where the application configured several adapters and no note
       *         is left. Only the election can answer then
       */
      private Optional<BusinessCockpitBpmsBridge> bridgeOfWorkflow(
          final Optional<WorkflowStart> start) {

        return start
            .map(WorkflowStart::adapterId)
            .filter(extension::hasABridgeFor)
            .map(extension::bridgeOf)
            .or(extension::theOnlyBridge);

      }

      /**
       * Whether a note of the start belongs to the BPMS this half serves. A note which names no
       * adapter comes from VanillaBP's election cache, and it is taken as well: the half decides
       * itself whether the id is one of its BPMS.
       */
      private static boolean writtenFor(
          final BusinessCockpitBpmsBridge bridge,
          final WorkflowStart start) {

        return (start.adapterId() == null) || start.adapterId().equals(bridge.adapterId());

      }

      /**
       * The BPMS half the election names. Only a read asks this: a read may wait for a BPMS, a
       * report never does.
       */
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
