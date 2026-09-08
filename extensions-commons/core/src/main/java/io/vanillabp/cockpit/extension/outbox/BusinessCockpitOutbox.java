package io.vanillabp.cockpit.extension.outbox;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import io.vanillabp.integration.spi.PhaseTwoOutbox;

/**
 * Which store an entry of the extension is written into.
 * <p>
 * An outbox entry has to be written in the very transaction which persists the workflow
 * aggregate it belongs to, because that is what ties the report to the change it reports. An
 * application may hold several stores - one per persistence, or one an application wrote for a
 * single aggregate - so which store that is depends on the aggregate, and picking any of them
 * would break the promise while looking green.
 * <p>
 * The attribution itself is never made here. VanillaBP answers it for its own operations and
 * answers it for the extension too, so an entry of the extension lands where an entry of the
 * core lands - see decision 13 in the repository's DECISIONS.md.
 * <p>
 * An event a BPMS observed names a workflow module, a BPMN process and a serialized id and no
 * class at all. The class is looked up rather than demanded: a workflow service says which
 * aggregate it is written for and which BPMN processes it serves, so
 * {@link #validateAtStartup(Map)} turns the annotations of the application into the store of
 * every one of its processes while it boots.
 */
public class BusinessCockpitOutbox {

  private final Function<Class<?>, PhaseTwoOutbox> perWorkflowAggregate;

  private final Supplier<Collection<PhaseTwoOutbox>> stores;

  private final String remedies;

  /**
   * The workflow aggregate every BPMN process of the application belongs to, taken from the
   * <code>&#64;WorkflowService</code> annotations while the application boots.
   */
  private volatile Map<String, Class<?>> workflowAggregateByBpmnProcessId = Map.of();

  /**
   * The store every workflow aggregate of this application writes into, where they all write
   * into one. <code>null</code> where the application holds several and an entry has to be
   * attributed.
   */
  private volatile PhaseTwoOutbox storeOfEveryWorkflowAggregate;

  /**
   * @param perWorkflowAggregate How the platform attributes a store to a workflow aggregate
   *          class, answering <code>null</code> where the application has none at all
   * @param stores Every store the application holds, the ones a
   *          <code>PhaseTwoOutboxAware</code> bean provides included
   * @param remedies What to do about a missing store on this platform, one line each, taken
   *          from the platform's own resolver so the developer reads the same list VanillaBP
   *          would have printed
   */
  public BusinessCockpitOutbox(
      final Function<Class<?>, PhaseTwoOutbox> perWorkflowAggregate,
      final Supplier<Collection<PhaseTwoOutbox>> stores,
      final String remedies) {

    this.perWorkflowAggregate = perWorkflowAggregate;
    this.stores = stores;
    this.remedies = remedies;

  }

  /**
   * Resolves the store of every workflow aggregate of the application while it boots, the way
   * VanillaBP validates the outbox of its own process services at startup.
   * <p>
   * A store which cannot be attributed to an aggregate ends the boot with the platform's own
   * message instead of surfacing at the first report. And the answers are kept, keyed by the
   * BPMN process the aggregate is served through, so that an event a BPMS observed - which
   * names identifiers and no class - reaches the store of its own aggregate rather than a store
   * the application happens to share.
   *
   * @param workflowAggregateByBpmnProcessId The workflow aggregate of every BPMN process the
   *          application serves
   * @throws IllegalStateException If a store is missing or cannot be attributed - the message
   *           names what to do
   */
  public void validateAtStartup(
      final Map<String, Class<?>> workflowAggregateByBpmnProcessId) {

    final var storesInUse = new LinkedHashSet<PhaseTwoOutbox>();
    workflowAggregateByBpmnProcessId
        .values()
        .forEach(
            workflowAggregateClass -> storesInUse
                .add(ofWorkflowAggregate(workflowAggregateClass)));
    this.workflowAggregateByBpmnProcessId = Map.copyOf(workflowAggregateByBpmnProcessId);
    this.storeOfEveryWorkflowAggregate = storesInUse.size() == 1
        ? storesInUse.iterator().next()
        : null;

  }

  /**
   * The store holding the entries of one workflow aggregate class.
   *
   * @param workflowAggregateClass The class of the aggregate whose transaction the entry rides
   * @return The store
   * @throws IllegalStateException If the application has no store, or none which can be
   *           attributed to that aggregate - the message names what to do
   */
  public PhaseTwoOutbox ofWorkflowAggregate(
      final Class<?> workflowAggregateClass) {

    final var outbox = perWorkflowAggregate.apply(workflowAggregateClass);
    if (outbox != null) {
      return outbox;
    }
    throw new IllegalStateException(noStoreAtAll());

  }

