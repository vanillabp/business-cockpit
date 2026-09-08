package io.vanillabp.cockpit.extension;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;
import io.vanillabp.cockpit.extension.handler.BusinessCockpitHandlers;
import io.vanillabp.cockpit.extension.outbox.BusinessCockpitOperations;
import io.vanillabp.cockpit.extension.outbox.BusinessCockpitOutbox;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitBpmsBridge;
import io.vanillabp.cockpit.extension.spi.BusinessCockpitEventPublisher;
import io.vanillabp.cockpit.extension.spi.EventTransaction;
import io.vanillabp.cockpit.extension.spi.UserTaskDetailsPrefill;
import io.vanillabp.cockpit.extension.spi.UserTaskEventKind;
import io.vanillabp.cockpit.extension.spi.UserTaskReference;
import io.vanillabp.cockpit.extension.spi.WorkflowEventKind;
import io.vanillabp.cockpit.extension.spi.WorkflowReference;
import io.vanillabp.cockpit.extension.templating.EventTitles;
import io.vanillabp.cockpit.extension.templating.Templating;
import io.vanillabp.cockpit.extension.transport.BusinessCockpitTransport;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.handler.HandlerCall;
import io.vanillabp.integration.spi.PhaseOperationRegistry;
import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.spi.PhaseTwoPermanentFailure;
import io.vanillabp.integration.spi.TransactionRunner;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;
import io.vanillabp.spi.service.WorkflowService;

/**
 * The Business Cockpit extension, without a single line knowing a BPMS or a platform.
 * <p>
 * Everything an event goes through happens here. A BPMS half reports what its engine observed,
 * which writes one outbox entry inside the transaction the BPMS is in. After that transaction
 * committed, the entry is dispatched: the BPMS is asked what it knows about the task or the
 * workflow now, the application's details provider is invoked to enrich it, the titles are
 * rendered, and the result is handed to the configured transport.
 * <p>
 * Reading at dispatch time rather than carrying the data through the outbox is what makes an
 * entry small enough for the store and what makes repeated updates collapse - see decision 3 in
 * the repository's DECISIONS.md.
 */
public class BusinessCockpitExtension implements BusinessCockpitEventPublisher {

  private static final Logger logger = LoggerFactory.getLogger(BusinessCockpitExtension.class);

  private final BusinessCockpitConfiguration configuration;

  private final BusinessCockpitTransport transport;

  private final Map<String, BusinessCockpitBpmsBridge> bridges;

  private final Map<String, WorkflowModuleDetailsProvider> workflowModuleDetailsProviders;

  private final ExtensionHandlers handlers;

  private final Templating templating;

  private final BusinessCockpitOutbox outbox;

  private final TransactionRunner transactionRunner;

  private final String source;

  private final Set<String> startedWorkflowModules = ConcurrentHashMap.newKeySet();

  /**
   * @param configuration What the application configured, already validated
   * @param transport Where the events go
   * @param bridges The BPMS halves, one per configured adapter id
   * @param workflowModuleDetailsProviders What the application says about its modules
   * @param handlers VanillaBP's invocation of the application's details providers
   * @param templating The renderer of the titles, {@link Templating#none()} without templates
   * @param outbox Which store an entry is written to
   * @param transactionRunner Opens a transaction where the caller brought none
   */
  public BusinessCockpitExtension(
      final BusinessCockpitConfiguration configuration,
      final BusinessCockpitTransport transport,
      final Collection<BusinessCockpitBpmsBridge> bridges,
      final Collection<WorkflowModuleDetailsProvider> workflowModuleDetailsProviders,
      final ExtensionHandlers handlers,
      final Templating templating,
      final BusinessCockpitOutbox outbox,
      final TransactionRunner transactionRunner) {

    this.configuration = configuration;
    this.transport = transport;
    this.handlers = handlers;
    this.templating = templating;
    this.outbox = outbox;
    this.transactionRunner = transactionRunner;
    this.source = sourceOfThisInstance();
    this.bridges = bridgesByAdapterId(bridges);
    this.workflowModuleDetailsProviders = detailsProvidersByWorkflowModule(
        workflowModuleDetailsProviders);

  }

