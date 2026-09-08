package io.vanillabp.cockpit.extension.outbox;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
 * core lands - see decision 12 in the repository's DECISIONS.md.
 * <p>
 * An event a BPMS observed names a workflow module, a BPMN process and a serialized id and no
 * class at all. Such an entry therefore goes into the store every workflow aggregate of this
 * application shares, which {@link #validateAtStartup(Collection)} works out while the
 * application boots - and where the aggregates do not share one, the boot ends there rather
 * than in the transaction of the first event.
 */
public class BusinessCockpitOutbox {

  private final Function<Class<?>, PhaseTwoOutbox> perWorkflowAggregate;

  private final Supplier<Collection<PhaseTwoOutbox>> stores;

  private final String remedies;

  /**
   * The store every workflow aggregate of this application writes into, which is the store an
   * entry naming no aggregate class can be written into as well. <code>null</code> until
   * {@link #validateAtStartup(Collection)} found it.
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
   * Two things are settled here. A store which cannot be attributed to an aggregate ends the
   * boot with the platform's own message instead of surfacing at the first report, and the one
   * store all aggregates share becomes the store for entries which name no aggregate class. An
   * application whose aggregates live in different stores has no such store, and is told so
   * now: an event a BPMS observed would otherwise land in a store the workflow's transaction
   * never reaches, and an entry which is not committed with what it reports reports something
   * that may never have happened.
   *
   * @param workflowAggregateClasses The workflow aggregates of the application
   * @throws IllegalStateException If a store is missing, cannot be attributed, or the
   *           aggregates do not share one - each message names what to do
   */
  public void validateAtStartup(
      final Collection<Class<?>> workflowAggregateClasses) {

    final var aggregatesByStore = new LinkedHashMap<PhaseTwoOutbox, List<String>>();
    for (final var workflowAggregateClass : workflowAggregateClasses) {
      final var store = ofWorkflowAggregate(workflowAggregateClass);
      aggregatesByStore
          .computeIfAbsent(store, ignored -> new LinkedList<>())
          .add(workflowAggregateClass.getName());
    }

    if (aggregatesByStore.size() > 1) {
      throw new IllegalStateException(
          """
              The Business Cockpit extension cannot report what a BPMS observes in this \
              application: its workflow aggregates live in different outbox stores (%s). Such an \
              event names a workflow module, a BPMN process and a serialized id and no aggregate \
              class, so the store holding that aggregate cannot be looked up, and an entry \
              written into another store would not be committed together with the workflow it \
              reports. Let the workflow aggregates of this application live in one persistence, \
              so that one store serves them all."""
              .formatted(aggregatesPerStore(aggregatesByStore)));
    }
    if (aggregatesByStore.size() == 1) {
      storeOfEveryWorkflowAggregate = aggregatesByStore.keySet().iterator().next();
    }

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
   * Such an event names identifiers and no workflow aggregate class, so the store is the one
   * every aggregate of this application shares, found while the application booted.
   *
   * @param workflowModuleId The module the event belongs to
   * @param bpmnProcessId The BPMN process the event belongs to
   * @return The store
   * @throws IllegalStateException If the application has no store, or more than one and no
   *           aggregate to attribute them by
   */
  public PhaseTwoOutbox ofEventObservedByABpms(
      final String workflowModuleId,
      final String bpmnProcessId) {

    if (storeOfEveryWorkflowAggregate != null) {
      return storeOfEveryWorkflowAggregate;
    }
    // no aggregate was registered, so nothing was attributed: a single store is still
    // unambiguous, several are not
    final var found = knownStores();
    if (found.size() == 1) {
      return found.getFirst();
    }
    if (found.isEmpty()) {
      throw new IllegalStateException(noStoreAtAll());
    }
    throw new IllegalStateException(
        """
            The Business Cockpit extension cannot tell which of the outbox stores %s the event \
            of workflow module '%s', BPMN process '%s' belongs in. An event a BPMS observed \
            names identifiers and no workflow aggregate class, so the store which holds that \
            aggregate cannot be looked up, and an entry written into another store would not be \
            committed together with the workflow it reports. Let the workflow aggregates of \
            this application live in one persistence, so that one store serves them all."""
            .formatted(names(found), workflowModuleId, bpmnProcessId));

  }

  /**
   * The store the registration of a workflow module is written into.
   * <p>
   * The registration is about the module and not about any workflow, so it is written in a
   * transaction of its own and belongs to no aggregate. The store the aggregates share carries
   * it, and where the application registered no aggregate at all its single store does.
   *
   * @param workflowModuleId The module to register
   * @return The store
   * @throws IllegalStateException If the application has no store at all
   */
  public PhaseTwoOutbox ofWorkflowModuleRegistration(
      final String workflowModuleId) {

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

  private static String aggregatesPerStore(
      final Map<PhaseTwoOutbox, List<String>> aggregatesByStore) {

    return aggregatesByStore
        .entrySet()
        .stream()
        .map(entry -> "%s holds %s".formatted(entry.getKey().getClass().getName(), entry.getValue()))
        .toList()
        .toString();

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
