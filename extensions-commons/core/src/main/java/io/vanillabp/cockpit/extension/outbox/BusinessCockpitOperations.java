package io.vanillabp.cockpit.extension.outbox;

import java.util.Optional;

import io.vanillabp.integration.spi.PhaseOperation;
import io.vanillabp.integration.spi.PhaseTwoCall;

/**
 * The three operations the Business Cockpit extension contributes to VanillaBP's outbox, and
 * the names of the values they carry.
 * <p>
 * <b>Everything here is a persisted contract.</b> An entry written by yesterday's version of an
 * application is dispatched by today's, so an operation is never renamed and the rule deriving
 * its idempotency key is never changed.
 * <p>
 * An entry is a row of identifiers. The report itself is put together at the moment of the
 * event and travels as the payload of the entry, which VanillaBP keeps beside it, so the 2048
 * characters an outbox store holds for arguments are not what a report has to fit in. See
 * decision 26 in the repository's DECISIONS.md.
 * <p>
 * Several waiting reports about one task still collapse into one. What changed is which of them
 * survives: the youngest takes the place of the one which waits, because each of them carries
 * its own state now and the newest is the one a reader wants. Why the keys look the way they do
 * is decision 4 in the repository's DECISIONS.md.
 */
public final class BusinessCockpitOperations {

  /** The namespace separating these operations from VanillaBP's own. */
  public static final String NAMESPACE = "businesscockpit";

  /** Reports one event of one user task. */
  public static final String PUBLISH_USER_TASK_EVENT = NAMESPACE
      + PhaseOperation.NAMESPACE_SEPARATOR
      + "PUBLISH_USER_TASK_EVENT";

  /** Reports one event of one workflow. */
  public static final String PUBLISH_WORKFLOW_EVENT = NAMESPACE
      + PhaseOperation.NAMESPACE_SEPARATOR
      + "PUBLISH_WORKFLOW_EVENT";

  /** Tells the cockpit server that this workflow module exists and where it answers. */
  public static final String REGISTER_WORKFLOW_MODULE = NAMESPACE
      + PhaseOperation.NAMESPACE_SEPARATOR
      + "REGISTER_WORKFLOW_MODULE";

  /**
   * What stands where an entry names a BPMN process but is about none: the registration of a
   * workflow module is about the module as a whole. The outbox stores require the column, so
   * the value says "every process of this module" rather than being left empty.
   */
  public static final String EVERY_BPMN_PROCESS = "*";

  /** The kind of event, the name of a {@code UserTaskEventKind} or {@code WorkflowEventKind}. */
  public static final String ARG_EVENT_KIND = "kind";

  /** The BPMS' identifier of the user task. */
  public static final String ARG_USER_TASK_ID = "userTaskId";

  /** The BPMS' identifier of the workflow. */
  public static final String ARG_WORKFLOW_ID = "workflowId";

  /** The task's form reference, one of the two keys a details provider is matched by. */
  public static final String ARG_TASK_DEFINITION = "taskDefinition";

  /** The task's BPMN element id, the other key a details provider is matched by. */
  public static final String ARG_BPMN_TASK_ID = "bpmnTaskId";

  /**
   * The version of the deployed BPMN process the event came from, or nothing where the BPMS
   * reports none. It picks the details provider which serves that version.
   * <p>
   * It travels with the entry although the dispatch reads the rest again. The version is part of
   * naming the event: a deployment made while the entry waited must not move the event to the
   * method of the newer model.
   */
  public static final String ARG_PROCESS_VERSION = "processVersion";

  /** The BPMS' identifier of the event, which becomes the event id the cockpit sees. */
  public static final String ARG_EVENT_ID = "eventId";

  /** When the BPMS says the event happened, in ISO-8601. */
  public static final String ARG_TIMESTAMP = "timestamp";

  private BusinessCockpitOperations() {
  }

  /**
   * @return The operation reporting a user-task event
   */
  public static PhaseOperation publishUserTaskEvent() {

    return PhaseOperation
        .extensionOperation(PUBLISH_USER_TASK_EVENT)
        .idempotencyKey(BusinessCockpitOperations::userTaskKey)
        .describedAs(
            args -> "reporting user task '%s' as %s to the Business Cockpit"
                .formatted(args.get(ARG_USER_TASK_ID), args.get(ARG_EVENT_KIND)))
        .hintingWhenUnknown(
            """
                The Business Cockpit adapter is not part of this application any more, so nobody \
                can report the event. The entry stays until the adapter is back or somebody \
                removes it.""")
        .build();

  }

  /**
   * @return The operation reporting a workflow event
   */
  public static PhaseOperation publishWorkflowEvent() {

    return PhaseOperation
        .extensionOperation(PUBLISH_WORKFLOW_EVENT)
        .idempotencyKey(BusinessCockpitOperations::workflowKey)
        .describedAs(
            args -> "reporting workflow '%s' as %s to the Business Cockpit"
                .formatted(args.get(ARG_WORKFLOW_ID), args.get(ARG_EVENT_KIND)))
        .hintingWhenUnknown(
            """
                The Business Cockpit adapter is not part of this application any more, so nobody \
                can report the event. The entry stays until the adapter is back or somebody \
                removes it.""")
        .build();

  }

  /**
   * @return The operation registering a workflow module
   */
  public static PhaseOperation registerWorkflowModule() {

    return PhaseOperation
        .extensionOperation(REGISTER_WORKFLOW_MODULE)
        .idempotencyKey(
            call -> Optional
                .of("%s|%s".formatted(REGISTER_WORKFLOW_MODULE, call.workflowModuleId())))
        .describedAs(
            args -> "registering the workflow module at the Business Cockpit")
        .hintingWhenUnknown(
            """
                The Business Cockpit adapter is not part of this application any more, so the \
                workflow module cannot be registered.""")
        .build();

  }

  /**
   * The key of a user-task entry: the operation, the BPMS holding the task, the task and the
   * kind of event.
   * <p>
   * Two pending updates of one task share a key on purpose. Only one of them survives, and it
   * is the youngest: every entry carries the report it was planned with, so the youngest is the
   * one which says what is true now.
   *
   * @param call The entry being scheduled
   * @return The key
   */
  private static Optional<String> userTaskKey(
      final PhaseTwoCall call) {

    return Optional
        .of(
            "%s|%s|%s|%s".formatted(
                PUBLISH_USER_TASK_EVENT,
                call.adapterId(),
                call.args().get(ARG_USER_TASK_ID),
                call.args().get(ARG_EVENT_KIND)));

  }

  /**
   * The key of a workflow entry, built the same way and for the same reason.
   *
   * @param call The entry being scheduled
   * @return The key
   */
  private static Optional<String> workflowKey(
      final PhaseTwoCall call) {

    return Optional
        .of(
            "%s|%s|%s|%s".formatted(
                PUBLISH_WORKFLOW_EVENT,
                call.adapterId(),
                call.args().get(ARG_WORKFLOW_ID),
                call.args().get(ARG_EVENT_KIND)));

  }

}