  /**
   * Registers the extension's operations, which is what makes the outbox accept and dispatch
   * them. Called once while the application starts.
   *
   * @param registry VanillaBP's registry of phase-two operations
   */
  public void registerOperations(
      final PhaseOperationRegistry registry) {

    registry
        .register(
            BusinessCockpitOperations.publishUserTaskEvent(),
            (
                call,
                previouslyAttempted) -> dispatchUserTaskEvent(call));
    registry
        .register(
            BusinessCockpitOperations.publishWorkflowEvent(),
            (
                call,
                previouslyAttempted) -> dispatchWorkflowEvent(call));
    registry
        .register(
            BusinessCockpitOperations.registerWorkflowModule(),
            (
                call,
                previouslyAttempted) -> dispatchWorkflowModuleRegistration(call));

  }

  /**
   * Tells VanillaBP how to find and invoke the application's details providers. Called once
   * while the application starts; the order relative to the scanning of the
   * <code>&#64;WorkflowService</code> classes does not matter.
   */
  public void registerHandlerContracts() {

    handlers.register(BusinessCockpitHandlers.userTaskContract());
    handlers.register(BusinessCockpitHandlers.workflowContract());

  }

  /**
   * Refuses what an application wrote into the reserved <code>version</code> attribute of
   * <code>&#64;UserTaskDetailsProvider</code>, naming the method it stands on.
   * <p>
   * The check runs here rather than while VanillaBP scans the annotations, because the callback
   * reading the lookup keys of an annotation is not told which method carries it, and a message
   * about an attribute which does not name the method leaves the developer searching.
   *
   * @param workflowServiceClasses The classes of the application which may carry the annotation
   * @throws IllegalStateException If one of them names a version - see decision 7 in the
   *           repository's DECISIONS.md
   */
  public void validateDetailsProviders(
      final Collection<Class<?>> workflowServiceClasses) {

    workflowServiceClasses.forEach(BusinessCockpitHandlers::rejectReservedVersionAttribute);

  }

  /**
   * Resolves the outbox store of every workflow aggregate of the application, ending the boot
   * where one cannot be attributed.
   * <p>
   * This runs once the application is up rather than while the extension's bean is created:
   * asking for the store of an aggregate reaches into the application's persistence, and
   * VanillaBP's own startup validation waits for the same reason.
   *
   * @param workflowServiceClasses The classes of the application carrying
   *          <code>&#64;WorkflowService</code>
   * @throws IllegalStateException If a store is missing, cannot be attributed to an aggregate,
   *           or the aggregates do not share one - see decision 12 in the repository's
   *           DECISIONS.md
   */
  public void validateOutboxAttribution(
      final Collection<Class<?>> workflowServiceClasses) {

    outbox.validateAtStartup(workflowAggregateClassesOf(workflowServiceClasses));

  }

  /**
   * The workflow aggregates the application's services are written for, each named once. A
   * class serving several BPMN processes names its aggregate in each of them, and several
   * services may share one aggregate.
   *
   * @param workflowServiceClasses The classes carrying <code>&#64;WorkflowService</code>
   * @return Their aggregates
   */
  private static Collection<Class<?>> workflowAggregateClassesOf(
      final Collection<Class<?>> workflowServiceClasses) {

    final Collection<Class<?>> aggregates = new LinkedHashSet<>();
    for (final var workflowServiceClass : workflowServiceClasses) {
      final var annotation = workflowServiceClass.getAnnotation(WorkflowService.class);
      if (annotation != null) {
        aggregates.add(annotation.workflowAggregateClass());
      }
    }
    return aggregates;

  }

