package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.BusinessCockpitExtension;
import io.vanillabp.cockpit.extension.config.BusinessCockpitConfiguration;
import io.vanillabp.cockpit.extension.event.RegisterWorkflowModuleEvent;
import io.vanillabp.cockpit.extension.event.UserTaskEvent;
import io.vanillabp.cockpit.extension.event.WorkflowEvent;
import io.vanillabp.cockpit.extension.outbox.BusinessCockpitOutbox;
import io.vanillabp.cockpit.extension.templating.Templating;
import io.vanillabp.cockpit.extension.transport.BusinessCockpitTransport;
import io.vanillabp.integration.adapter.migration.processservice.TransactionRunnerResolver;
import io.vanillabp.integration.extension.spi.handler.ExtensionHandlers;
import io.vanillabp.integration.extension.spi.handler.HandlerCall;
import io.vanillabp.integration.extension.spi.handler.HandlerContract;
import io.vanillabp.integration.spi.PhaseOperationRegistry;
import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.integration.spi.TransactionRunner;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.cockpit.workflowmodules.WorkflowModuleGroupHierarchy;

/**
 * A change of the group hierarchy which comes while the registration of the module is on its
 * way to the cockpit server.
 * <p>
 * The registration reads the hierarchy when its entry is dispatched. If the change comes after
 * that read, the entry on its way carries the old hierarchy. Then only a further registration
 * can tell the cockpit about the change. These tests run the outbox by hand, so the change
 * comes at exactly that moment and not by chance.
 */
@ExtendWith(SuppressOutputExtension.class)
public class RegistrationAfterAHierarchyChangeTest {

  private static final String MODULE = ConfigurationFixture.WORKFLOW_MODULE;

  private static final Map<String, Collection<String>> AT_START = Map.of("LEAD_AT_START", List.of("MEMBER"));

  private static final Map<String, Collection<String>> CHANGED = Map.of("CHANGED_LEAD", List.of("MEMBER"));

  /** A bean whose hierarchy the test changes. */
  private static final class ChangingHierarchy implements WorkflowModuleGroupHierarchy {

    private volatile Map<String, Collection<String>> hierarchy = AT_START;

    @Override
    public String getWorkflowModuleId() {

      return MODULE;

    }

    @Override
    public Map<String, Collection<String>> getGroupHierarchy() {

      return hierarchy;

    }

  }

  /**
   * An outbox store which keeps its entries in memory and which the test dispatches by hand. It
   * deduplicates the way the contract of {@link PhaseTwoOutbox} says. A key belongs to its entry
   * until that entry is done, also while a dispatch is running. A plain call is discarded
   * against it. A replacing call takes the place of an entry which waits, and becomes an entry
   * of its own beside one which a dispatch has taken.
   */
  private static final class OutboxRunByHand implements PhaseTwoOutbox {

    private enum State {
      WAITING,
      TAKEN,
      DONE
    }

    private static final class Entry {

      private PhaseTwoCall call;

      private State state = State.WAITING;

      private Entry(
          final PhaseTwoCall call) {

        this.call = call;

      }

    }

    private final List<Entry> entries = new ArrayList<>();

    private PhaseOperationRegistry registry;

    @Override
    public boolean schedule(
        final PhaseTwoCall call) {

      final var holdingTheKey = entries
          .stream()
          .filter(entry -> entry.state != State.DONE)
          .filter(entry -> entry.call.idempotencyKey().equals(call.idempotencyKey()))
          .findFirst();
      if (holdingTheKey.isEmpty()) {
        entries.add(new Entry(call));
        return true;
      }
      if (!call.replacesWhatIsStillWaiting()) {
        return false;
      }
      if (holdingTheKey.get().state == State.WAITING) {
        holdingTheKey.get().call = call;
        return true;
      }
      entries.add(new Entry(call));
      return true;

    }

    @Override
    public boolean scheduleReplacingWhatIsStillWaiting(
        final PhaseTwoCall call) {

      return schedule(call.replacingWhatIsStillWaiting());

    }

    /** Dispatches the waiting entries, one after the other, until none waits. */
    private void dispatchWhatWaits() {

      for (var round = 0; round < 10; ++round) {
        final var next = entries.stream().filter(entry -> entry.state == State.WAITING).findFirst();
        if (next.isEmpty()) {
          return;
        }
        next.get().state = State.TAKEN;
        registry
            .dispatchFor(next.get().call.operation())
            .orElseThrow()
            .dispatch(next.get().call, false);
        next.get().state = State.DONE;
      }
      throw new AssertionError("The outbox does not run dry");

    }

  }

  /** Records each registration and lets the test act right after the server got it. */
  private static final class RecordingTransport implements BusinessCockpitTransport {

