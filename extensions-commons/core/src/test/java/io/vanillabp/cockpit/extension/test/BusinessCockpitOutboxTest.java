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
 * is what the extension makes of the answer: the store is resolved for every aggregate while
 * the application starts, the one store they share carries what names no aggregate, and each
 * gap ends the boot with a message naming the way out.
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
    outbox.validateAtStartup(List.of(OrderAggregate.class, ShipmentAggregate.class));

    assertSame(RELATIONAL, outbox.ofWorkflowAggregate(OrderAggregate.class));
    assertSame(RELATIONAL, outbox.ofEventObservedByABpms("test-module", "TestProcess"));
    assertSame(RELATIONAL, outbox.ofWorkflowModuleRegistration("test-module"));

  }

  @Test
  @DisplayName("Aggregates living in different stores end the boot, naming both")
  public void aggregatesInDifferentStoresEndTheBoot() {

    final var outbox = outboxOf(
        Map.<Class<?>, PhaseTwoOutbox>of(
            OrderAggregate.class, RELATIONAL, ShipmentAggregate.class, DOCUMENTS),
        List.of(RELATIONAL, DOCUMENTS));

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> outbox.validateAtStartup(List.of(OrderAggregate.class, ShipmentAggregate.class)));

    assertTrue(failure.getMessage().contains(OrderAggregate.class.getName()), failure.getMessage());
    assertTrue(
        failure.getMessage().contains(ShipmentAggregate.class.getName()), failure.getMessage());
    assertTrue(failure.getMessage().contains("one persistence"), failure.getMessage());

  }

  @Test
  @DisplayName("An aggregate without any store ends the boot with the platform's remedies")
  public void anAggregateWithoutAStoreEndsTheBoot() {

    final var outbox = outboxOf(Map.of(), List.of());

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> outbox.validateAtStartup(List.of(OrderAggregate.class)));

    assertTrue(failure.getMessage().contains(REMEDIES), failure.getMessage());
    assertTrue(
        failure.getMessage().contains("io.vanillabp.integration.spi.PhaseTwoOutbox"),
        failure.getMessage());

  }

  @Test
  @DisplayName("Without a single workflow aggregate the application's one store still carries")
  public void withoutAnyAggregateTheSingleStoreCarries() {

    final var outbox = outboxOf(Map.of(), List.of(RELATIONAL));
    outbox.validateAtStartup(List.of());

    assertSame(RELATIONAL, outbox.ofEventObservedByABpms("test-module", "TestProcess"));
    assertSame(RELATIONAL, outbox.ofWorkflowModuleRegistration("test-module"));

  }

  @Test
  @DisplayName("Without an aggregate and with several stores the message names the module")
  public void withoutAnyAggregateSeveralStoresAreAmbiguous() {

    final var outbox = outboxOf(Map.of(), List.of(RELATIONAL, DOCUMENTS));
    outbox.validateAtStartup(List.of());

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> outbox.ofEventObservedByABpms("test-module", "TestProcess"));

    assertTrue(failure.getMessage().contains("test-module"), failure.getMessage());
    assertTrue(failure.getMessage().contains("TestProcess"), failure.getMessage());

  }

  @Test
  @DisplayName("A workflow module cannot be registered without any store, and is told so")
  public void aWorkflowModuleWithoutAnyStoreNamesItself() {

    final var outbox = outboxOf(Map.of(), List.of());

    final var failure = assertThrows(
        IllegalStateException.class,
        () -> outbox.ofWorkflowModuleRegistration("test-module"));

    assertTrue(failure.getMessage().contains("test-module"), failure.getMessage());
    assertTrue(failure.getMessage().contains(REMEDIES), failure.getMessage());

  }

}