  /**
   * Releases what the transport holds. Called when the application shuts down.
   */
  public void stop() {

    transport.close();

  }

  @Override
  public boolean publishUserTaskEvent(
      final UserTaskReference userTask,
      final UserTaskEventKind kind,
      final String bpmsEventId,
      final OffsetDateTime timestamp,
      final EventTransaction transaction) {

    return publishUserTaskEvent(userTask, kind, bpmsEventId, timestamp, transaction, null);

  }

  /**
   * Reports a user task, knowing which workflow aggregate class it belongs to.
   * <p>
   * That is what a report through <code>BusinessCockpitService</code> knows and what a BPMS
   * half does not: the class decides which of the application's outbox stores holds the entry,
   * and the store has to be the one the aggregate's own transaction reaches.
   *
   * @param userTask The task
   * @param kind What happened to it
   * @param bpmsEventId The BPMS' own id of the event, or <code>null</code> for one of ours
   * @param timestamp When it happened
   * @param transaction Whether the entry rides the caller's transaction or gets one of its own
   * @param workflowAggregateClass The aggregate's class, or <code>null</code> where the caller
   *          does not know it
   * @return Whether an entry was written, <code>false</code> where one of the same key is still
   *         waiting to be dispatched
   */
  public boolean publishUserTaskEvent(
      final UserTaskReference userTask,
      final UserTaskEventKind kind,
      final String bpmsEventId,
      final OffsetDateTime timestamp,
      final EventTransaction transaction,
      final Class<?> workflowAggregateClass) {

    final var args = new LinkedHashMap<String, String>();
    put(args, BusinessCockpitOperations.ARG_EVENT_KIND, kind.name());
    put(args, BusinessCockpitOperations.ARG_USER_TASK_ID, userTask.userTaskId());
    put(args, BusinessCockpitOperations.ARG_WORKFLOW_ID, userTask.workflowId());
    put(args, BusinessCockpitOperations.ARG_TASK_DEFINITION, userTask.taskDefinition());
    put(args, BusinessCockpitOperations.ARG_BPMN_TASK_ID, userTask.bpmnTaskId());
    put(args, BusinessCockpitOperations.ARG_EVENT_ID, eventIdOf(bpmsEventId));
    put(args, BusinessCockpitOperations.ARG_TIMESTAMP, timestampOf(timestamp).toString());

    final var call = PhaseTwoCall
        .of(
            BusinessCockpitOperations.publishUserTaskEvent(),
            userTask.workflowModuleId(),
            userTask.bpmnProcessId(),
            userTask.workflowAggregateId(),
            userTask.adapterId(),
            args);
    return schedule(
        call, transaction, workflowAggregateClass, userTask.workflowModuleId(), userTask
            .bpmnProcessId());

  }

  @Override
  public boolean publishWorkflowEvent(
      final WorkflowReference workflow,
      final WorkflowEventKind kind,
      final String bpmsEventId,
      final OffsetDateTime timestamp,
      final EventTransaction transaction) {

    return publishWorkflowEvent(workflow, kind, bpmsEventId, timestamp, transaction, null);

  }

