package io.vanillabp.cockpit.extension.outbox;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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
 * The extension therefore asks per aggregate wherever it knows the aggregate's class, which is
 * every report made through <code>BusinessCockpitService</code>. An event a BPMS observed names
 * a workflow module, a BPMN process and a serialized id and no class at all, and an application
 * running several stores is told so rather than having one picked for it - see decision 10 in
 * the repository's DECISIONS.md.
 */
public class BusinessCockpitOutbox {

  private final Function<Class<?>, PhaseTwoOutbox> perWorkflowAggregate;

  private final Supplier<Collection<PhaseTwoOutbox>> stores;

  private final String remedies;

  /**
   * @param perWorkflowAggregate How the platform attributes a store to a workflow aggregate
   *          class, answering <code>null</code> where the application has none at all
   * @param stores Every store the application holds, the ones a
   *          <code>PhaseTwoOutboxAware</code> bean provides included
   * @param remedies What to do about a missing store on this platform, one line each, written
   *          as the platform's own message writes them
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
   * Such an event names identifiers and no workflow aggregate class, so the store cannot be
   * attributed the way a report of <code>BusinessCockpitService</code> is. Exactly one store is
   * therefore unambiguous and several are not.
   *
   * @param workflowModuleId The module the event belongs to
   * @param bpmnProcessId The BPMN process the event belongs to
   * @return The store
   * @throws IllegalStateException If the application has no store, or more than one
   */
  public PhaseTwoOutbox ofEventObservedByABpms(
      final String workflowModuleId,
      final String bpmnProcessId) {

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
   * transaction of its own and belongs to no aggregate. Any store therefore carries it
   * correctly, and where an application holds several the first of them by class name is taken,
   * so that a restart writes it into the same one.
   *
   * @param workflowModuleId The module to register
   * @return The store
   * @throws IllegalStateException If the application has no store at all
   */
  public PhaseTwoOutbox ofWorkflowModuleRegistration(
      final String workflowModuleId) {

    final var found = knownStores();
    if (found.isEmpty()) {
      throw new IllegalStateException(noStoreAtAll());
    }
    return found.getFirst();

  }

  private List<PhaseTwoOutbox> knownStores() {

    return stores
        .get()
        .stream()
        .distinct()
        .sorted(Comparator.comparing(outbox -> outbox.getClass().getName()))
        .toList();

  }

  private String noStoreAtAll() {

    return """
        The Business Cockpit extension needs an outbox store: it reports every event after the \
        transaction which caused it was committed, which is what keeps an event from being \
        reported for something that was rolled back. To get one either
        %s"""
        .formatted(remedies);

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
