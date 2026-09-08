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
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.config.WorkflowModuleConfiguration;
import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;
import io.vanillabp.cockpit.extension.handler.BusinessCockpitHandlers;
import io.vanillabp.cockpit.extension.outbox.BusinessCockpitOperations;
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
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.integration.spi.TransactionRunner;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetails;
import io.vanillabp.spi.cockpit.usertask.UserTaskDetailsProvider;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetails;
import io.vanillabp.spi.cockpit.workflow.WorkflowDetailsProvider;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleDetailsProvider;

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

  private final PhaseTwoOutbox outbox;

  private final TransactionRunner transactionRunner;

  private final String source;

  private final java.util.Set<String> startedWorkflowModules = java.util.concurrent.ConcurrentHashMap
      .newKeySet();

  /**
   * @param configuration What the application configured, already validated
   * @param transport Where the events go
   * @param bridges The BPMS halves, one per configured adapter id
   * @param workflowModuleDetailsProviders What the application says about its modules
   * @param handlers VanillaBP's invocation of the application's details providers
   * @param templating The renderer of the titles, {@link Templating#none()} without templates
   * @param outbox The store the entries are written to
   * @param transactionRunner Opens a transaction where the caller brought none
   */
  public BusinessCockpitExtension(
      final BusinessCockpitConfiguration configuration,
      final BusinessCockpitTransport transport,
      final Collection<BusinessCockpitBpmsBridge> bridges,
      final Collection<WorkflowModuleDetailsProvider> workflowModuleDetailsProviders,
      final ExtensionHandlers handlers,
      final Templating templating,
      final PhaseTwoOutbox outbox,
      final TransactionRunner transactionRunner) {

    this.configuration = configuration;
    this.transport = transport;
    this.handlers = handlers;
    this.templating = templating;
    this.outbox = outbox;
    this.transactionRunner = transactionRunner;
    this.source = sourceOfThisInstance();
    this.bridges = bridges
        .stream()
        .collect(Collectors.toMap(BusinessCockpitBpmsBridge::adapterId, bridge -> bridge));
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
   * @return What the application configured
   */
  public BusinessCockpitConfiguration getConfiguration() {

    return configuration;

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
    return schedule(call, transaction);

  }

  @Override
  public boolean publishWorkflowEvent(
      final WorkflowReference workflow,
      final WorkflowEventKind kind,
      final String bpmsEventId,
      final OffsetDateTime timestamp,
      final EventTransaction transaction) {

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
    return schedule(call, transaction);

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
   */
  public void registerStartedWorkflowModules() {

    startedWorkflowModules
        .forEach(
            workflowModuleId -> transactionRunner
                .requireNew(() -> outbox
                    .schedule(
                        PhaseTwoCall
                            .of(
                                BusinessCockpitOperations.registerWorkflowModule(),
                                configuration.workflowModule(workflowModuleId).workflowModuleId(),
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
   *           names the ones which are and the three artifacts which provide one
   */
  public BusinessCockpitBpmsBridge bridgeOf(
      final String adapterId) {

    final var bridge = bridges.get(adapterId);
    if (bridge != null) {
      return bridge;
    }
    throw new IllegalStateException(
        """
            The Business Cockpit extension has no BPMS half for the adapter '%s'. Registered are: \
            %s. Add the artifact belonging to that adapter's BPMS to your workflow module - \
            'io.vanillabp.businesscockpit:businesscockpit-camunda7-adapter', \
            'io.vanillabp.businesscockpit:businesscockpit-camunda8-adapter' or \
            'io.vanillabp.businesscockpit:businesscockpit-process-engine-api-adapter', each in the \
            variant of your platform."""
            .formatted(
                adapterId,
                bridges.isEmpty() ? "none" : String.join(", ", bridges.keySet())));

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
        logger
            .debug(
                "Not reporting user task '{}' as {}: the BPMS '{}' does not know it any more",
                userTask.userTaskId(), kind, call.adapterId());
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
        logger
            .debug(
                "Not reporting workflow '{}' as {}: the BPMS '{}' does not know it any more",
                workflow.workflowId(), kind, call.adapterId());
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
        .payload(event)
        .variables(prefill.variables());
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

  private boolean schedule(
      final PhaseTwoCall call,
      final EventTransaction transaction) {

    return transaction == EventTransaction.NEW
        ? transactionRunner.requireNew(() -> outbox.schedule(call))
        : outbox.schedule(call);

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

  private static OffsetDateTime parseTimestamp(
      final String timestamp) {

    if (timestamp == null) {
      return OffsetDateTime.now();
    }
    try {
      return OffsetDateTime.parse(timestamp);
    } catch (final DateTimeParseException e) {
      logger.warn("Could not read the timestamp '{}' of an event, using the current time",
          timestamp);
      return OffsetDateTime.now();
    }

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

  /**
   * @return The settings of the given workflow module
   */
  public WorkflowModuleConfiguration workflowModule(
      final String workflowModuleId) {

    return configuration.workflowModule(workflowModuleId);

  }

}