  /**
   * Reports a workflow, knowing which workflow aggregate class it belongs to - the counterpart
   * of {@link #publishUserTaskEvent(UserTaskReference, UserTaskEventKind, String,
   * OffsetDateTime, EventTransaction, Class)} for a workflow.
   *
   * @param workflow The workflow
   * @param kind What happened to it
   * @param bpmsEventId The BPMS' own id of the event, or <code>null</code> for one of ours
   * @param timestamp When it happened
   * @param transaction Whether the entry rides the caller's transaction or gets one of its own
   * @param workflowAggregateClass The aggregate's class, or <code>null</code> where the caller
   *          does not know it
   * @return Whether an entry was written
   */
  public boolean publishWorkflowEvent(
      final WorkflowReference workflow,
      final WorkflowEventKind kind,
      final String bpmsEventId,
      final OffsetDateTime timestamp,
      final EventTransaction transaction,
      final Class<?> workflowAggregateClass) {

    final var args = new LinkedHashMap<String, String>();
    put(args, BusinessCockpitOperations.ARG_EVENT_KIND, kind.name());
    put(args, BusinessCockpitOperations.ARG_WORKFLOW_ID, workflow.workflowId());
    put(args, BusinessCockpitOperations.ARG_EVENT_ID, eventIdOf(bpmsEventId));
    put(args, BusinessCockpitOperations.ARG_TIMESTAMP, timestampOf(timestamp).toString());

    final var call = PhaseTwoCall
        .of(
            BusinessCockpitOperations.publishWorkflowEvent(),
            workflow.workflowModuleId(),
            workflow.bpmnProcessId(),
            workflow.workflowAggregateId(),
            workflow.adapterId(),
            args);
    return schedule(
        call, transaction, workflowAggregateClass, workflow.workflowModuleId(), workflow
            .bpmnProcessId());

  }

  /**
   * Remembers that a workflow module started, so that it can be registered at the cockpit
   * server once the application is up.
   * <p>
   * VanillaBP says this once per module and per adapter the module was deployed to, and the
   * registration is the same either way.
   *
   * @param workflowModuleId The module which started
   */
  public void workflowModuleStarted(
      final String workflowModuleId) {

    startedWorkflowModules.add(workflowModuleId);

  }

  /**
   * Schedules the registration of every workflow module which started.
   * <p>
   * This happens when the application is up rather than while its workflow modules are being
   * deployed, because the outbox store is only ready by then - on Quarkus the store creates its
   * table in a startup observer of its own, which runs after the deployment pipeline.
   * <p>
   * Nothing is sent here. An entry is written per module, and what reaches the cockpit server
   * reaches it afterwards, so a cockpit server which is down does not stop the application from
   * booting: the outbox keeps trying until the server answers.
   * <p>
   * A module which configured nothing about the cockpit is left out, the way it is left out of
   * everything else the extension does.
   */
  public void registerStartedWorkflowModules() {

    startedWorkflowModules
        .stream()
        .filter(configuration::reportsToTheCockpit)
        .forEach(
            workflowModuleId -> transactionRunner
                .requireNew(() -> outbox
                    .ofWorkflowModuleRegistration(workflowModuleId)
                    .schedule(
                        PhaseTwoCall
                            .of(
                                BusinessCockpitOperations.registerWorkflowModule(),
                                workflowModuleId,
                                BusinessCockpitOperations.EVERY_BPMN_PROCESS,
                                null,
                                null,
                                Map.of()))));

  }

  /**
   * Reads the current state of a user task and runs the details provider on it, without
   * reporting anything to the cockpit - what
   * <code>BusinessCockpitService.getUserTask(aggregate, id)</code> answers with.
   *
   * @param bridge The BPMS holding the task
   * @param userTask The task
   * @return The task as the cockpit would show it, or empty where the BPMS does not know it
   */
  public Optional<UserTaskEvent> readUserTask(
      final BusinessCockpitBpmsBridge bridge,
      final UserTaskReference userTask) {

    final var prefill = bridge.prefilledUserTaskDetails(userTask);
    if (prefill.isEmpty()) {
      return Optional.empty();
    }
    final var event = buildUserTaskEvent(
        userTask, UserTaskEventKind.UPDATED, UUID.randomUUID().toString(), OffsetDateTime.now());
    applyPrefill(event, prefill.get());
    invokeUserTaskDetailsProvider(event, userTask, prefill.get(), false);
    fillTitles(event, prefill.get());
    return Optional.of(event);

  }

