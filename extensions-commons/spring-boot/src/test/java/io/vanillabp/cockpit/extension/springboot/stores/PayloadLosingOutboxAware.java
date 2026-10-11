package io.vanillabp.cockpit.extension.springboot.stores;

import io.vanillabp.cockpit.extension.springboot.test.TestAggregate;
import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.integration.spi.PhaseTwoOutboxAware;

/**
 * VanillaBP's own store, but it throws away the report of every entry on the way in.
 * <p>
 * This is how a test gets an entry which outlived its report. In a running application this
 * happens when an entry waits longer than the outbox keeps a report, or when a crash happens
 * between the two writes. Here the entry is written with the reference of a report which was
 * never stored. For whoever dispatches it, that is the same thing.
 */
public class PayloadLosingOutboxAware implements PhaseTwoOutboxAware<TestAggregate> {

  private final PhaseTwoOutbox vanillabp;

  /**
   * @param vanillabp The store VanillaBP brings for the database of the test, which is the only
   *          bean of this type the application holds
   */
  public PayloadLosingOutboxAware(
      final PhaseTwoOutbox vanillabp) {

    this.vanillabp = vanillabp;

  }

  @Override
  public Class<TestAggregate> getAggregateClass() {

    return TestAggregate.class;

  }

  @Override
  public PhaseTwoOutbox getPhaseTwoOutbox() {

    return new PhaseTwoOutbox() {

      @Override
      public boolean schedule(
          final PhaseTwoCall call) {

        return vanillabp.schedule(withoutItsReport(call));

      }

      @Override
      public boolean scheduleReplacingWhatIsStillWaiting(
          final PhaseTwoCall call) {

        return vanillabp.scheduleReplacingWhatIsStillWaiting(withoutItsReport(call));

      }

    };

  }

  /**
   * The same call without its bytes. The arguments keep the reference of the report, so the
   * entry names one and the payload store holds nothing under it.
   */
  private static PhaseTwoCall withoutItsReport(
      final PhaseTwoCall call) {

    return new PhaseTwoCall(
        call.operation(), call.workflowModuleId(), call.bpmnProcessId(), call
            .workflowAggregateId(), call.adapterId(), call.args(), call.idempotencyKey(), null, call
                .replacesWhatIsStillWaiting());

  }

}
