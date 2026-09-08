package io.vanillabp.cockpit.extension.quarkus.it;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.vanillabp.integration.spi.PhaseTwoCall;
import io.vanillabp.integration.spi.PhaseTwoOutbox;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * A store of the application, next to the one VanillaBP brings for the datasource of the test.
 * It writes nowhere: what the tests of the attribution ask is which store was handed the entry,
 * and an entry which is never dispatched answers that as well as one which is.
 */
@ApplicationScoped
public class RecordingOutbox implements PhaseTwoOutbox {

  private final List<PhaseTwoCall> scheduled = new CopyOnWriteArrayList<>();

  @Override
  public boolean schedule(
      final PhaseTwoCall call) {

    scheduled.add(call);
    return true;

  }

  /**
   * @return What was written into this store
   */
  public List<PhaseTwoCall> getScheduled() {

    return scheduled;

  }

}