  /**
   * The BPMS half serving one adapter id.
   * <p>
   * Which half serves a dispatch is decided by the adapter id the entry carries, which the
   * election answered when the event was observed - see decision 5 in the repository's
   * DECISIONS.md.
   *
   * @param adapterId The adapter id an event came from
   * @return The bridge
   * @throws IllegalStateException If no BPMS half is registered for that adapter - the message
   *           names the adapters the application configured, the halves which are registered
   *           and the three artifacts which provide one
   */
  public BusinessCockpitBpmsBridge bridgeOf(
      final String adapterId) {

    final var bridge = bridges.get(adapterId);
    if (bridge != null) {
      return bridge;
    }
    throw new IllegalStateException(
        """
            The Business Cockpit extension has no BPMS half for the adapter '%s'. Configured \
            adapters are: %s, and a BPMS half is registered for: %s. Add the artifact belonging \
            to that adapter's BPMS to your workflow module - \
            'io.vanillabp.businesscockpit:businesscockpit-camunda7-adapter', \
            'io.vanillabp.businesscockpit:businesscockpit-camunda8-adapter' or \
            'io.vanillabp.businesscockpit:businesscockpit-process-engine-api-adapter', each in the \
            variant of your platform."""
            .formatted(
                adapterId,
                listed(configuration.getConfiguredAdapterIds()),
                listed(bridges.keySet())));

  }

  private void dispatchUserTaskEvent(
      final PhaseTwoCall call) {

    final var args = call.args();
    final var kind = UserTaskEventKind
        .valueOf(args.get(BusinessCockpitOperations.ARG_EVENT_KIND));
    final var userTask = new UserTaskReference(
        call.adapterId(), call.workflowModuleId(), call.bpmnProcessId(), call.workflowAggregateId(), args.get(
            BusinessCockpitOperations.ARG_WORKFLOW_ID), args.get(BusinessCockpitOperations.ARG_USER_TASK_ID), args.get(
                BusinessCockpitOperations.ARG_TASK_DEFINITION), args.get(BusinessCockpitOperations.ARG_BPMN_TASK_ID));
    final var event = buildUserTaskEvent(
        userTask, kind, args.get(BusinessCockpitOperations.ARG_EVENT_ID),
        parseTimestamp(args.get(BusinessCockpitOperations.ARG_TIMESTAMP)));

    if (carriesDetails(kind)) {
      final var prefill = bridgeOf(call.adapterId()).prefilledUserTaskDetails(userTask);
      if (prefill.isEmpty()) {
        reportDropped(
            "user task '%s' (workflow '%s' of '%s/%s', aggregate '%s')".formatted(
                userTask.userTaskId(), userTask.workflowId(), userTask.workflowModuleId(),
                userTask.bpmnProcessId(), userTask.workflowAggregateId()),
            kind.name(),
            call.adapterId());
        return;
      }
      applyPrefill(event, prefill.get());
      invokeUserTaskDetailsProvider(event, userTask, prefill.get(), true);
      fillTitles(event, prefill.get());
    }

    transport.publishUserTaskEvent(event);

  }