  /**
   * The store an event a BPMS observed is written into.
   * <p>
   * Such an event names a workflow module, a BPMN process and a serialized id and no class, and
   * the class is what decides the store. It is looked up: a BPMN process is served by a
   * workflow service, and that service says which aggregate it is written for.
   *
   * @param workflowModuleId The module the event belongs to
   * @param bpmnProcessId The BPMN process the event belongs to
   * @return The store
   * @throws IllegalStateException If the application has no store, or holds several and this
   *           process belongs to no workflow service of it
   */
  public PhaseTwoOutbox ofEventObservedByABpms(
      final String workflowModuleId,
      final String bpmnProcessId) {

    final var workflowAggregateClass = workflowAggregateByBpmnProcessId.get(bpmnProcessId);
    if (workflowAggregateClass != null) {
      return ofWorkflowAggregate(workflowAggregateClass);
    }
    if (storeOfEveryWorkflowAggregate != null) {
      return storeOfEveryWorkflowAggregate;
    }
    final var found = knownStores();
    if (found.size() == 1) {
      return found.getFirst();
    }
    if (found.isEmpty()) {
      throw new IllegalStateException(noStoreAtAll());
    }
    throw new IllegalStateException(
        """
            The Business Cockpit extension cannot tell which of the outbox stores %s the event of \
            BPMN process '%s' of workflow module '%s' belongs in. The store is the one holding the \
            workflow aggregate, and no workflow service of this application declares that BPMN \
            process - the ones it declares are %s. Annotate the service serving it with \
            '@WorkflowService(workflowAggregateClass = ..., bpmnProcess = @BpmnProcess(bpmnProcessId = \
            "%s"))', or name the process as one of its 'secondaryBpmnProcesses'."""
            .formatted(
                names(found), bpmnProcessId, workflowModuleId,
                listed(workflowAggregateByBpmnProcessId.keySet()), bpmnProcessId));

  }

  /**
   * The store the registration of a workflow module is written into.
   * <p>
   * The registration is about the module and not about any workflow, so it is written in a
   * transaction of its own and belongs to no aggregate. Any store carries it correctly; which
   * one is picked has to be the same after a restart, because two entries of the same
   * registration in two stores would be sent twice. The store of the module's first aggregate
   * by class name is that answer, and an application with one store has only that one anyway.
   *
   * @param workflowModuleId The module to register
   * @param bpmnProcessIdsOfTheModule The BPMN processes this module deployed
   * @return The store
   * @throws IllegalStateException If the application has no store at all
   */
  public PhaseTwoOutbox ofWorkflowModuleRegistration(
      final String workflowModuleId,
      final Collection<String> bpmnProcessIdsOfTheModule) {

    final var firstAggregateOfTheModule = bpmnProcessIdsOfTheModule
        .stream()
        .map(workflowAggregateByBpmnProcessId::get)
        .filter(Objects::nonNull)
        .distinct()
        .sorted(Comparator.comparing(Class::getName))
        .findFirst();
    if (firstAggregateOfTheModule.isPresent()) {
      return ofWorkflowAggregate(firstAggregateOfTheModule.get());
    }
    if (storeOfEveryWorkflowAggregate != null) {
      return storeOfEveryWorkflowAggregate;
    }
    final var found = knownStores();
    if (found.isEmpty()) {
      throw new IllegalStateException(
          """
              Workflow module '%s' cannot be registered at the Business Cockpit server: %s"""
              .formatted(workflowModuleId, noStoreAtAll()));
    }
    return found.getFirst();

  }

  private List<PhaseTwoOutbox> knownStores() {

    return stores
        .get()
        .stream()
        .distinct()
        .toList();

  }

  private String noStoreAtAll() {

    // the closing line is the platform's own: its resolver lists what enables a default and
    // leaves the store an application writes itself to whoever asks - here as there, that is
    // the last of the remedies
    return """
        The Business Cockpit extension needs an outbox store: it reports every event after the \
        transaction which caused it was committed, which is what keeps an event from being \
        reported for something that was rolled back. To get one either
        %s
        - define your own bean implementing io.vanillabp.integration.spi.PhaseTwoOutbox \
        (assign it to specific aggregates via a io.vanillabp.integration.spi.PhaseTwoOutboxAware \
        bean)."""
        .formatted(remedies);

  }

  private static String listed(
      final Collection<String> names) {

    return names.isEmpty()
        ? "none"
        : String.join(", ", names);

  }

  private static String names(
      final Collection<PhaseTwoOutbox> outboxes) {

    return outboxes
        .stream()
        .map(outbox -> outbox.getClass().getName())
        .toList()
        .toString();

  }

}