    private final List<RegisterWorkflowModuleEvent> registrations = new ArrayList<>();

    private Consumer<RegisterWorkflowModuleEvent> whenTheServerGotIt = registration -> {
    };

    @Override
    public String describe() {

      return "a transport of the test";

    }

    @Override
    public void publishUserTaskEvent(
        final UserTaskEvent event) {

      throw new UnsupportedOperationException();

    }

    @Override
    public void publishWorkflowEvent(
        final WorkflowEvent event) {

      throw new UnsupportedOperationException();

    }

    @Override
    public void registerWorkflowModule(
        final RegisterWorkflowModuleEvent event) {

      registrations.add(event);
      whenTheServerGotIt.accept(event);

    }

  }

  /** Runs the work at once. The outbox of this test needs no transaction. */
  private static final class NoTransaction implements TransactionRunner {

    @Override
    public <T> T requireNew(
        final Supplier<T> work) {

      return work.get();

    }

    @Override
    public <T> T inCurrent(
        final Supplier<T> work) {

      return work.get();

    }

    @Override
    public boolean isRollbackOnly() {

      return false;

    }

  }

  private static final class NoTransactions implements TransactionRunnerResolver {

    @Override
    public TransactionRunner resolveFor(
        final Class<?> workflowAggregateClass) {

      return new NoTransaction();

    }

    @Override
    public String remediesDescription() {

      return "";

    }

    @Override
    public String describeResolutionFor(
        final Class<?> workflowAggregateClass) {

      return "";

    }

  }

  /** A module without workflow services, which is all a registration needs. */
  private static final class NoHandlers implements ExtensionHandlers {

    @Override
    public void register(
        final HandlerContract contract) {
    }

    @Override
    public boolean hasHandler(
        final Class<? extends Annotation> annotationType,
        final String workflowModuleId,
        final String bpmnProcessId,
        final List<String> lookupKeys,
        final String processVersion) {

      return false;

    }

    @Override
    public Optional<Object> invoke(
        final HandlerCall call) {

      return Optional.empty();

    }

    @Override
    public Optional<Class<?>> workflowAggregateOf(
        final String workflowModuleId,
        final String bpmnProcessId) {

      return Optional.empty();

    }

    @Override
    public List<String> bpmnProcessesOf(
        final String workflowModuleId) {

      return List.of();

    }

    @Override
    public Optional<String> bpmnTaskNameOf(
        final String workflowModuleId,
        final String bpmnProcessId,
        final String activityId) {

      return Optional.empty();

    }

  }

  private final ChangingHierarchy hierarchy = new ChangingHierarchy();

  private final OutboxRunByHand store = new OutboxRunByHand();

  private final RecordingTransport transport = new RecordingTransport();

  private final BusinessCockpitExtension extension = startedExtension();

  private BusinessCockpitExtension startedExtension() {

    final var fixture = ConfigurationFixture.aConfiguredApplication();
    final var configuration = BusinessCockpitConfiguration
        .readAndValidate(fixture.properties(), fixture.settings(), false, false, List.of(hierarchy));
    final var started = new BusinessCockpitExtension(
        configuration, transport, List.of(), List.of(), new NoHandlers(), Templating
            .none(), new BusinessCockpitOutbox(ignored -> store, () -> List
                .of(store), ""), new NoTransactions(), () -> null);
    store.registry = new PhaseOperationRegistry();
    started.registerOperations(store.registry);
    started.workflowModuleStarted(MODULE);
    started.registerStartedWorkflowModules();
    return started;

  }

  private void changeTheHierarchy() {

    hierarchy.hierarchy = CHANGED;
    extension.registerWorkflowModuleAgain(MODULE);

  }

  private List<Map<String, Collection<String>>> hierarchiesTheServerGot() {

    return transport.registrations.stream().map(RegisterWorkflowModuleEvent::groupHierarchy).toList();

  }

  @Test
  @DisplayName("A change after the registration read the hierarchy is followed by a registration with the new one")
  public void aChangeDuringTheDispatchIsRegisteredAgain() {

    transport.whenTheServerGotIt = registration -> {
      if (registration.groupHierarchy().equals(AT_START)) {
        changeTheHierarchy();
      }
    };

    store.dispatchWhatWaits();

    assertEquals(List.of(AT_START, CHANGED), hierarchiesTheServerGot());

  }

  @Test
  @DisplayName("A change while the registration still waits is carried by that one registration")
  public void aChangeBeforeTheDispatchNeedsNoSecondRegistration() {

    changeTheHierarchy();

    store.dispatchWhatWaits();

    assertEquals(List.of(CHANGED), hierarchiesTheServerGot());

  }

}