  private void dispatchWorkflowEvent(
      final PhaseTwoCall call) {

    final var args = call.args();
    final var kind = WorkflowEventKind
        .valueOf(args.get(BusinessCockpitOperations.ARG_EVENT_KIND));
    final var workflow = new WorkflowReference(
        call.adapterId(), call.workflowModuleId(), call.bpmnProcessId(), call.workflowAggregateId(), args
            .get(BusinessCockpitOperations.ARG_WORKFLOW_ID));
    final var module = configuration.workflowModule(call.workflowModuleId());
    final var event = new WorkflowEvent(kind);
    event.setEventId(args.get(BusinessCockpitOperations.ARG_EVENT_ID));
    event.setTimestamp(parseTimestamp(args.get(BusinessCockpitOperations.ARG_TIMESTAMP)));
    event.setSource(source);
    event.setWorkflowId(workflow.workflowId());
    event.setWorkflowModuleId(workflow.workflowModuleId());
    event.setBpmnProcessId(workflow.bpmnProcessId());
    event.setUiUriPath(module.uiUriPath());
    event.setUiUriType(module.uiUriType());

    if (carriesDetails(kind)) {
      final var prefill = bridgeOf(call.adapterId()).prefilledWorkflowDetails(workflow);
      if (prefill.isEmpty()) {
        reportDropped(
            "workflow '%s' of '%s/%s' (aggregate '%s')".formatted(
                workflow.workflowId(), workflow.workflowModuleId(), workflow.bpmnProcessId(),
                workflow.workflowAggregateId()),
            kind.name(),
            call.adapterId());
        return;
      }
      event.setBpmnProcessVersion(prefill.get().bpmnProcessVersion());
      event.setBusinessId(prefill.get().businessId());
      event.setInitiator(prefill.get().initiator());
      final var returned = handlers
          .invoke(
              HandlerCall
                  .of(
                      WorkflowDetailsProvider.class, workflow.workflowModuleId(),
                      workflow.bpmnProcessId())
                  .workflowAggregateId(workflow.workflowAggregateId())
                  .payload(event)
                  .build());
      returned
          .map(WorkflowDetails.class::cast)
          .ifPresent(event::applyReturnedDetails);
      EventTitles.fill(event, module, templating, prefill.get().bpmnProcessName());
    }

    transport.publishWorkflowEvent(event);

  }

  private void dispatchWorkflowModuleRegistration(
      final PhaseTwoCall call) {

    final var module = configuration.workflowModule(call.workflowModuleId());
    final var provider = workflowModuleDetailsProviders.get(call.workflowModuleId());
    final var event = new RegisterWorkflowModuleEvent(
        UUID.randomUUID().toString(), OffsetDateTime.now(), source, module.workflowModuleId(), module
            .workflowModuleUri(), RegisterWorkflowModuleEvent.TASK_PROVIDER_API_URI_PATH, RegisterWorkflowModuleEvent.WORKFLOW_PROVIDER_API_URI_PATH, provider == null
                ? List.of() : provider.getAccessibleToGroups(), module.groupHierarchy());
    transport.registerWorkflowModule(event);

  }

  private UserTaskEvent buildUserTaskEvent(
      final UserTaskReference userTask,
      final UserTaskEventKind kind,
      final String eventId,
      final OffsetDateTime timestamp) {

    final var module = configuration.workflowModule(userTask.workflowModuleId());
    final var event = new UserTaskEvent(kind);
    event.setEventId(eventId);
    event.setTimestamp(timestamp);
    event.setSource(source);
    event.setUserTaskId(userTask.userTaskId());
    event.setWorkflowId(userTask.workflowId());
    event.setWorkflowModuleId(userTask.workflowModuleId());
    event.setBpmnProcessId(userTask.bpmnProcessId());
    event.setTaskDefinition(userTask.taskDefinition());
    event.setBpmnTaskId(userTask.bpmnTaskId());
    event.setUiUriPath(module.uiUriPath());
    event.setUiUriType(module.uiUriType());
    return event;

  }

  private void applyPrefill(
      final UserTaskEvent event,
      final UserTaskDetailsPrefill prefill) {

    event.setBpmnProcessVersion(prefill.bpmnProcessVersion());
    event.setBusinessId(prefill.businessId());
    event.setSubWorkflowId(prefill.subWorkflowId());
    event.setInitiator(prefill.initiator());
    event.setAssignee(prefill.assignee());
    event.setCandidateUsers(prefill.candidateUsers());
    event.setCandidateGroups(prefill.candidateGroups());
    event.setDueDate(prefill.dueDate());
    event.setFollowUpDate(prefill.followUpDate());
    if (prefill.workflowId() != null) {
      event.setWorkflowId(prefill.workflowId());
    }

  }

