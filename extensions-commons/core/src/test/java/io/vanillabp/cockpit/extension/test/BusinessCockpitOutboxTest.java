package io.vanillabp.cockpit.extension.test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.cockpit.extension.outbox.BusinessCockpitOutbox;
import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Which store an entry goes into, and what an application is told while it boots.
 * <p>
 * The attribution itself belongs to the platform and is asserted there. What is asserted here
 * is what the extension makes of the answer: the store of every aggregate is resolved while the
 * application starts, an event naming a BPMN process reaches the store of the aggregate that
 * process is served through, and each gap ends the boot with a message naming the way out.
 */
@ExtendWith(SuppressOutputExtension.class)
public class BusinessCockpitOutboxTest {

  private static final String REMEDIES = "- add the store of this platform, or";

  private static class OrderAggregate {
  }

  private static class ShipmentAggregate {
  }

  private static class Store implements PhaseTwoOutbox {

    @Override
    public boolean schedule(
        final PhaseTwoCall call) {

      return true;

    }

  }

  private static final PhaseTwoOutbox RELATIONAL = new Store();

  private static final PhaseTwoOutbox DOCUMENTS = new Store();

  private static BusinessCockpitOutbox.WorkflowProcess process(
      final String workflowModuleId,
      final String bpmnProcessId) {

    return new BusinessCockpitOutbox.WorkflowProcess(workflowModuleId, bpmnProcessId);

  }

  private static BusinessCockpitOutbox outboxOf(
      final Map<Class<?>, PhaseTwoOutbox> attribution,
      final Collection<PhaseTwoOutbox> stores) {

    return new BusinessCockpitOutbox(attribution::get, () -> stores, REMEDIES);

  }

  @Test
  @DisplayName("The store every aggregate shares is what an event of a BPMS is written into")
  public void theSharedStoreCarriesWhatNamesNoAggregate() {

    final var outbox = outboxOf(
        Map.<Class<?>, PhaseTwoOutbox>of(
            OrderAggregate.class, RELATIONAL, ShipmentAggregate.class, RELATIONAL),
        List.of(RELATIONAL));
    outbox
        .validateAtStartup(
            Map
                .of(
                    process("test-module", "Order"), OrderAggregate.class,
                    process("test-module", "Shipment"), ShipmentAggregate.class));

    assertSame(RELATIONAL, outbox.ofWorkflowAggregate(OrderAggregate.class));
    assertSame(RELATIONAL, outbox.ofEventObservedByABpms("test-module", "Order"));
    assertSame(
        RELATIONAL, outbox.ofWorkflowModuleRegistration("test-module", List.of("Order")));

  }

  @Test
  @DisplayName("Aggregates in two stores are served each from its own")
  public void eachAggregateIsServedFromItsOwnStore() {

    final var outbox = outboxOf(
        Map.<Class<?>, PhaseTwoOutbox>of(
            OrderAggregate.class, RELATIONAL, ShipmentAggregate.class, DOCUMENTS),
        List.of(RELATIONAL, DOCUMENTS));
    outbox
        .validateAtStartup(
            Map
                .of(
                    process("test-module", "Order"), OrderAggregate.class,
                    process("test-module", "Shipment"), ShipmentAggregate.class));

    assertSame(RELATIONAL, outbox.ofEventObservedByABpms("test-module", "Order"));
    assertSame(DOCUMENTS, outbox.ofEventObservedByABpms("test-module", "Shipment"));

  }

  @Test
  @DisplayName("Two workflow modules serving a process of the same name keep their own stores")
  public void aProcessNameIsOnlyUniqueWithinItsWorkflowModule() {

    // a workflow module is the boundary which makes two processes of one name legal, and their
    // aggregates may well live in different persistences
    final var outbox = outboxOf(
        Map.<Class<?>, PhaseTwoOutbox>of(
            OrderAggregate.class, RELATIONAL, ShipmentAggregate.class, DOCUMENTS),
        List.of(RELATIONAL, DOCUMENTS));
    outbox
        .validateAtStartup(
            Map
                .of(
                    process("ordering", "Handling"), OrderAggregate.class,
                    process("shipping", "Handling"), ShipmentAggregate.class));

    assertSame(RELATIONAL, outbox.ofEventObservedByABpms("ordering", "Handling"));
    assertSame(DOCUMENTS, outbox.ofEventObservedByABpms("shipping", "Handling"));
    assertSame(
        RELATIONAL, outbox.ofWorkflowModuleRegistration("ordering", List.of("Handling")));
    assertSame(
        DOCUMENTS, outbox.ofWorkflowModuleRegistration("shipping", List.of("Handling")));

  }

