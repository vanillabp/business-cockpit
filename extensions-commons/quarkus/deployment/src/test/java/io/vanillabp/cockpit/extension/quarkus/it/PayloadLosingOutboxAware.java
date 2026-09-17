package io.vanillabp.cockpit.extension.quarkus.it;

import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import io.vanillabp.integration.spi.PhaseTwoOutboxAware;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * VanillaBP's own store, with the report of every entry thrown away on the way in.
 * <p>
 * It is how a test reaches the entry which outlived its own report. In a running application
 * that happens when an entry waits longer than the outbox keeps a report, or when a crash
 * splits the two writes. Here the entry is written with the reference of a report which was
 * never stored, which is the same thing for whoever dispatches it.
 */
@ApplicationScoped
public class PayloadLosingOutboxAware implements PhaseTwoOutboxAware<TestAggregate> {

  /** The store VanillaBP brings for the datasource of the test. */
  @Inject
  PhaseTwoOutbox vanillabp;

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