  private void invokeUserTaskDetailsProvider(
      final UserTaskEvent event,
      final UserTaskReference userTask,
      final UserTaskDetailsPrefill prefill,
      final boolean savingTheWorkflowAggregate) {

    final var call = HandlerCall
        .of(
            UserTaskDetailsProvider.class, userTask.workflowModuleId(),
            userTask.bpmnProcessId())
        .lookupKeys(
            BusinessCockpitHandlers
                .lookupKeysOf(userTask.taskDefinition(), userTask.bpmnTaskId()))
        .workflowAggregateId(userTask.workflowAggregateId())
        .payload(event);
    // a variable the engine holds as null is left out rather than handed on: VanillaBP copies
    // the variables of an invocation into an immutable map, which has no room for a null, and a
    // '@TaskParam' of a variable nobody set receives null either way
    prefill
        .variables()
        .forEach((
            name,
            value) -> {
          if (value != null) {
            call.variable(name, value);
          }
        });
    prefill.multiInstances().forEach(call::multiInstance);
    if (!savingTheWorkflowAggregate) {
      call.withoutSavingTheWorkflowAggregate();
    }
    handlers
        .invoke(call.build())
        .map(UserTaskDetails.class::cast)
        .ifPresent(event::applyReturnedDetails);

  }

  private void fillTitles(
      final UserTaskEvent event,
      final UserTaskDetailsPrefill prefill) {

    EventTitles
        .fill(
            event,
            configuration.workflowModule(event.getWorkflowModuleId()),
            templating,
            prefill.bpmnTaskName(),
            prefill.bpmnProcessName());

  }

  /**
   * Writes one entry, into the store the workflow aggregate's transaction reaches and into the
   * transaction the caller asked for.
   * <p>
   * {@link EventTransaction#CURRENT} runs through the transaction runner rather than writing
   * straight away, so that a BPMS half which promised a running transaction and brought none is
   * told instead of having its entry committed on its own.
   */
  private boolean schedule(
      final PhaseTwoCall call,
      final EventTransaction transaction,
      final Class<?> workflowAggregateClass,
      final String workflowModuleId,
      final String bpmnProcessId) {

    if (!configuration.reportsToTheCockpit(workflowModuleId)) {
      logger
          .debug(
              "Not reporting anything of workflow module '{}': it configured none of the Business Cockpit's settings",
              workflowModuleId);
      return false;
    }
    final var store = workflowAggregateClass == null
        ? outbox.ofEventObservedByABpms(workflowModuleId, bpmnProcessId)
        : outbox.ofWorkflowAggregate(workflowAggregateClass);
    return transaction == EventTransaction.NEW
        ? transactionRunner.requireNew(() -> store.schedule(call))
        : transactionRunner.inCurrent(() -> store.schedule(call));

  }

  /**
   * Says that a report was dropped because the BPMS no longer knows what it is about.
   * <p>
   * Dropping a report which was to carry details loses what the cockpit would have shown, so it
   * is said out loud with everything needed to find the case again. A BPMS whose read model is
   * merely lagging behind must not end here at all: a bridge which may know the task in a
   * moment throws
   * <code>io.vanillabp.integration.spi.PhaseTwoRetryLater</code> instead of answering empty, and
   * the outbox brings the entry back.
   * <p>
   * Only a report which was to carry details reaches this method: the other kinds carry
   * nothing to look up, so nothing about them can be missing.
   */
  private static void reportDropped(
      final String what,
      final String kind,
      final String adapterId) {

    logger
        .warn(
            "Not reporting {} as {} to the Business Cockpit: the BPMS '{}' does not know it any more",
            what,
            kind,
            adapterId);

  }

  private static String listed(
      final Collection<String> names) {

    return names.isEmpty()
        ? "none"
        : String.join(", ", names);

  }

  private static boolean carriesDetails(
      final UserTaskEventKind kind) {

    return (kind == UserTaskEventKind.CREATED) || (kind == UserTaskEventKind.UPDATED);

  }

  private static boolean carriesDetails(
      final WorkflowEventKind kind) {

    return (kind == WorkflowEventKind.CREATED) || (kind == WorkflowEventKind.UPDATED);

  }