  @Test
  @DisplayName("The registration of a module goes into the store of the module's first aggregate")
  public void theRegistrationFollowsTheModulesOwnAggregates() {

    final var outbox = outboxOf(
        Map.<Class<?>, PhaseTwoOutbox>of(
            OrderAggregate.class, RELATIONAL, ShipmentAggregate.class, DOCUMENTS),
        List.of(RELATIONAL, DOCUMENTS));
    outbox
        .validateAtStartup(
            Map
                .of(
                    process("test-module", "Order"), OrderAggregate.class,
                    process("test-module", "Shipment"), ShipmentAggregate.class));

    assertSame(
        DOCUMENTS,
        outbox.ofWorkflowModuleRegistration("test-module", List.of("Shipment")));
    // by class name, so that a restart writes the registration into the same store again
    assertSame(
        RELATIONAL,
        outbox.ofWorkflowModuleRegistration("test-module", List.of("Shipment", "Order")));

  }

  @Test
  @DisplayName("An aggregate without any store ends the boot with the platform's remedies")
  public void anAggregateWithoutAStoreEndsTheBoot() {

    final var outbox = outboxOf(Map.of(), List.of());

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> outbox
            .validateAtStartup(Map.of(process("test-module", "Order"), OrderAggregate.class)));

    assertTrue(failure.getMessage().contains(REMEDIES), failure.getMessage());
    assertTrue(
        failure.getMessage().contains("io.vanillabp.integration.spi.PhaseTwoOutbox"),
        failure.getMessage());

  }

  @Test
  @DisplayName("Without a single workflow aggregate the application's one store still carries")
  public void withoutAnyAggregateTheSingleStoreCarries() {

    final var outbox = outboxOf(Map.of(), List.of(RELATIONAL));
    outbox.validateAtStartup(Map.of());

    assertSame(RELATIONAL, outbox.ofEventObservedByABpms("test-module", "TestProcess"));
    assertSame(
        RELATIONAL, outbox.ofWorkflowModuleRegistration("test-module", List.of("TestProcess")));

  }

  @Test
  @DisplayName("An unknown BPMN process with several stores is told which processes are known")
  public void anUnknownProcessWithSeveralStoresIsAmbiguous() {

    final var outbox = outboxOf(
        Map.<Class<?>, PhaseTwoOutbox>of(
            OrderAggregate.class, RELATIONAL, ShipmentAggregate.class, DOCUMENTS),
        List.of(RELATIONAL, DOCUMENTS));
    outbox
        .validateAtStartup(
            Map
                .of(
                    process("test-module", "Order"), OrderAggregate.class,
                    process("test-module", "Shipment"), ShipmentAggregate.class));

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> outbox.ofEventObservedByABpms("test-module", "Unwired"));

    assertTrue(failure.getMessage().contains("test-module"), failure.getMessage());
    assertTrue(failure.getMessage().contains("Unwired"), failure.getMessage());
    assertTrue(failure.getMessage().contains("Order"), failure.getMessage());
    assertTrue(failure.getMessage().contains("@WorkflowService"), failure.getMessage());

  }

  @Test
  @DisplayName("A workflow module cannot be registered without any store, and is told so")
  public void aWorkflowModuleWithoutAnyStoreNamesItself() {

    final var outbox = outboxOf(Map.of(), List.of());

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> outbox.ofWorkflowModuleRegistration("test-module", List.of()));

    assertTrue(failure.getMessage().contains("test-module"), failure.getMessage());
    assertTrue(failure.getMessage().contains(REMEDIES), failure.getMessage());

  }

}
