package io.vanillabp.cockpit.extension;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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
import io.vanillabp.integration.adapter.migration.processservice.TransactionRunnerResolver;
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

/**
 * The Business Cockpit extension. No line of it knows a BPMS or a platform.
 * <p>
 * Everything an event goes through happens here. A BPMS half reports what its engine observed.
 * That report writes one outbox entry, inside the transaction the BPMS is in. Once that
 * transaction has committed, the entry is dispatched: the BPMS is asked what it knows about the
 * task or the workflow now, the application's details provider is invoked to enrich it, the
 * titles are rendered, and the result goes to the configured transport.
 * <p>
 * The data is read at dispatch time and not carried through the outbox. That keeps an entry
 * small enough for the store, and it lets repeated updates collapse into one. See decision 3 in
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

  private final TransactionRunnerResolver transactionRunners;

  private final String source;

  private final Set<String> startedWorkflowModules = ConcurrentHashMap.newKeySet();

  /**
   * The BPMN processes each workflow module deployed, in the order VanillaBP wired them. They
   * tell the registration of a module which store to go to: one belonging to an aggregate of
   * that module, and not one of another module.
   */
  private final Map<String, Set<String>> bpmnProcessesPerWorkflowModule = new ConcurrentHashMap<>();

  /**
   * @param configuration What the application configured, already validated
   * @param transport Where the events go
   * @param bridges The BPMS halves, one per configured adapter id
   * @param workflowModuleDetailsProviders What the application says about its modules
   * @param handlers VanillaBP's invocation of the application's details providers
   * @param templating The renderer of the titles, {@link Templating#none()} without templates
   * @param outbox Which store an entry is written to
   * @param transactionRunners Which transaction an entry of a workflow aggregate is written in
   */
  public BusinessCockpitExtension(
      final BusinessCockpitConfiguration configuration,
      final BusinessCockpitTransport transport,
      final Collection<BusinessCockpitBpmsBridge> bridges,
      final Collection<WorkflowModuleDetailsProvider> workflowModuleDetailsProviders,
      final ExtensionHandlers handlers,
      final Templating templating,
      final BusinessCockpitOutbox outbox,
      final TransactionRunnerResolver transactionRunners) {

    this.configuration = configuration;
    this.transport = transport;
    this.handlers = handlers;
    this.templating = templating;
    this.outbox = outbox;
    this.transactionRunners = transactionRunners;
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
   * Resolves where an entry of this application is written and in which transaction. The boot
   * ends if one of the two cannot be answered for a workflow aggregate of this application.
   * <p>
   * This runs once the application is up, not while the extension's bean is created. Asking for
   * the store of an aggregate reaches into the application's persistence, and VanillaBP's own
   * startup validation waits for the same reason.
   * <p>
   * Which BPMN processes a workflow module holds, and which aggregate each of them works on,
   * comes from VanillaBP. The application's beans are not scanned a second time. VanillaBP read
   * the <code>&#64;WorkflowService</code> annotations while it built the process services,
   * through whatever proxies the platform put around them.
   *
   * @throws IllegalStateException If a store is missing, if it cannot be attributed to an
   *           aggregate, or if no transaction can be opened for one. See decisions 13 and 16 in
   *           the repository's DECISIONS.md
   */
  public void validateWhereEntriesAreWritten() {

    final var workflowAggregates = workflowAggregatesByBpmnProcess();
    outbox.validateAtStartup(workflowAggregates);
    workflowAggregates
        .values()
        .stream()
        .distinct()
        .forEach(this::requireTransactionFor);

  }

  /**
   * The workflow aggregate which serves each BPMN process of the reporting workflow modules,
   * asked of VanillaBP.
   * <p>
   * A workflow service declares two things: the aggregate it is written for, and the processes
   * it serves. The processes are its primary one and any it names as secondary, which is a
   * process called by a call activity or a process which was renamed. VanillaBP registered
   * every one of those pairs while it built the process services. So an event which names a
   * BPMN process yields the class whose transaction the entry has to ride, and nobody reads the
   * annotations a second time.
   * <p>
   * The key is the pair, not the BPMN process alone. Two workflow modules of one application
   * may serve a process of the same name, and their aggregates may live in different
   * persistences.
   *
   * @return The aggregate of every BPMN process of every reporting workflow module
   */
  private Map<BusinessCockpitOutbox.WorkflowProcess, Class<?>> workflowAggregatesByBpmnProcess() {

    final var aggregates = new LinkedHashMap<BusinessCockpitOutbox.WorkflowProcess, Class<?>>();
    configuration
        .getWorkflowModuleIds()
        .forEach(
            workflowModuleId -> handlers
                .bpmnProcessesOf(workflowModuleId)
                .forEach(
                    bpmnProcessId -> handlers
                        .workflowAggregateOf(workflowModuleId, bpmnProcessId)
                        .ifPresent(
                            workflowAggregateClass -> aggregates
                                .put(
                                    new BusinessCockpitOutbox.WorkflowProcess(
                                        workflowModuleId, bpmnProcessId),
                                    workflowAggregateClass))));
    return aggregates;

  }

  /**
   * The transaction an entry about this workflow aggregate is written in.
   * <p>
   * It is the transaction the aggregate's own writes go through, and that may well be a runner
   * the application contributed. A transaction of the extension's own would commit the report
   * separately from the change it reports. It would also lose what the platform's runner knows
   * about the transaction it opened: its rollback-only verdict, its pre-commit callbacks, and
   * that it sees an optimistic-locking failure.
   *
   * @param workflowAggregateClass The aggregate, or <code>null</code> for an entry belonging to
   *          none
   * @return The runner
   * @throws IllegalStateException If this application can open no transaction at all
   */
  private TransactionRunner requireTransactionFor(
      final Class<?> workflowAggregateClass) {

    // some entries belong to no workflow aggregate: the registration of a workflow module, and
    // an event about a BPMN process which no workflow service of this application declares.
    // They are written through the runner which serves every aggregate nobody claimed, and
    // asking for the root of the type hierarchy is how that runner is found
    final var runner = transactionRunners
        .resolveFor(
            workflowAggregateClass == null
                ? Object.class
                : workflowAggregateClass);
    if (runner != null) {
      return runner;
    }
    throw new IllegalStateException(
        """
            The Business Cockpit adapter cannot open a transaction for %s: it reports every \
            event after the transaction which caused it was committed, and an event a remote \
            engine reported arrives on a worker thread which brings no transaction of its own. \
            To get one either
            %s"""
            .formatted(
                workflowAggregateClass == null
                    ? "the registration of a workflow module"
                    : "workflow aggregate '%s'".formatted(workflowAggregateClass.getName()),
                transactionRunners.remediesDescription()));

  }

  /**
   * Releases what the transport holds. Called when the application shuts down.
   */
  public void stop() {

    transport.close();

  }

  @Override
  public boolean reportsUserTasks() {

    return configuration.isUserTasksEnabled();

  }

  @Override
  public boolean reportsWorkflows() {

    return configuration.isWorkflowListEnabled();

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
   * A report through <code>BusinessCockpitService</code> knows that class, a BPMS half does
   * not. The class decides which of the application's outbox stores holds the entry, and that
   * store has to be the one the aggregate's own transaction reaches.
   *
   * @param userTask The task
   * @param kind What happened to it
   * @param bpmsEventId The BPMS' own id of the event, or <code>null</code> for one of ours
   * @param timestamp When it happened
   * @param transaction Whether the entry rides the caller's transaction or gets one of its own
   * @param workflowAggregateClass The aggregate's class, or <code>null</code> where the caller
   *          does not know it
   * @return Whether an entry was written, <code>false</code> where one of the same key is still
   *         waiting to be dispatched or where the application reports no user tasks at all
   */
  public boolean publishUserTaskEvent(
      final UserTaskReference userTask,
      final UserTaskEventKind kind,
      final String bpmsEventId,
      final OffsetDateTime timestamp,
      final EventTransaction transaction,
      final Class<?> workflowAggregateClass) {

    if (!configuration.isUserTasksEnabled()) {
      return false;
    }

    final var args = new LinkedHashMap<String, String>();
    put(args, BusinessCockpitOperations.ARG_EVENT_KIND, kind.name());
    put(args, BusinessCockpitOperations.ARG_USER_TASK_ID, userTask.userTaskId());
    put(args, BusinessCockpitOperations.ARG_WORKFLOW_ID, userTask.workflowId());
    put(args, BusinessCockpitOperations.ARG_TASK_DEFINITION, userTask.taskDefinition());
    put(args, BusinessCockpitOperations.ARG_BPMN_TASK_ID, userTask.bpmnTaskId());
    put(args, BusinessCockpitOperations.ARG_PROCESS_VERSION, userTask.processVersion());
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
   * Reports a workflow, knowing which workflow aggregate class it belongs to. It is the
   * counterpart of {@link #publishUserTaskEvent(UserTaskReference, UserTaskEventKind, String,
   * OffsetDateTime, EventTransaction, Class)} for a workflow.
   *
   * @param workflow The workflow
   * @param kind What happened to it
   * @param bpmsEventId The BPMS' own id of the event, or <code>null</code> for one of ours
   * @param timestamp When it happened
   * @param transaction Whether the entry rides the caller's transaction or gets one of its own
   * @param workflowAggregateClass The aggregate's class, or <code>null</code> where the caller
   *          does not know it
   * @return Whether an entry was written, <code>false</code> where the application reports no
   *         workflows at all
   */
  public boolean publishWorkflowEvent(
      final WorkflowReference workflow,
      final WorkflowEventKind kind,
      final String bpmsEventId,
      final OffsetDateTime timestamp,
      final EventTransaction transaction,
      final Class<?> workflowAggregateClass) {

    if (!configuration.isWorkflowListEnabled()) {
      return false;
    }

    final var args = new LinkedHashMap<String, String>();
    put(args, BusinessCockpitOperations.ARG_EVENT_KIND, kind.name());
    put(args, BusinessCockpitOperations.ARG_WORKFLOW_ID, workflow.workflowId());
    put(args, BusinessCockpitOperations.ARG_PROCESS_VERSION, workflow.processVersion());
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
   * Notes which workflow module a BPMN process was deployed by, which VanillaBP says once per
   * file while it wires them.
   * <p>
   * The registration of a workflow module belongs to no workflow aggregate, yet it is written
   * into the store of one of the module's own aggregates. So it has to be known which
   * aggregates the module holds.
   *
   * @param workflowModuleId The module
   * @param bpmnProcessId One of its BPMN processes
   */
  public void workflowWired(
      final String workflowModuleId,
      final String bpmnProcessId) {

    bpmnProcessesPerWorkflowModule
        .computeIfAbsent(workflowModuleId, ignored -> ConcurrentHashMap.newKeySet())
        .add(bpmnProcessId);

  }

  /**
   * Schedules the registration of every workflow module which started.
   * <p>
   * This happens when the application is up, not while its workflow modules are being deployed.
   * Only by then is the outbox store ready. On Quarkus the store creates its table in a startup
   * observer of its own, which runs after the deployment pipeline.
   * <p>
   * Nothing is sent here. One entry is written per module, and it reaches the cockpit server
   * later. So a cockpit server which is down does not stop the application from booting: the
   * outbox keeps trying until the server answers.
   * <p>
   * A module which configured nothing about the cockpit is left out, the way it is left out of
   * everything else the extension does.
   * <p>
   * A setting a module left to its workflows is checked first, because only now is it known
   * which workflows the module holds. A workflow which does not carry that setting either ends
   * the boot, and no module is registered. The application hears about it here, and not at the
   * first event of that one process.
   */
  public void registerStartedWorkflowModules() {

    startedWorkflowModules
        .stream()
        .filter(configuration::reportsToTheCockpit)
        .forEach(
            workflowModuleId -> configuration
                .validateWhatTheWorkflowsHaveToSay(
                    workflowModuleId,
                    bpmnProcessesPerWorkflowModule.getOrDefault(workflowModuleId, Set.of())));

    startedWorkflowModules
        .stream()
        .filter(configuration::reportsToTheCockpit)
        .forEach(
            workflowModuleId -> requireTransactionFor(null)
                .requireNew(() -> outbox
                    .ofWorkflowModuleRegistration(
                        workflowModuleId,
                        bpmnProcessesPerWorkflowModule
                            .getOrDefault(workflowModuleId, Set.of()))
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
   * Runs a question of <code>BusinessCockpitService</code> in one transaction only: the one
   * running on the calling thread, or one of VanillaBP's own if no transaction runs.
   * <p>
   * Joining the caller's transaction makes the answer match what the caller sees. A workflow
   * service which changed its aggregate and has not written it yet gets an answer built from
   * that state, because the details provider reads the aggregate in the caller's own unit of
   * work. A transaction of the extension's own would read what is committed. It would answer
   * about the case as it was before the caller touched it, in the same call which accepts a
   * report of that very change.
   * <p>
   * Opening one where nothing runs is what keeps a caller outside a transaction answerable. A REST
   * controller reading a user task brings nothing to join, and it gets an answer all the same.
   * <p>
   * The runner is the one the aggregate's own writes go through, for the same reason a report
   * rides that one. A workflow aggregate kept in a system the platform does not manage is read
   * through the unit of work of that system and no other. Which transaction a question works in
   * is decision 21 in the repository's DECISIONS.md.
   *
   * @param <T> What the question answers with
   * @param workflowAggregateClass The aggregate the question is about
   * @param question The read to run
   * @return What the question answered
   */
  public <T> T readInOneTransaction(
      final Class<?> workflowAggregateClass,
      final Supplier<T> question) {

    return requireTransactionFor(workflowAggregateClass)
        .requireTransaction(question);

  }

  /**
   * Whether the calling thread runs a transaction the given aggregate's writes would go through.
   * <p>
   * A report through <code>BusinessCockpitService</code> needs one. The service asks before it
   * writes, so a caller who brought none reads a sentence about the code they wrote instead of
   * the platform's wording about a missing transaction.
   * <p>
   * A runner an application contributed answers <code>true</code> where it cannot tell, which
   * leaves the refusal to the runner itself.
   *
   * @param workflowAggregateClass The aggregate a report would be written for
   * @return Whether a transaction is open
   */
  public boolean aTransactionIsOpenFor(
      final Class<?> workflowAggregateClass) {

    return requireTransactionFor(workflowAggregateClass)
        .isTransactionActive();

  }

  /**
   * Whether a workflow module takes part in the Business Cockpit at all.
   * <code>BusinessCockpitService</code> asks this before it demands a transaction. A module
   * which configured nothing about the cockpit is then not refused for a report nobody writes.
   *
   * @param workflowModuleId The module
   * @return Whether it configured the extension
   */
  public boolean reportsAnythingOf(
      final String workflowModuleId) {

    return configuration.reportsToTheCockpit(workflowModuleId);

  }

  /**
   * Reads the current state of a user task and runs the details provider on it, without
   * reporting anything to the cockpit. This is what
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
    invokeUserTaskDetailsProvider(event, userTask, prefill.get());
    fillTitles(event, prefill.get());
    return Optional.of(event);

  }

  /**
   * The BPMS half serving one adapter id.
   * <p>
   * The adapter id the entry carries decides which half serves a dispatch. The election
   * answered that id when the event was observed. See decision 5 in the repository's
   * DECISIONS.md.
   *
   * @param adapterId The adapter id an event came from
   * @return The bridge
   * @throws IllegalStateException If no BPMS half is registered for that adapter. The message
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
            The Business Cockpit has no BPMS half for the adapter '%s'. Configured \
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
        call.adapterId(), call.workflowModuleId(), call.bpmnProcessId(), args.get(
            BusinessCockpitOperations.ARG_PROCESS_VERSION), call.workflowAggregateId(), args.get(
                BusinessCockpitOperations.ARG_WORKFLOW_ID), args.get(BusinessCockpitOperations.ARG_USER_TASK_ID), args
                    .get(
                        BusinessCockpitOperations.ARG_TASK_DEFINITION), args
                            .get(BusinessCockpitOperations.ARG_BPMN_TASK_ID));
    final var event = buildUserTaskEvent(
        userTask, kind, args.get(BusinessCockpitOperations.ARG_EVENT_ID),
        parseTimestamp(args.get(BusinessCockpitOperations.ARG_TIMESTAMP)));

    final var described = "user task '%s' (workflow '%s' of '%s/%s', aggregate '%s')".formatted(
        userTask.userTaskId(), userTask.workflowId(), userTask.workflowModuleId(), userTask
            .bpmnProcessId(),
        userTask.workflowAggregateId());
    final var prefill = bridgeOf(call.adapterId()).prefilledUserTaskDetails(userTask);
    if (prefill.isEmpty()) {
      if (!ends(kind)) {
        reportDropped(described, kind.name(), call.adapterId());
        return;
      }
      reportedWithoutDetails(described, kind.name(), call.adapterId());
    } else {
      applyPrefill(event, prefill.get());
      invokeUserTaskDetailsProvider(event, userTask, prefill.get());
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
        call.adapterId(), call.workflowModuleId(), call.bpmnProcessId(), args.get(
            BusinessCockpitOperations.ARG_PROCESS_VERSION), call.workflowAggregateId(), args
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

    final var described = "workflow '%s' of '%s/%s' (aggregate '%s')".formatted(
        workflow.workflowId(), workflow.workflowModuleId(), workflow.bpmnProcessId(), workflow
            .workflowAggregateId());
    final var prefill = bridgeOf(call.adapterId()).prefilledWorkflowDetails(workflow);
    if (prefill.isEmpty()) {
      if (!ends(kind)) {
        reportDropped(described, kind.name(), call.adapterId());
        return;
      }
      reportedWithoutDetails(described, kind.name(), call.adapterId());
    } else {
      event.setBpmnProcessVersion(prefill.get().bpmnProcessVersion());
      event.setBusinessId(prefill.get().businessId());
      event.setInitiator(prefill.get().initiator());
      final var returned = handlers
          .invoke(
              HandlerCall
                  .of(
                      WorkflowDetailsProvider.class, workflow.workflowModuleId(),
                      workflow.bpmnProcessId())
                  .processVersion(workflow.processVersion())
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

  /**
   * Runs the application's <code>&#64;UserTaskDetailsProvider</code>, if it has one.
   * <p>
   * The aggregate is there to be read. The platform does not save it afterwards, whichever way
   * the provider was reached. A provider is a question about what to report, not an instruction
   * to change the case. See decision 17 in the repository's DECISIONS.md. The contract of the
   * annotation carries that rule, so no call repeats it.
   */
  private void invokeUserTaskDetailsProvider(
      final UserTaskEvent event,
      final UserTaskReference userTask,
      final UserTaskDetailsPrefill prefill) {

    final var call = HandlerCall
        .of(
            UserTaskDetailsProvider.class, userTask.workflowModuleId(),
            userTask.bpmnProcessId())
        .lookupKeys(
            BusinessCockpitHandlers
                .lookupKeysOf(userTask.taskDefinition(), userTask.bpmnTaskId()))
        .processVersion(userTask.processVersion())
        .workflowAggregateId(userTask.workflowAggregateId())
        .payload(event);
    // a variable the engine holds as null is left out instead of being handed on. VanillaBP
    // copies the variables of an invocation into an immutable map, and such a map has no room
    // for a null. A '@TaskParam' of a variable nobody set receives null either way
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
   * {@link EventTransaction#CURRENT} runs through the transaction runner instead of writing
   * straight away. A BPMS half which promised a running transaction and brought none is then
   * told about it, instead of having its entry committed on its own.
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
    final var aggregateClass = aggregateOf(workflowModuleId, bpmnProcessId, workflowAggregateClass);
    final var store = aggregateClass == null
        ? outbox.ofEventObservedByABpms(workflowModuleId, bpmnProcessId)
        : outbox.ofWorkflowAggregate(aggregateClass);
    final var transactionRunner = requireTransactionFor(aggregateClass);
    if (transaction == EventTransaction.NEW) {
      return transactionRunner.requireNew(() -> store.schedule(call));
    }
    // whether the entry was written at all tells the two failures apart. A runner which never
    // reached the supplier refused the transaction. Everything else is the store's own failure
    // and travels on as it is
    final var entered = new AtomicBoolean();
    try {
      return transactionRunner.inCurrent(() -> {
        entered.set(true);
        return store.schedule(call);
      });
    } catch (final RuntimeException e) {
      if (entered.get()) {
        throw e;
      }
      throw new IllegalStateException(
          """
              The Business Cockpit was asked to report %s of workflow module '%s' with \
              EventTransaction.CURRENT, and the thread it was asked on runs no transaction. That \
              value means "write the entry in the transaction the BPMS is already in", which only \
              an embedded engine invoking its listeners inside its own transaction can promise. \
              The BPMS half of adapter '%s' has to pass EventTransaction.NEW where it reports from \
              a worker thread, or open a transaction around the report."""
              .formatted(
                  call.operation(), workflowModuleId, call.adapterId()), e);
    }

  }

  /**
   * The workflow aggregate an entry belongs to. It is the class VanillaBP serves that BPMN
   * process of that workflow module with, and otherwise the class the caller knows.
   * <p>
   * The class decides both the store and the transaction, and VanillaBP names the class whose
   * own writes reach that store. So a caller who names another class is served with VanillaBP's
   * answer instead of being refused. Two workflow services may declare one BPMN process for
   * different aggregates. VanillaBP then serves the process with the class it found first and
   * warns about the other. If the extension refused there, it would end a boot which the
   * platform lets run, and it would repeat a warning which has already been said.
   */
  private Class<?> aggregateOf(
      final String workflowModuleId,
      final String bpmnProcessId,
      final Class<?> workflowAggregateClass) {

    return handlers
        .workflowAggregateOf(workflowModuleId, bpmnProcessId)
        .orElse(workflowAggregateClass);

  }

  /**
   * Says that a report was dropped because the BPMS no longer knows what it is about.
   * <p>
   * A dropped report was to carry details, so the cockpit loses what it would have shown. That
   * is said out loud, with everything needed to find the case again. A BPMS whose read model is
   * only lagging behind must not end up here. A bridge which may know the task in a moment
   * throws <code>io.vanillabp.integration.spi.PhaseTwoRetryLater</code> instead of answering
   * empty, and the outbox brings the entry back.
   * <p>
   * Only a report of a task or a case which is still running reaches this method. An end is
   * reported without its details instead, because a report which never arrives leaves a task
   * the cockpit shows as open forever.
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

  /**
   * Says that an end was reported with the identifiers alone.
   * <p>
   * The BPMS was asked and answered nothing. For an end that is an answer, not a defect. An
   * engine may forget a task the moment it is over, and a read model answering out of a cache
   * may have dropped it by the time the report goes out. The cockpit then keeps the business
   * data of the last change instead of the data the case was finished with, and this line says
   * which case that happened to.
   */
  private static void reportedWithoutDetails(
      final String what,
      final String kind,
      final String adapterId) {

    logger
        .info(
            "Reporting {} as {} to the Business Cockpit without details: the BPMS '{}' does not know it any more",
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

  /**
   * @param kind What happened to the user task
   * @return Whether the task is over afterwards
   */
  private static boolean ends(
      final UserTaskEventKind kind) {

    return (kind == UserTaskEventKind.COMPLETED) || (kind == UserTaskEventKind.CANCELED);

  }

  /**
   * @param kind What happened to the workflow
   * @return Whether the case is over afterwards
   */
  private static boolean ends(
      final WorkflowEventKind kind) {

    return (kind == WorkflowEventKind.COMPLETED) || (kind == WorkflowEventKind.CANCELLED);

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
   * A value which cannot be read ends the dispatch for good. It is not replaced by the current
   * time. The cockpit orders the history of a case by this timestamp, so an entry with a broken
   * one would be repeated forever or, worse, be shown at the wrong moment. Only this extension
   * writes the value, so an unreadable one is a defect which no retry heals.
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