  private static void put(
      final Map<String, String> args,
      final String key,
      final String value) {

    if (value != null) {
      args.put(key, value);
    }

  }

  private static String eventIdOf(
      final String bpmsEventId) {

    return (bpmsEventId == null) || bpmsEventId.isBlank()
        ? UUID.randomUUID().toString()
        : bpmsEventId;

  }

  private static OffsetDateTime timestampOf(
      final OffsetDateTime timestamp) {

    return timestamp == null ? OffsetDateTime.now() : timestamp;

  }

  /**
   * Reads back the time an entry was written with.
   * <p>
   * A value which cannot be read ends the dispatch for good rather than being replaced by the
   * current time: the timestamp is what the cockpit orders the case's history by, and an entry
   * carrying a broken one would be repeated forever or, worse, be shown at the wrong moment.
   * Only this extension writes the value, so an unreadable one is a defect and not something a
   * retry heals.
   */
  private static OffsetDateTime parseTimestamp(
      final String timestamp) {

    if (timestamp == null) {
      return OffsetDateTime.now();
    }
    try {
      return OffsetDateTime.parse(timestamp);
    } catch (final DateTimeParseException e) {
      throw new PhaseTwoPermanentFailure(
          """
              The outbox entry of a Business Cockpit report carries '%s' as its '%s', which is no \
              ISO-8601 timestamp. The entry cannot be dispatched and repeating it will not change \
              that; remove it from the outbox store."""
              .formatted(timestamp, BusinessCockpitOperations.ARG_TIMESTAMP), e);
    }

  }

  /**
   * The BPMS halves by the adapter id each of them serves.
   *
   * @param bridges What the application brought
   * @return The halves, one per adapter id
   * @throws IllegalStateException If two of them claim the same adapter id, which would make
   *           the answer to "who holds this task" depend on the order the beans were found in
   */
  private static Map<String, BusinessCockpitBpmsBridge> bridgesByAdapterId(
      final Collection<BusinessCockpitBpmsBridge> bridges) {

    final var byAdapterId = new LinkedHashMap<String, BusinessCockpitBpmsBridge>();
    for (final var bridge : bridges) {
      final var previous = byAdapterId.put(bridge.adapterId(), bridge);
      if (previous != null) {
        throw new IllegalStateException(
            """
                Two beans implementing BusinessCockpitBpmsBridge serve the adapter '%s': %s and \
                %s. The Business Cockpit asks exactly one of them what a BPMS knows about a task \
                or a workflow, so let each configured adapter have one - a bridge of a second \
                BPMS answers for the adapter id that BPMS is configured under."""
                .formatted(
                    bridge.adapterId(),
                    previous.getClass().getName(),
                    bridge.getClass().getName()));
      }
    }
    return byAdapterId;

  }

  private static Map<String, WorkflowModuleDetailsProvider> detailsProvidersByWorkflowModule(
      final Collection<WorkflowModuleDetailsProvider> providers) {

    final var byWorkflowModule = new LinkedHashMap<String, WorkflowModuleDetailsProvider>();
    for (final var provider : providers) {
      final var previous = byWorkflowModule.put(provider.getWorkflowModuleId(), provider);
      if (previous != null) {
        throw new IllegalStateException(
            """
                Two beans implementing WorkflowModuleDetailsProvider answer for the workflow \
                module '%s': %s and %s. The Business Cockpit asks exactly one of them which \
                groups may see the module's cases, so let each module have one."""
                .formatted(
                    provider.getWorkflowModuleId(),
                    previous.getClass().getName(),
                    provider.getClass().getName()));
      }
    }
    return byWorkflowModule;

  }

  /**
   * Which instance of the workflow module reported an event. The cockpit shows it where several
   * instances report the same case and somebody has to tell which log to read.
   */
  private static String sourceOfThisInstance() {

    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (final UnknownHostException e) {
      return "unknown-host";
    }

  }

}
